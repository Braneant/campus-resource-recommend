package com.campus.recommend.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/** 子图 VO（节点 + 边） */
@Data
public class GraphSubgraph implements Serializable {
    private List<GraphNode> nodes;
    private List<GraphEdge> edges;
}
