package com.campus.recommend.vo;

import lombok.Data;

import java.io.Serializable;

/** 知识图谱关系 VO */
@Data
public class GraphEdge implements Serializable {
    private String source;
    private String target;
    /** 关系类型：BELONGS_TO / TEACHES / CONTAINS / PREREQUISITE / RELATED_TO / BELONGS_TO_KP */
    private String relType;
    /** 关系展示文案 */
    private String label;
}
