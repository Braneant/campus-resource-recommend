package com.campus.recommend.service;

import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.ResultCode;
import com.campus.recommend.config.Neo4jTemplate;
import com.campus.recommend.entity.LearningResource;
import com.campus.recommend.entity.Neo4jSyncLog;
import com.campus.recommend.mapper.LearningResourceMapper;
import com.campus.recommend.mapper.Neo4jSyncLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Each log is a durable outbox item. Row lock serializes retries across JVMs. */
@Service
public class GraphSyncWorker {
    @Autowired private Neo4jSyncLogMapper logs;
    @Autowired private LearningResourceMapper resources;
    @Autowired private Neo4jTemplate graph;
    @Autowired private ApplicationEventPublisher events;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retry(Long id) {
        Neo4jSyncLog entry = logs.lockById(id);
        if (entry == null) throw new BusinessException(ResultCode.NOT_FOUND, "同步记录不存在");
        if (Integer.valueOf(1).equals(entry.getSyncStatus())) return;
        // Different outbox records for the same resource must serialize with each other AND
        // with the audit status update, otherwise a slow approval could resurrect a rejection.
        LearningResource resource = resources.lockById(entry.getResourceId());
        try {
            if (resource == null) throw new IllegalStateException("Resource missing");
            // Always apply current MySQL state, so retrying an old approval cannot resurrect a rejected resource.
            if (!Integer.valueOf(1).equals(resource.getStatus())) {
                graph.runWrite("MATCH (r:Resource {resource_id:$rid}) DETACH DELETE r",
                        Map.of("rid", resource.getResourceNo()));
            } else {
                writeResource(resource);
            }
            entry.setSyncStatus(1);
            entry.setErrorMsg(null);
        } catch (RuntimeException failure) {
            entry.setSyncStatus(2);
            entry.setErrorMsg("图谱同步失败，请检查 Neo4j 连接与知识点是否存在后重试");
        }
        // Explicit SQL clears a previous error_msg (MyBatis default ignores null on entity updates).
        logs.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Neo4jSyncLog>()
                .eq("id", id).set("sync_status", entry.getSyncStatus()).set("error_msg", entry.getErrorMsg()));
        if (Integer.valueOf(1).equals(entry.getSyncStatus()) && events != null) {
            events.publishEvent(new ResourceDataChanged());
        }
    }

    private void writeResource(LearningResource r) {
        if (r.getPrimaryKpId() != null) {
            List<Map<String, Object>> found = graph.runRead(
                    "MATCH (k:KnowledgePoint {id:$id}) RETURN count(k) AS count", Map.of("id", r.getPrimaryKpId()));
            if (found.isEmpty() || ((Number) found.get(0).get("count")).longValue() == 0) {
                throw new IllegalStateException("Knowledge point missing");
            }
        }
        Map<String, Object> p = new HashMap<>();
        p.put("rid", r.getResourceNo()); p.put("title", r.getTitle()); p.put("author", r.getAuthor());
        p.put("type", r.getType() == null ? 1 : r.getType()); p.put("kpId", r.getPrimaryKpId());
        p.put("views", r.getViewCount() == null ? 0 : r.getViewCount());
        p.put("downloads", r.getDownloadCount() == null ? 0 : r.getDownloadCount());
        p.put("rating", r.getAvgRating() == null ? 0.0 : r.getAvgRating().doubleValue());
        p.put("heat", r.getHeat() == null ? 0.0 : r.getHeat().doubleValue());
        // One Cypher transaction: resource update and replacement of its knowledge-point relation are atomic.
        graph.runWrite("MERGE (r:Resource {resource_id:$rid}) "
                + "SET r.title=$title,r.author=$author,r.type=$type,r.view_count=$views,"
                + "r.download_count=$downloads,r.avg_rating=$rating,r.heat=$heat,r.status=1 "
                + "WITH r OPTIONAL MATCH (r)-[old:BELONGS_TO_KP]->() DELETE old "
                + "WITH DISTINCT r OPTIONAL MATCH (k:KnowledgePoint {id:$kpId}) "
                + "FOREACH (point IN CASE WHEN k IS NULL THEN [] ELSE [k] END | MERGE (r)-[:BELONGS_TO_KP]->(point))", p);
    }
}
