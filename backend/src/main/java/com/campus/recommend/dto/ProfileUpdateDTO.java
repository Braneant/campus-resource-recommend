package com.campus.recommend.dto;

import lombok.Data;

/** 更新个人资料入参（03 契约 3.2） */
@Data
public class ProfileUpdateDTO {
    private String nickname;
    private String realName;
    private Integer gender;
    private String email;
    private String phone;
    private String avatar;
    private Long collegeId;
    private Long majorId;
}
