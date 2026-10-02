package com.campus.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 需求回复表 requirement_reply */
@Data
@TableName("requirement_reply")
public class RequirementReply {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long requirementId;
    private Long studentId;
    private String content;
    private Integer likeCount;
    private LocalDateTime createTime;
}
