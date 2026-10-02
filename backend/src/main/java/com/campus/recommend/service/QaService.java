package com.campus.recommend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.PageResult;
import com.campus.recommend.common.ResultCode;
import com.campus.recommend.dto.QaDTO;
import com.campus.recommend.dto.QaReplyDTO;
import com.campus.recommend.entity.QaPost;
import com.campus.recommend.entity.QaReply;
import com.campus.recommend.entity.StudentUser;
import com.campus.recommend.mapper.QaPostMapper;
import com.campus.recommend.mapper.QaReplyMapper;
import com.campus.recommend.mapper.StudentUserMapper;
import com.campus.recommend.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 问答广场服务 */
@Slf4j
@Service
public class QaService {

    @Autowired
    private QaPostMapper postMapper;
    @Autowired
    private QaReplyMapper replyMapper;
    @Autowired
    private StudentUserMapper studentUserMapper;

    public PageResult<Map<String, Object>> list(String keyword, String tags, Long categoryId, Integer status,
                                                int pageNum, int pageSize) {
        pageNum = pageNum < 1 ? 1 : pageNum;
        pageSize = pageSize < 1 ? 10 : pageSize;
        LambdaQueryWrapper<QaPost> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) qw.like(QaPost::getTitle, keyword);
        if (StringUtils.hasText(tags)) qw.like(QaPost::getTags, tags);
        if (categoryId != null) qw.eq(QaPost::getCategoryId, categoryId);
        if (status != null) qw.eq(QaPost::getStatus, status);
        qw.orderByDesc(QaPost::getCreateTime);
        IPage<QaPost> page = postMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        List<Map<String, Object>> list = page.getRecords().stream().map(this::toMapWithName).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal(), pageNum, pageSize);
    }

    public Map<String, Object> publish(QaDTO dto, Long studentId) {
        QaPost p = new QaPost();
        p.setStudentId(studentId);
        p.setTitle(dto.getTitle());
        p.setContent(dto.getContent());
        p.setCategoryId(dto.getCategoryId());
        p.setTags(dto.getTags());
        p.setViewCount(0);
        p.setReplyCount(0);
        p.setLikeCount(0);
        p.setStatus(0);
        p.setCreateTime(LocalDateTime.now());
        postMapper.insert(p);
        Map<String, Object> m = new HashMap<>();
        m.put("postId", p.getId());
        return m;
    }

    public Map<String, Object> detail(Long id) {
        QaPost p = postMapper.selectById(id);
        if (p == null) throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        // 浏览量 +1
        p.setViewCount((p.getViewCount() == null ? 0 : p.getViewCount()) + 1);
        postMapper.updateById(p);

        Map<String, Object> m = toMapWithName(p);
        List<QaReply> replies = replyMapper.selectList(new LambdaQueryWrapper<QaReply>()
                .eq(QaReply::getPostId, id).orderByAsc(QaReply::getCreateTime));
        List<Map<String, Object>> replyList = replies.stream().map(rp -> {
            Map<String, Object> rm = JsonUtil.toMap(rp);
            StudentUser stu = studentUserMapper.selectById(rp.getStudentId());
            rm.put("studentName", stu != null ? (StringUtils.hasText(stu.getNickname()) ? stu.getNickname() : stu.getUsername()) : "匿名");
            return rm;
        }).toList();
        m.put("replies", replyList);
        return m;
    }

    public Map<String, Object> reply(Long id, QaReplyDTO dto, Long studentId) {
        QaPost p = postMapper.selectById(id);
        if (p == null) throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        QaReply rp = new QaReply();
        rp.setPostId(id);
        rp.setStudentId(studentId);
        rp.setContent(dto.getContent());
        rp.setParentId(dto.getParentId());
        rp.setLikeCount(0);
        rp.setIsBest(0);
        rp.setCreateTime(LocalDateTime.now());
        replyMapper.insert(rp);
        p.setReplyCount((p.getReplyCount() == null ? 0 : p.getReplyCount()) + 1);
        postMapper.updateById(p);
        Map<String, Object> m = new HashMap<>();
        m.put("replyId", rp.getId());
        return m;
    }

    /** 设为最佳答案（仅楼主可操作） */
    public void setBest(Long replyId, Long studentId) {
        QaReply rp = replyMapper.selectById(replyId);
        if (rp == null) throw new BusinessException(ResultCode.NOT_FOUND, "回复不存在");
        QaPost p = postMapper.selectById(rp.getPostId());
        if (p == null || !p.getStudentId().equals(studentId)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "仅楼主可设为最佳答案");
        }
        // 先清除该帖其它最佳
        List<QaReply> others = replyMapper.selectList(new LambdaQueryWrapper<QaReply>()
                .eq(QaReply::getPostId, rp.getPostId()).eq(QaReply::getIsBest, 1));
        others.forEach(o -> {
            o.setIsBest(0);
            replyMapper.updateById(o);
        });
        rp.setIsBest(1);
        replyMapper.updateById(rp);
    }

    private Map<String, Object> toMapWithName(QaPost p) {
        Map<String, Object> m = JsonUtil.toMap(p);
        StudentUser stu = studentUserMapper.selectById(p.getStudentId());
        m.put("studentName", stu != null ? (StringUtils.hasText(stu.getNickname()) ? stu.getNickname() : stu.getUsername()) : "匿名");
        return m;
    }
}
