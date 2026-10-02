package com.campus.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 课程表 course */
@Data
@TableName("course")
public class Course {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long collegeId;
    private Long majorId;
    private String name;
    private String code;
    private String teacher;
    private BigDecimal credit;
    private String semester;
    private String description;
    private LocalDateTime createTime;
}
