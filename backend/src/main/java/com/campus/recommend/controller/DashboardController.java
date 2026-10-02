package com.campus.recommend.controller;

import com.campus.recommend.annotation.RequireRole;
import com.campus.recommend.common.Result;
import com.campus.recommend.service.DashboardService;
import com.campus.recommend.vo.DashboardStatsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 数据大屏统计 /api/admin/dashboard（共 5 个接口，ECharts 聚合数据） */
@Slf4j
@RestController
@RequestMapping("/api/admin/dashboard")
@RequireRole("ADMIN")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /** 总览卡片 */
    @GetMapping("/stats")
    public Result<DashboardStatsVO> stats() {
        return Result.success(dashboardService.stats());
    }

    /** 资源增长趋势（折线） */
    @GetMapping("/resource-trend")
    public Result<Map<String, Object>> resourceTrend() {
        return Result.success(dashboardService.resourceTrend());
    }

    /** 资源分类占比（饼图） */
    @GetMapping("/category-dist")
    public Result<Map<String, Object>> categoryDist() {
        return Result.success(dashboardService.categoryDist());
    }

    /** 知识点热度 Top（柱状，来自 Neo4j） */
    @GetMapping("/kp-heat")
    public Result<Map<String, Object>> kpHeat() {
        return Result.success(dashboardService.kpHeat());
    }

    /** 学院资源量排行（柱状） */
    @GetMapping("/college-rank")
    public Result<Map<String, Object>> collegeRank() {
        return Result.success(dashboardService.collegeRank());
    }
}
