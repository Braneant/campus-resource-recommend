package com.campus.recommend.vo;

import lombok.Data;

import java.io.Serializable;

/** 数据大屏总览卡片 VO */
@Data
public class DashboardStatsVO implements Serializable {
    private Long userCount;
    private Long resourceCount;
    private Long resourcePassed;
    private Long requirementCount;
    private Long qaCount;
    private Long kpCount;
}
