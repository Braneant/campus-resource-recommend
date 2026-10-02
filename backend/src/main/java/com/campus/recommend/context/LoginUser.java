package com.campus.recommend.context;

import lombok.Data;

/**
 * 当前登录用户信息（由 JwtInterceptor 写入，请求结束清除）
 * 通过 ThreadLocal 在 Controller / Service 间传递，避免层层透传参数
 */
@Data
public class LoginUser {

    /** 用户id：学生为 student_user.id，管理员为 admin.id */
    private Long userId;
    /** 角色：STUDENT / ADMIN */
    private String role;
    /** 登录名 */
    private String username;

    /** 角色常量 */
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_ADMIN = "ADMIN";
}
