package com.campus.recommend.controller;

import com.campus.recommend.annotation.RequireRole;
import com.campus.recommend.common.Result;
import com.campus.recommend.dto.GraphNodeCreateDTO;
import com.campus.recommend.dto.GraphRelationDTO;
import com.campus.recommend.dto.GraphSyncDTO;
import com.campus.recommend.service.GraphService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 管理端图谱运维 /api/admin/graph（共 5 个接口） */
@Slf4j
@RestController
@RequestMapping("/api/admin/graph")
@RequireRole("ADMIN")
public class AdminGraphController {

    @Autowired
    private GraphService graphService;

    /** 新增节点 */
    @PostMapping("/node")
    public Result<Void> createNode(@RequestBody GraphNodeCreateDTO dto) {
        graphService.createNode(dto);
        return Result.success();
    }

    /** 删除节点（DETACH DELETE） */
    @DeleteMapping("/node/{type}/{id}")
    public Result<Void> deleteNode(@PathVariable String type, @PathVariable String id) {
        graphService.deleteNode(type, id);
        return Result.success();
    }

    /** 新增关系 */
    @PostMapping("/relation")
    public Result<Void> createRelation(@RequestBody GraphRelationDTO dto) {
        graphService.createRelation(dto);
        return Result.success();
    }

    /** 删除关系 */
    @DeleteMapping("/relation")
    public Result<Void> deleteRelation(@RequestBody GraphRelationDTO dto) {
        graphService.deleteRelation(dto);
        return Result.success();
    }

    /** 全量同步 MySQL → Neo4j */
    @PostMapping("/sync")
    public Result<Map<String, Object>> fullSync(@RequestBody(required = false) GraphSyncDTO dto) {
        int count = graphService.fullSync(dto);
        return Result.success(Map.of("synced", count));
    }
}
