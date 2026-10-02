package com.campus.recommend.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** 单条推荐解释 VO（/api/recommend/explain） */
@Data
public class RecommendExplainVO implements Serializable {
    private Long resourceId;
    private String title;
    /** 命中来源列表：cf / kg / hot */
    private List<String> sources;
    /** 综合解释文案 */
    private String reason;
    /** 各路分数明细：{cf, kg, hot, final} */
    private Map<String, Double> scoreBreakdown;
}
