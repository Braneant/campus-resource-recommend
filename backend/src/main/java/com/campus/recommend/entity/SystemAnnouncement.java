package com.campus.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 系统公告表 system_announcement */
@Data
@TableName("system_announcement")
public class SystemAnnouncement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminId;
    private String title;
    private String content;
    /** 优先级：1普通 2重要 3紧急 */
    private Integer priority;
    /** 发布状态：0草稿 1已发布 */
    private Integer publishStatus;
    private LocalDateTime createTime;
    private LocalDateTime publishTime;
}
