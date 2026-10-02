package com.campus.recommend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.PageResult;
import com.campus.recommend.common.ResultCode;
import com.campus.recommend.context.LoginContext;
import com.campus.recommend.entity.LearningResource;
import com.campus.recommend.entity.ResourceReviewRecord;
import com.campus.recommend.mapper.LearningResourceMapper;
import com.campus.recommend.mapper.ResourceReviewRecordMapper;
import com.campus.recommend.mapper.StudentUserMapper;
import com.campus.recommend.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资源审核服务（管理端）：审核列表 / 通过（触发 Neo4j 同步）/ 驳回 / 全量列表
 */
@Slf4j
@Service
public class ResourceAuditService {

    @Autowired
    private LearningResourceMapper resourceMapper;
    @Autowired
    private ResourceReviewRecordMapper reviewRecordMapper;
    @Autowired
    private StudentUserMapper studentUserMapper;
    @Autowired
    private Neo4jSyncService neo4jSyncService;

    /** 审核列表（status：0待审 / 2驳回） */
    public PageResult<Map<String, Object>> auditList(Integer status, int pageNum, int pageSize) {
        pageNum = pageNum < 1 ? 1 : pageNum;
        pageSize = pageSize < 1 ? 10 : pageSize;
        LambdaQueryWrapper<LearningResource> qw = new LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(LearningResource::getStatus, status);
        } else {
            qw.in(LearningResource::getStatus, 0, 2);
        }
        qw.orderByDesc(LearningResource::getCreateTime);
        IPage<LearningResource> page = resourceMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        List<Map<String, Object>> list = page.getRecords().stream().map(r -> {
            Map<String, Object> m = JsonUtil.toMap(r);
            // 提交人信息与提交时间
            ResourceReviewRecord submit = reviewRecordMapper.selectOne(new LambdaQueryWrapper<ResourceReviewRecord>()
                    .eq(ResourceReviewRecord::getResourceId, r.getId())
                    .eq(ResourceReviewRecord::getAction, 1)
                    .orderByDesc(ResourceReviewRecord::getCreateTime).last("LIMIT 1"));
            if (submit != null) {
                m.put("submitterName", studentUserMapper.selectById(submit.getSubmitterId()) != null
                        ? studentUserMapper.selectById(submit.getSubmitterId()).getNickname() : null);
                m.put("submitTime", submit.getCreateTime());
            }
            return m;
        }).toList();
        return new PageResult<>(list, page.getTotal(), pageNum, pageSize);
    }

    /** 审核通过：status=1 + 审核记录 + Neo4j 同步 */
    public void approve(Long id, Long kpId, Long reviewerId) {
        LearningResource r = resourceMapper.selectById(id);
        if (r == null) throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在");
        r.setStatus(1);
        r.setAuditTime(LocalDateTime.now());
        if (kpId != null) r.setPrimaryKpId(kpId);
        resourceMapper.updateById(r);

        ResourceReviewRecord rec = new ResourceReviewRecord();
        rec.setResourceId(id);
        rec.setReviewerId(reviewerId);
        rec.setAction(2);
        rec.setCreateTime(LocalDateTime.now());
        reviewRecordMapper.insert(rec);

        // 触发 Neo4j 同步（审核通过 → 入图）
        try {
            neo4jSyncService.syncResource(r, 1);
        } catch (Exception e) {
            log.warn("资源 {} 审核通过，但 Neo4j 同步失败：{}", id, e.getMessage());
        }
    }

    /** 审核驳回 */
    public void reject(Long id, String comment, Long reviewerId) {
        LearningResource r = resourceMapper.selectById(id);
        if (r == null) throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在");
        r.setStatus(2);
        resourceMapper.updateById(r);

        ResourceReviewRecord rec = new ResourceReviewRecord();
        rec.setResourceId(id);
        rec.setReviewerId(reviewerId);
        rec.setAction(3);
        rec.setReviewComment(comment);
        rec.setCreateTime(LocalDateTime.now());
        reviewRecordMapper.insert(rec);
    }

    /** 管理端全量资源列表 */
    public PageResult<Map<String, Object>> adminList(Integer status, String keyword, int pageNum, int pageSize) {
        pageNum = pageNum < 1 ? 1 : pageNum;
        pageSize = pageSize < 1 ? 10 : pageSize;
        LambdaQueryWrapper<LearningResource> qw = new LambdaQueryWrapper<>();
        if (status != null) qw.eq(LearningResource::getStatus, status);
        if (StringUtils.hasText(keyword)) qw.like(LearningResource::getTitle, keyword);
        qw.orderByDesc(LearningResource::getCreateTime);
        IPage<LearningResource> page = resourceMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        List<Map<String, Object>> list = page.getRecords().stream().map(JsonUtil::toMap).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal(), pageNum, pageSize);
    }
}
