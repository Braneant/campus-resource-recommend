package com.campus.recommend.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/** 登录返回 VO：{token, role, user} */
@Data
public class LoginVO implements Serializable {
    private String token;
    private String role;
    /** 用户信息（学生或管理员脱敏对象） */
    private Map<String, Object> user;
}
