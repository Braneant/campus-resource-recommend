package com.campus.recommend.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/** 知识图谱节点 VO（供 ECharts 力导向图） */
@Data
public class GraphNode implements Serializable {
    /** 节点 id：College/Major/Course/KnowledgePoint 为 Long 字符串；Resource 为 resource_id */
    private String id;
    /** 节点类型：COLLEGE/MAJOR/COURSE/KNOWLEDGE_POINT/RESOURCE */
    private String type;
    /** 展示标签（节点名称/资源标题） */
    private String label;
    /** 热度（无则 null） */
    private Double heat;
    /** 其余属性（id/name/code 等），便于前端 tooltip */
    private Map<String, Object> properties;
}
