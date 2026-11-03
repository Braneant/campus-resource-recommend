package com.campus.recommend.service;

import com.campus.recommend.common.BusinessException;
import com.campus.recommend.config.Neo4jTemplate;
import com.campus.recommend.entity.*;
import com.campus.recommend.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图谱同步服务（MySQL → Neo4j，对应 02 文档 §六）
 * 审核通过时调用 syncResource 将资源节点 MERGE 进 Neo4j 并绑定知识点；
 * 支持全量同步与学科节点同步。
 */
@Slf4j
@Service
public class Neo4jSyncService {

    @Autowired
    private Neo4jTemplate neo4jTemplate;
    @Autowired
    private Neo4jSyncLogMapper syncLogMapper;
    @Autowired
    private LearningResourceMapper resourceMapper;
    @Autowired
    private CollegeMapper collegeMapper;
    @Autowired
    private MajorMapper majorMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private GraphSyncWorker worker;

    /** Enqueue in the caller's MySQL transaction; contact Neo4j only after commit. */
    @Transactional
    public void syncResource(LearningResource res, Integer syncType) {
        Neo4jSyncLog log = new Neo4jSyncLog();
        log.setResourceId(res.getId());
        log.setSyncType(syncType == null ? 2 : syncType);
        log.setSyncStatus(0);
        log.setCreateTime(LocalDateTime.now());
        syncLogMapper.insert(log);
        Long logId = log.getId();
        Runnable dispatch = () -> {
            try { worker.retry(logId); }
            catch (RuntimeException failure) {
                // The committed pending row survives process/connection failures.
                Neo4jSyncService.log.warn("Graph sync pending; logId={}", logId);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { dispatch.run(); }
            });
        } else {
            dispatch.run();
        }
    }

    /** Recover committed pending tasks after restart; failed tasks can be retried by an administrator. */
    @Scheduled(fixedDelayString = "${neo4j.sync.retry-delay-ms:60000}")
    public void drainPending() {
        for (Neo4jSyncLog entry : syncLogMapper.selectList(new LambdaQueryWrapper<Neo4jSyncLog>()
                .eq(Neo4jSyncLog::getSyncStatus, 0).orderByAsc(Neo4jSyncLog::getId).last("LIMIT 20"))) {
            try { worker.retry(entry.getId()); }
            catch (RuntimeException failure) { log.warn("Graph sync pending; logId={}", entry.getId()); }
        }
    }

    public List<Neo4jSyncLog> recentLogs(Integer status) {
        LambdaQueryWrapper<Neo4jSyncLog> query = new LambdaQueryWrapper<>();
        if (status != null) query.eq(Neo4jSyncLog::getSyncStatus, status);
        return syncLogMapper.selectList(query.orderByDesc(Neo4jSyncLog::getId).last("LIMIT 100"));
    }

    public void retry(Long id) { worker.retry(id); }

    /** 从图谱移除资源（驳回/下架） */
    public void removeResourceFromGraph(String resourceNo) {
        Map<String, Object> p = new HashMap<>();
        p.put("resourceId", resourceNo);
        neo4jTemplate.runWrite("MATCH (r:Resource {resource_id:$resourceId}) DETACH DELETE r", p);
    }

    /** 全量同步：将 MySQL 已通过资源（及学科节点）重新 MERGE 进 Neo4j */
    public int fullSync(String scope) {
        int count = 0;
        if (!"resource".equals(scope)) {
            syncSubjectNodes();
        }
        if (!"college".equals(scope)) {
            List<LearningResource> list = resourceMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LearningResource>()
                            .eq(LearningResource::getStatus, 1));
            for (LearningResource res : list) {
                syncResource(res, 2);
                count++;
            }
        }
        return count;
    }

    /** 同步学院 / 专业 / 课程节点及其层级关系 */
    public void syncSubjectNodes() {
        // 学院
        for (College c : collegeMapper.selectList(null)) {
            Map<String, Object> p = new HashMap<>();
            p.put("id", c.getId());
            p.put("name", c.getName());
            p.put("code", c.getCode());
            p.put("description", c.getDescription());
            neo4jTemplate.runWrite("MERGE (n:College {id:$id}) SET n.name=$name, n.code=$code, n.description=$description", p);
        }
        // 专业
        for (Major m : majorMapper.selectList(null)) {
            Map<String, Object> p = new HashMap<>();
            p.put("id", m.getId());
            p.put("collegeId", m.getCollegeId());
            p.put("name", m.getName());
            p.put("code", m.getCode());
            p.put("description", m.getDescription());
            neo4jTemplate.runWrite("MERGE (n:Major {id:$id}) SET n.college_id=$collegeId, n.name=$name, n.code=$code, n.description=$description", p);
            Map<String, Object> rel = new HashMap<>();
            rel.put("mid", m.getId());
            rel.put("cid", m.getCollegeId());
            neo4jTemplate.runWrite("MATCH (m:Major {id:$mid}), (c:College {id:$cid}) MERGE (m)-[:BELONGS_TO]->(c)", rel);
        }
        // 课程
        for (Course co : courseMapper.selectList(null)) {
            Map<String, Object> p = new HashMap<>();
            p.put("id", co.getId());
            p.put("collegeId", co.getCollegeId());
            p.put("majorId", co.getMajorId());
            p.put("name", co.getName());
            p.put("code", co.getCode());
            p.put("teacher", co.getTeacher());
            p.put("credit", co.getCredit() == null ? 0.0 : co.getCredit().doubleValue());
            p.put("semester", co.getSemester());
            neo4jTemplate.runWrite("MERGE (n:Course {id:$id}) SET n.college_id=$collegeId, n.major_id=$majorId, n.name=$name, n.code=$code, n.teacher=$teacher, n.credit=$credit, n.semester=$semester", p);
            if (co.getMajorId() != null) {
                Map<String, Object> rel = new HashMap<>();
                rel.put("coid", co.getId());
                rel.put("mid", co.getMajorId());
                neo4jTemplate.runWrite("MATCH (co:Course {id:$coid}), (m:Major {id:$mid}) MERGE (co)-[:BELONGS_TO]->(m)", rel);
            }
        }
    }
}
