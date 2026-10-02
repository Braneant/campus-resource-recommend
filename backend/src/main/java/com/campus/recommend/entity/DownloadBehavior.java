package com.campus.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 下载行为表 download_behavior（ItemCF 数据源，权重 0.8） */
@Data
@TableName("download_behavior")
public class DownloadBehavior {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long resourceId;
    private LocalDateTime createTime;
}
