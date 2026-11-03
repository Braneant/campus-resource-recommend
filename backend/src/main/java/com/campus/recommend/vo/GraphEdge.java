package com.campus.recommend.vo;

import lombok.Data;

import java.io.Serializable;

/** 知识图谱关系 VO */
@Data
public class GraphEdge implements Serializable {
    private String source;
    private String target;
    /** 起点/终点节点标签（College/Major/Course/KnowledgePoint/Resource）。
     *  节点 id 只在同类内唯一（学院 1 和专业 1 都是 1），加上标签前端才能拼出全局唯一的画布键。 */
    private String sourceType;
    private String targetType;
    /** 起点/终点的显示名（title 或 name）。不带这个字段时，前端展开邻居只能拿 id 当文字显示。 */
    private String sourceLabel;
    private String targetLabel;
    /** 关系类型：BELONGS_TO / TEACHES / CONTAINS / PREREQUISITE / RELATED_TO / BELONGS_TO_KP */
    private String relType;
    /** 关系展示文案 */
    private String label;
}
