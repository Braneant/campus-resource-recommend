package com.campus.recommend.controller;

import com.campus.recommend.common.Result;
import com.campus.recommend.service.GraphService;
import com.campus.recommend.vo.GraphEdge;
import com.campus.recommend.vo.GraphNode;
import com.campus.recommend.vo.GraphSubgraph;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 知识图谱可视化模块 /api/graph（公开） */
@Slf4j
@RestController
@RequestMapping("/api/graph")
public class GraphController {

    @Autowired
    private GraphService graphService;

    /** 9.1 节点列表 / 搜索 */
    @GetMapping("/nodes")
    public Result<List<GraphNode>> nodes(@RequestParam(required = false) String type,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer limit) {
        return Result.success(graphService.listNodes(type, keyword, limit));
    }

    /** 9.2 关系列表 */
    @GetMapping("/edges")
    public Result<List<GraphEdge>> edges(@RequestParam(required = false) String fromId,
                                         @RequestParam(required = false) String fromType,
                                         @RequestParam(required = false) String relType) {
        return Result.success(graphService.listEdges(fromId, fromType, relType));
    }

    /** 9.3 多跳子图（力导向图渲染） */
    @GetMapping("/subgraph")
    public Result<GraphSubgraph> subgraph(@RequestParam(required = false) String resourceId,
                                          @RequestParam(required = false) String kpId,
                                          @RequestParam(defaultValue = "2") int depth) {
        return Result.success(graphService.subgraph(resourceId, kpId, depth));
    }

    /** 9.4 节点详情 + 邻居 */
    @GetMapping("/node/{type}/{id}")
    public Result<Map<String, Object>> nodeDetail(@PathVariable String type, @PathVariable String id) {
        return Result.success(graphService.nodeDetail(type, id));
    }
}
