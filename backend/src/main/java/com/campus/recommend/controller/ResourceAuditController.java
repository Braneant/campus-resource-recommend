package com.campus.recommend.controller;

import com.campus.recommend.annotation.RequireRole;
import com.campus.recommend.common.PageResult;
import com.campus.recommend.common.Result;
import com.campus.recommend.context.LoginContext;
import com.campus.recommend.dto.ResourceAuditDTO;
import com.campus.recommend.service.ResourceAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 资源审核（管理端）/api/admin/resource */
@Slf4j
@RestController
@RequestMapping("/api/admin/resource")
@RequireRole("ADMIN")
public class ResourceAuditController {

    @Autowired
    private ResourceAuditService auditService;

    /** 5.1 审核列表（status: 0待审 / 2驳回） */
    @GetMapping("/audit")
    public Result<PageResult<Map<String, Object>>> auditList(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(auditService.auditList(status, pageNum, pageSize));
    }

    /** 5.2 审核通过（触发 Neo4j 同步） */
    @PostMapping("/audit/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) ResourceAuditDTO dto) {
        Long reviewerId = LoginContext.get() != null ? LoginContext.get().getUserId() : null;
        Long kpId = dto != null ? dto.getKpId() : null;
        auditService.approve(id, kpId, reviewerId);
        return Result.success();
    }

    /** 5.3 审核驳回 */
    @PostMapping("/audit/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) ResourceAuditDTO dto) {
        Long reviewerId = LoginContext.get() != null ? LoginContext.get().getUserId() : null;
        String comment = dto != null ? dto.getReviewComment() : null;
        auditService.reject(id, comment, reviewerId);
        return Result.success();
    }

    /** 5.4 资源全量列表（管理） */
    @GetMapping
    public Result<PageResult<Map<String, Object>>> adminList(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(auditService.adminList(status, keyword, pageNum, pageSize));
    }
}
