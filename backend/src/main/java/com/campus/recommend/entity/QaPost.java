package com.campus.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 问答帖子表 qa_post（问答广场） */
@Data
@TableName("qa_post")
public class QaPost {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String title;
    private String content;
    private Long categoryId;
    private String tags;
    private Integer viewCount;
    private Integer replyCount;
    private Integer likeCount;
    /** 状态：0正常 1已结帖 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
