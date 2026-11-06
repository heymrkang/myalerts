package com.mrkang.myalerts.service;

import com.mrkang.myalerts.dao.User;
import com.mrkang.myalerts.dao.UserDao;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** 인증 처리 비즈니스 서비스 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final UserDao userDao;
    private final JwtUtil jwtUtil;

    // 1. 사용자 로그인 검증 후 토큰을 발급하고 쿠키를 설정한다.
    public Map<String, Object> login(String userId, String userPw, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();

        // 1-1. 사용자 조회
        User user = userDao.findById(userId);
        if (user == null) {
            result.put("success", false);
            result.put("message", "아이디 또는 비밀번호가 일치하지 않습니다.");
            return result;
        }

        // 1-2. 비밀번호 일치 검증
        if (!PasswordUtil.matches(userPw, user.getUserPw())) {
            result.put("success", false);
            result.put("message", "아이디 또는 비밀번호가 일치하지 않습니다.");
            return result;
        }

        // 1-3. 액세스 토큰 및 리프래시 토큰 발급
        String accessToken = jwtUtil.createAccessToken(userId);
        String refreshToken = jwtUtil.createRefreshToken(userId);

        // 1-4. DB에 리프래시 토큰 저장
        userDao.updateRefreshToken(userId, refreshToken);

        // 1-5. 쿠키에 토큰 세팅
        addCookie(response, ACCESS_TOKEN_COOKIE, accessToken, 15 * 60);
        addCookie(response, REFRESH_TOKEN_COOKIE, refreshToken, 7 * 24 * 60 * 60);

        result.put("success", true);
        result.put("userId", userId);
        return result;
    }

    // 2. 신규 사용자 회원가입을 처리한다.
    public Map<String, Object> register(String userId, String userPw) {
        Map<String, Object> result = new HashMap<>();

        // 2-1. 아이디 중복 확인
        User existing = userDao.findById(userId);
        if (existing != null) {
            result.put("success", false);
            result.put("message", "이미 존재하는 사용자 아이디입니다.");
            return result;
        }

        // 2-2. 비밀번호 단방향 해싱 후 사용자 생성
        User newUser = new User();
        newUser.setUserId(userId);
        newUser.setUserPw(PasswordUtil.hash(userPw));
        newUser.setIsDel("N");

        userDao.insert(newUser);

        result.put("success", true);
        result.put("message", "회원가입이 완료되었습니다.");
        return result;
    }

    // 3. 로그아웃 처리하여 DB 토큰을 무효화하고 쿠키를 삭제한다.
    public void logout(String userId, HttpServletResponse response) {
        // 3-1. DB 리프래시 토큰 제거
        if (userId != null && !userId.isBlank()) {
            userDao.clearRefreshToken(userId);
        }

        // 3-2. 토큰 쿠키 제거
        deleteCookie(response, ACCESS_TOKEN_COOKIE);
        deleteCookie(response, REFRESH_TOKEN_COOKIE);
    }

    // 4. 현재 요청의 토큰을 파싱 및 재발급하여 유효한 사용자를 식별한다.
    public User resolveUser(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = getCookieValue(request, ACCESS_TOKEN_COOKIE);
        String refreshToken = getCookieValue(request, REFRESH_TOKEN_COOKIE);

        // 4-1. 액세스 토큰이 유효한 경우 사용자 반환
        String userIdFromAccess = jwtUtil.validateAndGetUserId(accessToken);
        if (userIdFromAccess != null) {
            return userDao.findById(userIdFromAccess);
        }

        // 4-2. 액세스 토큰 만료 시 리프래시 토큰 검증
        if (refreshToken != null) {
            String userIdFromRefresh = jwtUtil.validateAndGetUserId(refreshToken);
            if (userIdFromRefresh != null) {
                User user = userDao.findById(userIdFromRefresh);
                // 4-3. RTR 적용: DB 리프래시 토큰과 일치 시 Access 및 Refresh 토큰 동시 갱신
                if (user != null && refreshToken.equals(user.getRefreshToken())) {
                    String newAccessToken = jwtUtil.createAccessToken(userIdFromRefresh);
                    String newRefreshToken = jwtUtil.createRefreshToken(userIdFromRefresh);

                    userDao.updateRefreshToken(userIdFromRefresh, newRefreshToken);

                    addCookie(response, ACCESS_TOKEN_COOKIE, newAccessToken, 15 * 60);
                    addCookie(response, REFRESH_TOKEN_COOKIE, newRefreshToken, 7 * 24 * 60 * 60);
                    return user;
                } else if (user != null && user.getRefreshToken() != null) {
                    // 4-4. 탈취 감지 (이미 사용된 구버전 리프래시 토큰 재사용 시도): 세션 강제 무효화
                    userDao.clearRefreshToken(userIdFromRefresh);
                    deleteCookie(response, ACCESS_TOKEN_COOKIE);
                    deleteCookie(response, REFRESH_TOKEN_COOKIE);
                }
            }
        }

        return null;
    }

    // 5. HttpOnly 쿠키 생성 헬퍼
    private void addCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        response.addCookie(cookie);
    }

    // 6. 쿠키 삭제 헬퍼
    private void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    // 7. 요청 쿠키 값 조회 헬퍼
    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
