package com.campus.recommend.controller;

import com.campus.recommend.common.PageResult;
import com.campus.recommend.common.Result;
import com.campus.recommend.context.LoginContext;
import com.campus.recommend.dto.QaDTO;
import com.campus.recommend.dto.QaReplyDTO;
import com.campus.recommend.service.QaService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 问答广场模块 /api/qa */
@Slf4j
@RestController
@RequestMapping("/api/qa")
public class QaController {

    @Autowired
    private QaService qaService;

    /** 1 帖子列表 */
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(qaService.list(keyword, tags, categoryId, status, pageNum, pageSize));
    }

    /** 2 发帖 */
    @PostMapping
    public Result<Map<String, Object>> publish(@Valid @RequestBody QaDTO dto) {
        Long stuId = LoginContext.getStudentId();
        return Result.success(qaService.publish(dto, stuId));
    }

    /** 3 帖子详情 + 回复 */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(qaService.detail(id));
    }

    /** 4 回复 */
    @PostMapping("/{id}/reply")
    public Result<Map<String, Object>> reply(@PathVariable Long id, @Valid @RequestBody QaReplyDTO dto) {
        Long stuId = LoginContext.getStudentId();
        return Result.success(qaService.reply(id, dto, stuId));
    }

    /** 5 设为最佳答案 */
    @PostMapping("/reply/{replyId}/best")
    public Result<Void> bestReply(@PathVariable Long replyId) {
        Long stuId = LoginContext.getStudentId();
        qaService.setBest(replyId, stuId);
        return Result.success();
    }
}
