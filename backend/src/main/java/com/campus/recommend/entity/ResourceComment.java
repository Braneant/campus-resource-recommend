package com.campus.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 资源评论表 resource_comment */
@Data
@TableName("resource_comment")
public class ResourceComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resourceId;
    private Long studentId;
    private String content;
    private Long parentId;
    private Integer likeCount;
    private LocalDateTime createTime;
}
