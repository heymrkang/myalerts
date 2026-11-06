package com.mrkang.myalerts.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 비밀번호 단방향 해시 유틸 */
public class PasswordUtil {

    // 1. 평문 비밀번호를 SHA-256 16진수 문자열로 해싱한다.
    public static String hash(String rawPassword) {
        if (rawPassword == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("해시 알고리즘 조회 실패", e);
        }
    }

    // 2. 입력받은 평문과 저장된 해시값이 일치하는지 검증한다.
    public static boolean matches(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        return hash(rawPassword).equalsIgnoreCase(hashedPassword);
    }
}
