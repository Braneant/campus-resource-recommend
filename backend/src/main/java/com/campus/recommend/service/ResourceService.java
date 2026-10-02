package com.campus.recommend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.PageResult;
import com.campus.recommend.common.ResultCode;
import com.campus.recommend.context.LoginContext;
import com.campus.recommend.dto.CommentDTO;
import com.campus.recommend.dto.ResourceUploadDTO;
import com.campus.recommend.entity.*;
import com.campus.recommend.mapper.*;
import com.campus.recommend.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 资源服务：检索 / 详情 / 预览 / 下载 / 评论 / 收藏 / 评分 / 上传 / 热度重算
 */
@Slf4j
@Service
public class ResourceService {

    @Autowired
    private LearningResourceMapper resourceMapper;
    @Autowired
    private ResourceCommentMapper commentMapper;
    @Autowired
    private FavoriteBehaviorMapper favoriteMapper;
    @Autowired
    private DownloadBehaviorMapper downloadMapper;
    @Autowired
    private RatingBehaviorMapper ratingMapper;
    @Autowired
    private StudentUserMapper studentUserMapper;
    @Autowired
    private ResourceReviewRecordMapper reviewRecordMapper;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    /**
     * 资源检索（公开）
     * sort: hot(热度) / new(最新) / rating(评分)，默认 hot
     */
    public PageResult<LearningResource> search(String keyword, Long categoryId, Long collegeId,
                                               Long courseId, Integer type, String sort,
                                               Integer pageNum, Integer pageSize, Integer status) {
        pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        pageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        LambdaQueryWrapper<LearningResource> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(LearningResource::getTitle, keyword);
        }
        if (categoryId != null) qw.eq(LearningResource::getCategoryId, categoryId);
        if (collegeId != null) qw.eq(LearningResource::getCollegeId, collegeId);
        if (courseId != null) qw.eq(LearningResource::getCourseId, courseId);
        if (type != null) qw.eq(LearningResource::getType, type);
        // 公开检索默认只看已通过资源；管理端可指定 status
        if (status != null) {
            qw.eq(LearningResource::getStatus, status);
        } else {
            qw.eq(LearningResource::getStatus, 1);
        }
        if ("new".equals(sort)) {
            qw.orderByDesc(LearningResource::getCreateTime);
        } else if ("rating".equals(sort)) {
            qw.orderByDesc(LearningResource::getAvgRating);
        } else {
            qw.orderByDesc(LearningResource::getHeat);
        }
        IPage<LearningResource> page = resourceMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        return new PageResult<>(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    /** 资源详情（公开）：资源完整对象 + kpId + commentCount */
    public Map<String, Object> detail(Long id) {
        LearningResource r = resourceMapper.selectById(id);
        if (r == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在");
        }
        long commentCount = commentMapper.selectCount(
                new LambdaQueryWrapper<ResourceComment>().eq(ResourceComment::getResourceId, id));
        Map<String, Object> m = JsonUtil.toMap(r);
        m.put("kpId", r.getPrimaryKpId());
        m.put("commentCount", commentCount);
        return m;
    }

    /** 资源预览（登录）：计浏览量 */
    public Map<String, Object> preview(Long id) {
        Long stuId = LoginContext.getStudentId();
        if (stuId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        // 浏览量 +1
        resourceMapper.update(null, new UpdateWrapper<LearningResource>()
                .eq("id", id).setSql("view_count = view_count + 1"));
        LearningResource r = resourceMapper.selectById(id);
        Map<String, Object> m = new HashMap<>();
        m.put("fileUrl", r.getFileUrl());
        m.put("canPreview", r.getFileUrl() != null);
        return m;
    }

    /** 资源下载（登录）：写下载行为 + 下载量 +1（幂等） */
    public Map<String, Object> download(Long id) {
        Long stuId = LoginContext.getStudentId();
        if (stuId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        long exists = downloadMapper.selectCount(new LambdaQueryWrapper<DownloadBehavior>()
                .eq(DownloadBehavior::getStudentId, stuId).eq(DownloadBehavior::getResourceId, id));
        if (exists == 0) {
            DownloadBehavior db = new DownloadBehavior();
            db.setStudentId(stuId);
            db.setResourceId(id);
            db.setCreateTime(LocalDateTime.now());
            downloadMapper.insert(db);
            resourceMapper.update(null, new UpdateWrapper<LearningResource>()
                    .eq("id", id).setSql("download_count = download_count + 1"));
        }
        LearningResource r = resourceMapper.selectById(id);
        Map<String, Object> m = new HashMap<>();
        m.put("fileUrl", r.getFileUrl());
        return m;
    }

    /** 评论列表（含昵称） */
    public PageResult<Map<String, Object>> comments(Long id, int pageNum, int pageSize) {
        pageNum = pageNum < 1 ? 1 : pageNum;
        pageSize = pageSize < 1 ? 10 : pageSize;
        IPage<ResourceComment> page = commentMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ResourceComment>()
                        .eq(ResourceComment::getResourceId, id)
                        .orderByDesc(ResourceComment::getCreateTime));
        List<Long> stuIds = page.getRecords().stream().map(ResourceComment::getStudentId).distinct().toList();
        Map<Long, String> nickMap;
        if (!stuIds.isEmpty()) {
            List<StudentUser> stus = studentUserMapper.selectList(
                    new LambdaQueryWrapper<StudentUser>().in(StudentUser::getId, stuIds));
            nickMap = stus.stream().collect(Collectors.toMap(StudentUser::getId,
                    s -> StringUtils.hasText(s.getNickname()) ? s.getNickname() : s.getUsername(), (a, b) -> a));
        } else {
            nickMap = Collections.emptyMap();
        }
        List<Map<String, Object>> list = page.getRecords().stream().map(c -> {
            Map<String, Object> m = JsonUtil.toMap(c);
            m.put("nickname", nickMap.getOrDefault(c.getStudentId(), "匿名"));
            return m;
        }).toList();
        return new PageResult<>(list, page.getTotal(), pageNum, pageSize);
    }

    /** 发表评论 */
    public Map<String, Object> postComment(Long id, CommentDTO dto) {
        Long stuId = LoginContext.getStudentId();
        if (stuId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        ResourceComment c = new ResourceComment();
        c.setResourceId(id);
        c.setStudentId(stuId);
        c.setContent(dto.getContent());
        c.setParentId(dto.getParentId());
        c.setLikeCount(0);
        c.setCreateTime(LocalDateTime.now());
        commentMapper.insert(c);
        Map<String, Object> m = new HashMap<>();
        m.put("commentId", c.getId());
        return m;
    }

    /** 收藏（切换/新增，幂等） */
    public void favorite(Long id) {
        Long stuId = LoginContext.getStudentId();
        if (stuId == null) throw new BusinessException(ResultCode.UNAUTHORIZED);
        long exists = favoriteMapper.selectCount(new LambdaQueryWrapper<FavoriteBehavior>()
                .eq(FavoriteBehavior::getStudentId, stuId).eq(FavoriteBehavior::getResourceId, id));
        if (exists == 0) {
            FavoriteBehavior f = new FavoriteBehavior();
            f.setStudentId(stuId);
            f.setResourceId(id);
            f.setCreateTime(LocalDateTime.now());
            favoriteMapper.insert(f);
            resourceMapper.update(null, new UpdateWrapper<LearningResource>()
                    .eq("id", id).setSql("favorite_count = favorite_count + 1"));
        }
    }

    /** 取消收藏 */
    public void unfavorite(Long id) {
        Long stuId = LoginContext.getStudentId();
        if (stuId == null) throw new BusinessException(ResultCode.UNAUTHORIZED);
        long deleted = favoriteMapper.delete(new LambdaQueryWrapper<FavoriteBehavior>()
                .eq(FavoriteBehavior::getStudentId, stuId).eq(FavoriteBehavior::getResourceId, id));
        if (deleted > 0) {
            resourceMapper.update(null, new UpdateWrapper<LearningResource>()
                    .eq("id", id).setSql("favorite_count = IF(favorite_count > 0, favorite_count - 1, 0)"));
        }
    }

    /** 评分：写入/更新评分行为，重算 avg_rating 与 rating_count */
    public void rating(Long id, int score) {
        Long stuId = LoginContext.getStudentId();
        if (stuId == null) throw new BusinessException(ResultCode.UNAUTHORIZED);
        RatingBehavior exist = ratingMapper.selectOne(new LambdaQueryWrapper<RatingBehavior>()
                .eq(RatingBehavior::getStudentId, stuId).eq(RatingBehavior::getResourceId, id));
        if (exist == null) {
            RatingBehavior rb = new RatingBehavior();
            rb.setStudentId(stuId);
            rb.setResourceId(id);
            rb.setScore(score);
            rb.setCreateTime(LocalDateTime.now());
            rb.setUpdateTime(LocalDateTime.now());
            ratingMapper.insert(rb);
        } else {
            exist.setScore(score);
            exist.setUpdateTime(LocalDateTime.now());
            ratingMapper.updateById(exist);
        }
        // 重算
        List<RatingBehavior> all = ratingMapper.selectList(
                new LambdaQueryWrapper<RatingBehavior>().eq(RatingBehavior::getResourceId, id));
        double sum = all.stream().mapToInt(RatingBehavior::getScore).sum();
        double avg = sum / all.size();
        LearningResource r = new LearningResource();
        r.setId(id);
        r.setAvgRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        r.setRatingCount(all.size());
        resourceMapper.updateById(r);
    }

    /** 学生上传资源（进入待审核） */
    public Map<String, Object> upload(ResourceUploadDTO dto, MultipartFile file) {
        Long stuId = LoginContext.getStudentId();
        if (stuId == null) throw new BusinessException(ResultCode.UNAUTHORIZED);

        LearningResource r = new LearningResource();
        r.setResourceNo("RES" + System.currentTimeMillis());
        r.setTitle(dto.getTitle());
        r.setCategoryId(dto.getCategoryId());
        r.setCollegeId(dto.getCollegeId());
        r.setMajorId(dto.getMajorId());
        r.setCourseId(dto.getCourseId());
        r.setPrimaryKpId(dto.getKpId());
        r.setType(dto.getType());
        r.setDescription(dto.getDescription());
        r.setAuthor(dto.getAuthor());
        r.setUploaderId(stuId);
        r.setUploaderType(1);
        r.setViewCount(0);
        r.setDownloadCount(0);
        r.setFavoriteCount(0);
        r.setAvgRating(BigDecimal.ZERO);
        r.setRatingCount(0);
        r.setHeat(BigDecimal.ZERO);
        r.setStatus(0);
        if (file != null && !file.isEmpty()) {
            r.setFileUrl(saveFile(file));
        }
        resourceMapper.insert(r);

        // 提交审核记录
        ResourceReviewRecord rec = new ResourceReviewRecord();
        rec.setResourceId(r.getId());
        rec.setSubmitterId(stuId);
        rec.setAction(1);
        rec.setCreateTime(LocalDateTime.now());
        reviewRecordMapper.insert(rec);

        Map<String, Object> m = new HashMap<>();
        m.put("resourceId", r.getId());
        m.put("resourceNo", r.getResourceNo());
        m.put("status", r.getStatus());
        return m;
    }

    /** 保存上传文件到本地目录，返回可访问 URL（演示用相对路径） */
    private String saveFile(MultipartFile file) {
        try {
            File dir = new File(uploadPath);
            if (!dir.exists()) dir.mkdirs();
            String original = file.getOriginalFilename();
            String suffix = original != null && original.contains(".")
                    ? original.substring(original.lastIndexOf('.')) : "";
            String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + suffix;
            file.transferTo(new File(dir, fileName));
            return "/files/" + fileName;
        } catch (Exception e) {
            log.warn("文件保存失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 热度重算（定时任务，每日 02:30）：对全部已通过资源，
     * 将 view/download/avgRating 做 min-max 归一化后加权得到综合热度 heat（0~1）。
     */
    @Scheduled(cron = "0 30 2 * * ?")
    public void recomputeHeat() {
        List<LearningResource> list = resourceMapper.selectList(
                new LambdaQueryWrapper<LearningResource>().eq(LearningResource::getStatus, 1));
        if (list.isEmpty()) return;
        double maxV = list.stream().mapToInt(r -> r.getViewCount() == null ? 0 : r.getViewCount()).max().orElse(0);
        double maxD = list.stream().mapToInt(r -> r.getDownloadCount() == null ? 0 : r.getDownloadCount()).max().orElse(0);
        double maxR = list.stream().map(r -> r.getAvgRating() == null ? 0 : r.getAvgRating().doubleValue()).max(Double::compare).orElse(0.0);
        for (LearningResource r : list) {
            double v = maxV > 0 ? (r.getViewCount() == null ? 0 : r.getViewCount()) / maxV : 0;
            double d = maxD > 0 ? (r.getDownloadCount() == null ? 0 : r.getDownloadCount()) / maxD : 0;
            double rt = maxR > 0 ? (r.getAvgRating() == null ? 0 : r.getAvgRating().doubleValue()) / maxR : 0;
            double heat = 0.5 * v + 0.3 * d + 0.2 * rt;
            r.setHeat(BigDecimal.valueOf(heat).setScale(4, RoundingMode.HALF_UP));
            resourceMapper.updateById(r);
        }
        log.info("热度重算完成，共 {} 条资源", list.size());
    }
}
