package com.campus.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 资源需求表 resource_requirement（需求互助） */
@Data
@TableName("resource_requirement")
public class ResourceRequirement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String title;
    private String content;
    private Long categoryId;
    private Long collegeId;
    /** 状态：0待解决 1已解决 */
    private Integer status;
    private Integer replyCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
