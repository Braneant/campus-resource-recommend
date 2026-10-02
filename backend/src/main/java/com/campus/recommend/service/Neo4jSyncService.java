package com.campus.recommend.service;

import com.campus.recommend.common.BusinessException;
import com.campus.recommend.config.Neo4jTemplate;
import com.campus.recommend.entity.*;
import com.campus.recommend.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    /** 同步单个资源到 Neo4j（幂等 MERGE） */
    public void syncResource(LearningResource res, Integer syncType) {
        Neo4jSyncLog log = new Neo4jSyncLog();
        log.setResourceId(res.getId());
        log.setSyncType(syncType == null ? 2 : syncType);
        log.setSyncStatus(0);
        log.setCreateTime(LocalDateTime.now());
        syncLogMapper.insert(log);
        try {
            Map<String, Object> p = new HashMap<>();
            p.put("resourceId", res.getResourceNo());
            p.put("title", res.getTitle());
            p.put("type", res.getType() == null ? 1 : res.getType());
            p.put("author", res.getAuthor());
            p.put("viewCount", res.getViewCount() == null ? 0 : res.getViewCount());
            p.put("downloadCount", res.getDownloadCount() == null ? 0 : res.getDownloadCount());
            p.put("avgRating", res.getAvgRating() == null ? 0.0 : res.getAvgRating().doubleValue());
            p.put("heat", res.getHeat() == null ? 0.0 : res.getHeat().doubleValue());

            // 1) MERGE 资源节点
            neo4jTemplate.runWrite(
                    "MERGE (r:Resource {resource_id:$resourceId}) " +
                            "SET r.title=$title, r.type=$type, r.author=$author, " +
                            "r.view_count=$viewCount, r.download_count=$downloadCount, " +
                            "r.avg_rating=$avgRating, r.heat=$heat, r.status=1", p);

            // 2) 绑定主知识点（先删旧关系再建新）
            if (res.getPrimaryKpId() != null) {
                Map<String, Object> kp = new HashMap<>();
                kp.put("resourceId", res.getResourceNo());
                kp.put("kpId", res.getPrimaryKpId());
                neo4jTemplate.runWrite(
                        "MATCH (r:Resource {resource_id:$resourceId}) " +
                                "OPTIONAL MATCH (r)-[old:BELONGS_TO_KP]->() DELETE old " +
                                "WITH r MATCH (k:KnowledgePoint {id:$kpId}) " +
                                "MERGE (r)-[:BELONGS_TO_KP]->(k)", kp);
            }
            log.setSyncStatus(1);
        } catch (BusinessException e) {
            log.setSyncStatus(2);
            log.setErrorMsg(e.getMessage());
            log.setCreateTime(LocalDateTime.now());
        }
        syncLogMapper.updateById(log);
    }

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
