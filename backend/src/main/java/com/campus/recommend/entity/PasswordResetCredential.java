package com.campus.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 每位学生只保留最近一条凭证；不持久化邮件中的明文验证码。 */
@Getter
@Setter
@TableName("password_reset_credential")
public class PasswordResetCredential {
    @TableId(value = "student_id", type = IdType.INPUT)
    private Long studentId;
    private String tokenHash;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime consumedAt;
}
