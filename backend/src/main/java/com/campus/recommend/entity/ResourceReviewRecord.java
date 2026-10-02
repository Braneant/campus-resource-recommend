package com.campus.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 资源审核记录表 resource_review_record */
@Data
@TableName("resource_review_record")
public class ResourceReviewRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resourceId;
    private Long submitterId;
    private Long reviewerId;
    /** 动作：1提交 2通过 3驳回 */
    private Integer action;
    private String reviewComment;
    private LocalDateTime createTime;
}
