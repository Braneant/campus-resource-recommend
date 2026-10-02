package com.campus.recommend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 学生注册入参 */
@Data
public class RegisterDTO {
    @NotBlank(message = "学号不能为空")
    private String studentNo;
    @NotBlank(message = "登录名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
    private String nickname;
    @NotNull(message = "学院id不能为空")
    private Long collegeId;
    private Long majorId;
    private String email;
}
