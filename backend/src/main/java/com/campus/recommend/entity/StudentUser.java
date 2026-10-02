package com.campus.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 学生用户表 student_user */
@Data
@TableName("student_user")
public class StudentUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String studentNo;
    private String username;
    private String password;
    private String nickname;
    private String realName;
    /** 性别：0未知 1男 2女 */
    private Integer gender;
    private Long collegeId;
    private Long majorId;
    private String grade;
    private String email;
    private String phone;
    private String avatar;
    /** 状态：0禁用 1正常 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
