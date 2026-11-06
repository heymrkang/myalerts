package com.mrkang.myalerts.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 사용자 DB 접근 매퍼 */
@Mapper
public interface UserDao {

    // 1. 사용자 ID로 활성 사용자 단건 조회
    @Select("SELECT user_id, user_pw, is_del, refresh_token, time_create, time_update " +
            "FROM tb_user WHERE user_id = #{userId} AND is_del = 'N'")
    User findById(@Param("userId") String userId);

    // 2. 신규 사용자 등록 (time_update는 NULL)
    @Insert("INSERT INTO tb_user (user_id, user_pw, is_del, refresh_token, time_create, time_update) " +
            "VALUES (#{userId}, #{userPw}, 'N', NULL, CURRENT_TIMESTAMP, NULL)")
    int insert(User user);

    // 3. 리프래시 토큰 업데이트 (time_update를 현재 시간으로 갱신)
    @Update("UPDATE tb_user SET refresh_token = #{refreshToken}, time_update = CURRENT_TIMESTAMP " +
            "WHERE user_id = #{userId}")
    int updateRefreshToken(@Param("userId") String userId, @Param("refreshToken") String refreshToken);

    // 4. 로그아웃 시 리프래시 토큰 제거
    @Update("UPDATE tb_user SET refresh_token = NULL, time_update = CURRENT_TIMESTAMP " +
            "WHERE user_id = #{userId}")
    int clearRefreshToken(@Param("userId") String userId);
}
