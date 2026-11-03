package com.campus.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.recommend.entity.PasswordResetCredential;
import com.campus.recommend.entity.StudentUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface PasswordResetCredentialMapper extends BaseMapper<PasswordResetCredential> {
    // 所有签发/消费先锁同一学生行，包括首次签发时凭证行尚不存在的情况。
    @Select("SELECT * FROM student_user WHERE student_no = #{studentNo} FOR UPDATE")
    StudentUser lockStudentByNumber(@Param("studentNo") String studentNo);

    @Select("SELECT * FROM student_user WHERE username = #{username} FOR UPDATE")
    StudentUser lockStudentByUsername(@Param("username") String username);

    @Update("UPDATE password_reset_credential SET token_hash=#{tokenHash}, issued_at=#{issuedAt}, "
            + "expires_at=#{expiresAt}, consumed_at=NULL WHERE student_id=#{studentId}")
    int replace(PasswordResetCredential credential);

    @Update("UPDATE password_reset_credential SET consumed_at=#{now} "
            + "WHERE student_id=#{studentId} AND token_hash=#{tokenHash} AND consumed_at IS NULL AND expires_at > #{now}")
    int consume(@Param("studentId") Long studentId, @Param("tokenHash") String tokenHash,
                @Param("now") LocalDateTime now);
}
