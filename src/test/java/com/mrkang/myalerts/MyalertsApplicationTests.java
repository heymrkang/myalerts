package com.mrkang.myalerts;

import com.mrkang.myalerts.dao.User;
import com.mrkang.myalerts.dao.UserDao;
import com.mrkang.myalerts.service.AuthService;
import com.mrkang.myalerts.service.JwtUtil;
import com.mrkang.myalerts.service.PasswordUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MyalertsApplicationTests {

    @Autowired
    private UserDao userDao;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("비밀번호 SHA-256 단방향 해시 및 매칭 검증")
    void testPasswordUtil() {
        String raw = "my_password_123";
        String hashed = PasswordUtil.hash(raw);

        assertNotNull(hashed);
        assertTrue(PasswordUtil.matches(raw, hashed));
        assertFalse(PasswordUtil.matches("wrong_pw", hashed));
    }

    @Test
    @DisplayName("JWT Access/Refresh 토큰 생성 및 검증")
    void testJwtUtil() {
        String userId = "test_agent";
        String access = jwtUtil.createAccessToken(userId);
        String refresh = jwtUtil.createRefreshToken(userId);

        assertNotNull(access);
        assertNotNull(refresh);
        assertEquals(userId, jwtUtil.validateAndGetUserId(access));
        assertEquals(userId, jwtUtil.validateAndGetUserId(refresh));

        // 위조 토큰 검증 실패 확인
        assertNull(jwtUtil.validateAndGetUserId(access + "tampered"));
    }

    @Test
    @DisplayName("UserDao 및 스키마 컬럼 동작 검증 (is_del, time_update)")
    void testUserDao() {
        String testId = "dao_user_" + System.currentTimeMillis();
        User user = new User();
        user.setUserId(testId);
        user.setUserPw(PasswordUtil.hash("pw"));

        int inserted = userDao.insert(user);
        assertEquals(1, inserted);

        User loaded = userDao.findById(testId);
        assertNotNull(loaded);
        assertEquals("N", loaded.getIsDel());
        assertNull(loaded.getTimeUpdate()); // 최초 생성 시 NULL 검증

        // 리프래시 토큰 업데이트 후 time_update 갱신 검증
        userDao.updateRefreshToken(testId, "dummy_token");
        User updated = userDao.findById(testId);
        assertNotNull(updated.getTimeUpdate());
        assertEquals("dummy_token", updated.getRefreshToken());
    }

    @Test
    @DisplayName("AuthService 로그인, 토큰 쿠키 세팅 및 회원가입 검증")
    void testAuthServiceFlow() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 1. 회원가입 진행
        String newId = "user_" + System.currentTimeMillis();
        Map<String, Object> regResult = authService.register(newId, "pwpw");
        assertTrue((Boolean) regResult.get("success"));

        // 2. 가입한 계정 로그인 성공 확인
        Map<String, Object> loginResult = authService.login(newId, "pwpw", response);
        assertTrue((Boolean) loginResult.get("success"));
        assertNotNull(response.getCookie("access_token"));
        assertNotNull(response.getCookie("refresh_token"));

        // 3. 잘못된 비밀번호 실패 확인
        MockHttpServletResponse failResponse = new MockHttpServletResponse();
        Map<String, Object> failResult = authService.login(newId, "wrong", failResponse);
        assertFalse((Boolean) failResult.get("success"));

        // 4. resolveUser 쿠키 기반 사용자 복원 및 RTR 검증
        MockHttpServletRequest request = new MockHttpServletRequest();
        // 액세스 토큰 없이 리프래시 토큰만 전송 (액세스 만료 상황 시뮬레이션)
        jakarta.servlet.http.Cookie refreshCookie = response.getCookie("refresh_token");
        request.setCookies(refreshCookie);

        MockHttpServletResponse resolveResponse = new MockHttpServletResponse();
        User resolved = authService.resolveUser(request, resolveResponse);
        assertNotNull(resolved);
        assertEquals(newId, resolved.getUserId());

        // RTR: 새 액세스 토큰과 새 리프래시 토큰이 쿠키로 재발급되었는지 검증
        jakarta.servlet.http.Cookie rotatedAccess = resolveResponse.getCookie("access_token");
        jakarta.servlet.http.Cookie rotatedRefresh = resolveResponse.getCookie("refresh_token");
        assertNotNull(rotatedAccess);
        assertNotNull(rotatedRefresh);
        assertNotEquals(refreshCookie.getValue(), rotatedRefresh.getValue());

        // 5. 탈취된 구버전 리프래시 토큰 재사용 시도 시 즉시 무효화 검증
        MockHttpServletRequest replayRequest = new MockHttpServletRequest();
        replayRequest.setCookies(refreshCookie); // 이미 교체된 구버전 토큰
        MockHttpServletResponse replayResponse = new MockHttpServletResponse();
        User replayUser = authService.resolveUser(replayRequest, replayResponse);
        assertNull(replayUser); // 차단됨

        // DB에서도 세션 제거되었는지 확인
        User dbUser = userDao.findById(newId);
        assertNull(dbUser.getRefreshToken());
    }
}
