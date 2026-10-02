package com.campus.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 评分行为表 rating_behavior（ItemCF 显式数据源，score/5.0） */
@Data
@TableName("rating_behavior")
public class RatingBehavior {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long resourceId;
    /** 评分 1~5 */
    private Integer score;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
