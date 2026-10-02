package com.campus.recommend.controller;

import com.campus.recommend.common.PageResult;
import com.campus.recommend.common.Result;
import com.campus.recommend.context.LoginContext;
import com.campus.recommend.dto.RequirementDTO;
import com.campus.recommend.dto.RequirementReplyDTO;
import com.campus.recommend.service.RequirementService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 需求互助模块 /api/requirement */
@Slf4j
@RestController
@RequestMapping("/api/requirement")
public class RequirementController {

    @Autowired
    private RequirementService requirementService;

    /** 1 需求列表 */
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(requirementService.list(keyword, collegeId, status, pageNum, pageSize));
    }

    /** 2 发布需求 */
    @PostMapping
    public Result<Map<String, Object>> publish(@Valid @RequestBody RequirementDTO dto) {
        Long stuId = LoginContext.getStudentId();
        return Result.success(requirementService.publish(dto, stuId));
    }

    /** 3 需求详情 + 回复 */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(requirementService.detail(id));
    }

    /** 4 回复需求 */
    @PostMapping("/{id}/reply")
    public Result<Map<String, Object>> reply(@PathVariable Long id, @Valid @RequestBody RequirementReplyDTO dto) {
        Long stuId = LoginContext.getStudentId();
        return Result.success(requirementService.reply(id, dto, stuId));
    }

    /** 5 标记已解决 */
    @PostMapping("/{id}/resolve")
    public Result<Void> resolve(@PathVariable Long id) {
        requirementService.resolve(id);
        return Result.success();
    }
}
