package com.campus.recommend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.recommend.common.PageResult;
import com.campus.recommend.common.Result;
import com.campus.recommend.context.LoginContext;
import com.campus.recommend.entity.DownloadBehavior;
import com.campus.recommend.entity.FavoriteBehavior;
import com.campus.recommend.entity.LearningResource;
import com.campus.recommend.entity.RatingBehavior;
import com.campus.recommend.mapper.DownloadBehaviorMapper;
import com.campus.recommend.mapper.FavoriteBehaviorMapper;
import com.campus.recommend.mapper.LearningResourceMapper;
import com.campus.recommend.mapper.RatingBehaviorMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 个人中心的学习足迹：收藏、下载、上传、统计。
 * 原来的页面只有占位空态，数据其实都在 favorite_behavior / download_behavior / learning_resource 里。
 */
@RestController
@RequestMapping("/api/user")
public class UserResourceController {

    @Autowired
    private FavoriteBehaviorMapper favoriteMapper;
    @Autowired
    private DownloadBehaviorMapper downloadMapper;
    @Autowired
    private RatingBehaviorMapper ratingMapper;
    @Autowired
    private LearningResourceMapper resourceMapper;

    /** 个人中心四个数字 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Long uid = LoginContext.getStudentId();
        if (uid == null) return Result.error(1002, "请使用学生账号登录");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("favoriteCount", favoriteMapper.selectCount(
                new LambdaQueryWrapper<FavoriteBehavior>().eq(FavoriteBehavior::getStudentId, uid)));
        data.put("downloadCount", downloadMapper.selectCount(
                new LambdaQueryWrapper<DownloadBehavior>().eq(DownloadBehavior::getStudentId, uid)));
        data.put("uploadCount", resourceMapper.selectCount(
                new LambdaQueryWrapper<LearningResource>().eq(LearningResource::getUploaderId, uid)));
        data.put("ratingCount", ratingMapper.selectCount(
                new LambdaQueryWrapper<RatingBehavior>().eq(RatingBehavior::getStudentId, uid)));
        return Result.success(data);
    }

    /** 我的收藏 */
    @GetMapping("/favorites")
    public Result<PageResult<LearningResource>> favorites(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long uid = LoginContext.getStudentId();
        if (uid == null) return Result.error(1002, "请使用学生账号登录");

        Page<FavoriteBehavior> page = new Page<>(pageNum, pageSize);
        Page<FavoriteBehavior> result = favoriteMapper.selectPage(page,
                new LambdaQueryWrapper<FavoriteBehavior>()
                        .eq(FavoriteBehavior::getStudentId, uid)
                        .orderByDesc(FavoriteBehavior::getCreateTime));
        List<Long> ids = result.getRecords().stream()
                .map(FavoriteBehavior::getResourceId)
                .collect(Collectors.toList());
        return Result.success(toResourcePage(ids, result.getTotal(), pageNum, pageSize));
    }

    /** 我的下载 */
    @GetMapping("/downloads")
    public Result<PageResult<LearningResource>> downloads(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long uid = LoginContext.getStudentId();
        if (uid == null) return Result.error(1002, "请使用学生账号登录");

        Page<DownloadBehavior> page = new Page<>(pageNum, pageSize);
        Page<DownloadBehavior> result = downloadMapper.selectPage(page,
                new LambdaQueryWrapper<DownloadBehavior>()
                        .eq(DownloadBehavior::getStudentId, uid)
                        .orderByDesc(DownloadBehavior::getCreateTime));
        List<Long> ids = result.getRecords().stream()
                .map(DownloadBehavior::getResourceId)
                .collect(Collectors.toList());
        return Result.success(toResourcePage(ids, result.getTotal(), pageNum, pageSize));
    }

    /** 我的上传 */
    @GetMapping("/uploads")
    public Result<PageResult<LearningResource>> uploads(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long uid = LoginContext.getStudentId();
        if (uid == null) return Result.error(1002, "请使用学生账号登录");

        Page<LearningResource> page = new Page<>(pageNum, pageSize);
        Page<LearningResource> result = resourceMapper.selectPage(page,
                new LambdaQueryWrapper<LearningResource>()
                        .eq(LearningResource::getUploaderId, uid)
                        .orderByDesc(LearningResource::getCreateTime));
        return Result.success(new PageResult<>(result.getRecords(), result.getTotal(), pageNum, pageSize));
    }

    /** 按行为表的顺序取资源，避免列表顺序被资源表打乱 */
    private PageResult<LearningResource> toResourcePage(List<Long> ids, long total, int pageNum, int pageSize) {
        if (ids == null || ids.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), total, pageNum, pageSize);
        }
        Map<Long, LearningResource> byId = resourceMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(LearningResource::getId, r -> r, (a, b) -> a));
        List<LearningResource> list = ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new PageResult<>(list, total, pageNum, pageSize);
    }
}
