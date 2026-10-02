package com.campus.recommend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.recommend.common.PageResult;
import com.campus.recommend.common.ResultCode;
import com.campus.recommend.common.BusinessException;
import com.campus.recommend.dto.RequirementDTO;
import com.campus.recommend.dto.RequirementReplyDTO;
import com.campus.recommend.entity.RequirementReply;
import com.campus.recommend.entity.ResourceRequirement;
import com.campus.recommend.entity.StudentUser;
import com.campus.recommend.mapper.RequirementReplyMapper;
import com.campus.recommend.mapper.ResourceRequirementMapper;
import com.campus.recommend.mapper.StudentUserMapper;
import com.campus.recommend.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 需求互助服务 */
@Slf4j
@Service
public class RequirementService {

    @Autowired
    private ResourceRequirementMapper requirementMapper;
    @Autowired
    private RequirementReplyMapper replyMapper;
    @Autowired
    private StudentUserMapper studentUserMapper;

    public PageResult<Map<String, Object>> list(String keyword, Long collegeId, Integer status,
                                                int pageNum, int pageSize) {
        pageNum = pageNum < 1 ? 1 : pageNum;
        pageSize = pageSize < 1 ? 10 : pageSize;
        LambdaQueryWrapper<ResourceRequirement> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) qw.like(ResourceRequirement::getTitle, keyword);
        if (collegeId != null) qw.eq(ResourceRequirement::getCollegeId, collegeId);
        if (status != null) qw.eq(ResourceRequirement::getStatus, status);
        qw.orderByDesc(ResourceRequirement::getCreateTime);
        IPage<ResourceRequirement> page = requirementMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        List<Map<String, Object>> list = page.getRecords().stream().map(this::toMapWithName).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal(), pageNum, pageSize);
    }

    public Map<String, Object> publish(RequirementDTO dto, Long studentId) {
        ResourceRequirement r = new ResourceRequirement();
        r.setStudentId(studentId);
        r.setTitle(dto.getTitle());
        r.setContent(dto.getContent());
        r.setCategoryId(dto.getCategoryId());
        r.setCollegeId(dto.getCollegeId());
        r.setStatus(0);
        r.setReplyCount(0);
        r.setCreateTime(LocalDateTime.now());
        requirementMapper.insert(r);
        Map<String, Object> m = new HashMap<>();
        m.put("requirementId", r.getId());
        return m;
    }

    public Map<String, Object> detail(Long id) {
        ResourceRequirement r = requirementMapper.selectById(id);
        if (r == null) throw new BusinessException(ResultCode.NOT_FOUND, "需求不存在");
        Map<String, Object> m = toMapWithName(r);
        List<RequirementReply> replies = replyMapper.selectList(new LambdaQueryWrapper<RequirementReply>()
                .eq(RequirementReply::getRequirementId, id).orderByAsc(RequirementReply::getCreateTime));
        List<Map<String, Object>> replyList = replies.stream().map(rp -> {
            Map<String, Object> rm = JsonUtil.toMap(rp);
            StudentUser stu = studentUserMapper.selectById(rp.getStudentId());
            rm.put("studentName", stu != null ? (StringUtils.hasText(stu.getNickname()) ? stu.getNickname() : stu.getUsername()) : "匿名");
            return rm;
        }).toList();
        m.put("replies", replyList);
        return m;
    }

    public Map<String, Object> reply(Long id, RequirementReplyDTO dto, Long studentId) {
        ResourceRequirement r = requirementMapper.selectById(id);
        if (r == null) throw new BusinessException(ResultCode.NOT_FOUND, "需求不存在");
        RequirementReply rp = new RequirementReply();
        rp.setRequirementId(id);
        rp.setStudentId(studentId);
        rp.setContent(dto.getContent());
        rp.setLikeCount(0);
        rp.setCreateTime(LocalDateTime.now());
        replyMapper.insert(rp);
        // 回复数 +1
        r.setReplyCount((r.getReplyCount() == null ? 0 : r.getReplyCount()) + 1);
        requirementMapper.updateById(r);
        Map<String, Object> m = new HashMap<>();
        m.put("replyId", rp.getId());
        return m;
    }

    public void resolve(Long id) {
        ResourceRequirement r = requirementMapper.selectById(id);
        if (r == null) throw new BusinessException(ResultCode.NOT_FOUND, "需求不存在");
        r.setStatus(1);
        requirementMapper.updateById(r);
    }

    private Map<String, Object> toMapWithName(ResourceRequirement r) {
        Map<String, Object> m = JsonUtil.toMap(r);
        StudentUser stu = studentUserMapper.selectById(r.getStudentId());
        m.put("studentName", stu != null ? (StringUtils.hasText(stu.getNickname()) ? stu.getNickname() : stu.getUsername()) : "匿名");
        return m;
    }
}
