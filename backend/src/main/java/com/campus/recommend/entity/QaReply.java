package com.campus.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 问答回复表 qa_reply */
@Data
@TableName("qa_reply")
public class QaReply {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long studentId;
    private String content;
    private Long parentId;
    private Integer likeCount;
    /** 是否最佳答案：0否 1是 */
    private Integer isBest;
    private LocalDateTime createTime;
}
