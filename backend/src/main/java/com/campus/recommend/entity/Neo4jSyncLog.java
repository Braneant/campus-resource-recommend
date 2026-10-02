package com.campus.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** Neo4j 同步日志表 neo4j_sync_log */
@Data
@TableName("neo4j_sync_log")
public class Neo4jSyncLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resourceId;
    /** 同步类型：1新增 2更新 3删除 */
    private Integer syncType;
    /** 状态：0待同步 1成功 2失败 */
    private Integer syncStatus;
    private String errorMsg;
    private LocalDateTime createTime;
}
