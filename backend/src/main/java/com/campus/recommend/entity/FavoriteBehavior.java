package com.campus.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 收藏行为表 favorite_behavior（ItemCF 数据源，权重 1.0） */
@Data
@TableName("favorite_behavior")
public class FavoriteBehavior {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long resourceId;
    private LocalDateTime createTime;
}
