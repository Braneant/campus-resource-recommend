package com.campus.recommend.controller;

import com.campus.recommend.common.PageResult;
import com.campus.recommend.common.Result;
import com.campus.recommend.dto.CommentDTO;
import com.campus.recommend.dto.RatingDTO;
import com.campus.recommend.dto.ResourceUploadDTO;
import com.campus.recommend.entity.LearningResource;
import com.campus.recommend.service.ResourceService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** 资源模块 /api/resource（学生侧） */
@Slf4j
@RestController
@RequestMapping("/api/resource")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    /** 4.1 资源检索（公开） */
    @GetMapping("/search")
    public Result<PageResult<LearningResource>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(resourceService.search(keyword, categoryId, collegeId, courseId, type, sort, pageNum, pageSize, null));
    }

    /** 4.2 资源详情（公开） */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(resourceService.detail(id));
    }

    /** 4.3 资源预览（计浏览量，登录） */
    @GetMapping("/{id}/preview")
    public Result<Map<String, Object>> preview(@PathVariable Long id) {
        return Result.success(resourceService.preview(id));
    }

    /** 4.4 资源下载（写下载行为，登录） */
    @GetMapping("/{id}/download")
    public Result<Map<String, Object>> download(@PathVariable Long id) {
        return Result.success(resourceService.download(id));
    }

    /** 4.5 评论列表 */
    @GetMapping("/{id}/comments")
    public Result<PageResult<Map<String, Object>>> comments(@PathVariable Long id,
                                                            @RequestParam(defaultValue = "1") int pageNum,
                                                            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(resourceService.comments(id, pageNum, pageSize));
    }

    /** 4.6 发表评论 */
    @PostMapping("/{id}/comments")
    public Result<Map<String, Object>> postComment(@PathVariable Long id, @Valid @RequestBody CommentDTO dto) {
        return Result.success(resourceService.postComment(id, dto));
    }

    /** 4.7 收藏 */
    @PostMapping("/{id}/favorite")
    public Result<Void> favorite(@PathVariable Long id) {
        resourceService.favorite(id);
        return Result.success();
    }

    /** 4.8 取消收藏 */
    @DeleteMapping("/{id}/favorite")
    public Result<Void> unfavorite(@PathVariable Long id) {
        resourceService.unfavorite(id);
        return Result.success();
    }

    /** 4.9 评分 */
    @PostMapping("/{id}/rating")
    public Result<Void> rating(@PathVariable Long id, @Valid @RequestBody RatingDTO dto) {
        resourceService.rating(id, dto.getScore());
        return Result.success();
    }

    /** 4.10 上传资源（multipart/form-data，进入待审核） */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestPart(value = "file", required = false) MultipartFile file,
                                              @ModelAttribute ResourceUploadDTO dto) {
        return Result.success(resourceService.upload(dto, file));
    }
}
