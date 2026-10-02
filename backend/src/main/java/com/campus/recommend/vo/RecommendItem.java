package com.campus.recommend.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 推荐项 VO（推荐接口统一返回结构）
 * resourceId/title/type/coverUrl/author/avgRating 来自 MySQL；
 * recommendScore 为三路融合后的最终分（0~1）；source 为 dominant 来源；reason 为可解释文案。
 */
@Data
public class RecommendItem implements Serializable {
    private Long resourceId;
    private String resourceNo;
    private String title;
    private Integer type;
    private String coverUrl;
    private String author;
    private Double avgRating;
    /** 融合后最终分 0~1 */
    private Double recommendScore;
    /** 主导来源：cf / kg / hot */
    private String source;
    /** 可解释推荐理由（规则模板生成） */
    private String reason;
}
