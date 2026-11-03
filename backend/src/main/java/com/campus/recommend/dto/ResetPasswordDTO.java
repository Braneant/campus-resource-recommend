package com.campus.recommend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 敏感字段不生成 toString，避免凭证或新密码被意外记录。 */
@Getter
@Setter
public class ResetPasswordDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名过长")
    private String username;
    @NotBlank(message = "邮件验证码不能为空")
    @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "请粘贴邮件中的完整验证码")
    private String code;
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 72, message = "密码须为至少8位且不超过72字节")
    private String newPassword;
}
