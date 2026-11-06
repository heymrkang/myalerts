package com.mrkang.myalerts.dao;

import lombok.Data;
import java.time.LocalDateTime;

/** 사용자 데이터 모델 */
@Data
public class User {
    private String userId;
    private String userPw;
    private String isDel;
    private String refreshToken;
    private LocalDateTime timeCreate;
    private LocalDateTime timeUpdate;
}
