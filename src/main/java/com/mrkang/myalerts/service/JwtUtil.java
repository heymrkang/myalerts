package com.mrkang.myalerts.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** JWT 발급 및 검증 유틸 (순수 JDK 표준 라이브러리 기반) */
@Component
public class JwtUtil {

    private static final String FIXED_HEADER = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"; // {"alg":"HS256","typ":"JWT"}
    private static final Pattern SUB_PATTERN = Pattern.compile("\"sub\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern EXP_PATTERN = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");

    private final String secretKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtUtil(
            @Value("${jwt.secret:myalerts-default-jwt-secret-key-32bytes-long!!}") String secretKey,
            @Value("${jwt.access-token-validity-ms:900000}") long accessTokenValidityMs,
            @Value("${jwt.refresh-token-validity-ms:604800000}") long refreshTokenValidityMs) {
        this.secretKey = secretKey;
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    // 1. 액세스 토큰을 생성한다.
    public String createAccessToken(String userId) {
        return createToken(userId, accessTokenValidityMs);
    }

    // 2. 리프래시 토큰을 생성한다.
    public String createRefreshToken(String userId) {
        return createToken(userId, refreshTokenValidityMs);
    }

    // 3. 내부 규격에 맞게 JWT 토큰을 서명하여 반환한다.
    private String createToken(String userId, long validityMs) {
        try {
            long now = System.currentTimeMillis();
            long exp = now + validityMs;

            String payloadJson = String.format("{\"sub\":\"%s\",\"iat\":%d,\"exp\":%d,\"jti\":\"%s\"}",
                    userId.replace("\"", "\\\""), now / 1000, exp / 1000, java.util.UUID.randomUUID());

            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String content = FIXED_HEADER + "." + encodedPayload;
            String signature = sign(content);

            return content + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("JWT 생성 실패", e);
        }
    }

    // 4. JWT 토큰의 서명 및 만료 시간을 검증하고 userId를 추출한다.
    public String validateAndGetUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String content = parts[0] + "." + parts[1];
            String expectedSignature = sign(content);

            // 4-1. 서명 일치 확인 (상수 시간 비교)
            if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                return null;
            }

            // 4-2. 만료 시간 및 subject 추출
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

            Matcher expMatcher = EXP_PATTERN.matcher(payloadJson);
            if (!expMatcher.find()) {
                return null;
            }
            long exp = Long.parseLong(expMatcher.group(1));
            long nowInSeconds = System.currentTimeMillis() / 1000;

            if (exp <= nowInSeconds) {
                return null;
            }

            Matcher subMatcher = SUB_PATTERN.matcher(payloadJson);
            if (!subMatcher.find()) {
                return null;
            }

            return subMatcher.group(1);
        } catch (Exception e) {
            return null;
        }
    }

    // 5. HmacSHA256 알고리즘으로 서명 문자열을 만든다.
    private String sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
    }
}
