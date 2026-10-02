package com.campus.recommend.service;

import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.ResultCode;
import com.campus.recommend.config.Neo4jTemplate;
import com.campus.recommend.dto.GraphNodeCreateDTO;
import com.campus.recommend.dto.GraphRelationDTO;
import com.campus.recommend.dto.GraphSyncDTO;
import com.campus.recommend.vo.GraphEdge;
import com.campus.recommend.vo.GraphNode;
import com.campus.recommend.vo.GraphSubgraph;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.LinkedHashMap;

/**
 * 知识图谱服务：可视化查询（节点/关系/子图/详情） + 管理端运维（增删节点/关系/全量同步）
 * 节点 id 规则：College/Major/Course/KnowledgePoint 用属性 id(Long)；Resource 用 resource_id(String)。
 */
@Slf4j
@Service
public class GraphService {

    @Autowired
    private Neo4jTemplate neo4jTemplate;
    @Autowired
    private Neo4jSyncService neo4jSyncService;

    /** 节点列表 / 搜索 */
    public List<GraphNode> listNodes(String type, String keyword, Integer limit) {
        String label = safeLabel(type == null ? "Resource" : type);
        StringBuilder cypher = new StringBuilder("MATCH (n:").append(label).append(") ");
        Map<String, Object> params = new HashMap<>();
        if (keyword != null && !keyword.isBlank()) {
            cypher.append("WHERE n.name CONTAINS $kw OR n.title CONTAINS $kw ");
            params.put("kw", keyword);
        }
        cypher.append("RETURN n ");
        if (limit != null && limit > 0) cypher.append("LIMIT ").append(limit);
        else cypher.append("LIMIT 200");
        List<GraphNode> nodes = new ArrayList<>();
        for (Record rec : neo4jTemplate.runReadRaw(cypher.toString(), params)) {
            nodes.add(toGraphNode(rec.get("n").asNode()));
        }
        return nodes;
    }

    /** 关系列表 */
    public List<GraphEdge> listEdges(String fromType, String fromId, String relType) {
        String label = safeLabel(fromType == null ? "Resource" : fromType);
        StringBuilder cypher = new StringBuilder("MATCH (a:").append(label).append(")");
        if (relType != null && !relType.isBlank()) {
            cypher.append("-[r:").append(safeRel(relType)).append("]->");
        } else {
            cypher.append("-[r]->");
        }
        cypher.append("(b) ");
        Map<String, Object> params = new HashMap<>();
        if (fromId != null && !fromId.isBlank()) {
            cypher.append("WHERE a").append(idPredicate(label)).append(" = $fid ");
            params.put("fid", rawId(label, fromId));
        }
        cypher.append("RETURN a, r, b LIMIT 300");
        List<GraphEdge> edges = new ArrayList<>();
        for (Record rec : neo4jTemplate.runReadRaw(cypher.toString(), params)) {
            Node a = rec.get("a").asNode();
            Node b = rec.get("b").asNode();
            Relationship r = rec.get("r").asRelationship();
            GraphEdge e = new GraphEdge();
            e.setSource(nodeLogicalId(a));
            e.setTarget(nodeLogicalId(b));
            e.setRelType(r.type());
            e.setLabel(relLabel(r.type()));
            edges.add(e);
        }
        return edges;
    }

    /** 多跳子图（力导向图渲染） */
    public GraphSubgraph subgraph(String resourceId, String kpId, int depth) {
        depth = depth < 1 ? 1 : Math.min(depth, 4);
        String sid = resourceId != null ? resourceId : kpId;
        if (sid == null) throw new BusinessException(ResultCode.PARAM_ERROR, "resourceId 或 kpId 必填其一");
        String startMatch = resourceId != null
                ? "MATCH (start:Resource {resource_id:$sid}) "
                : "MATCH (start:KnowledgePoint {id:$sid}) ";
        String cypher = startMatch + "MATCH p=(start)-[*1.." + depth + "]-(n) RETURN nodes(p) AS ns, relationships(p) AS rs";
        Map<String, Object> params = new HashMap<>();
        params.put("sid", rawId(resourceId != null ? "Resource" : "KnowledgePoint", sid));

        Map<Long, GraphNode> nodeMap = new HashMap<>();
        List<GraphEdge> edgeList = new ArrayList<>();
        for (Record rec : neo4jTemplate.runReadRaw(cypher, params)) {
            List<Node> ns = rec.get("ns").asList(Value::asNode);
            List<Relationship> rs = rec.get("rs").asList(Value::asRelationship);
            Map<Long, String> elemToLogical = new HashMap<>();
            for (Node n : ns) {
                elemToLogical.put(n.id(), nodeLogicalId(n));
                nodeMap.put(n.id(), toGraphNode(n));
            }
            for (Relationship r : rs) {
                GraphEdge e = new GraphEdge();
                e.setSource(elemToLogical.getOrDefault(r.startNodeId(), String.valueOf(r.startNodeId())));
                e.setTarget(elemToLogical.getOrDefault(r.endNodeId(), String.valueOf(r.endNodeId())));
                e.setRelType(r.type());
                e.setLabel(relLabel(r.type()));
                edgeList.add(e);
            }
        }
        GraphSubgraph sg = new GraphSubgraph();
        sg.setNodes(new ArrayList<>(nodeMap.values()));
        sg.setEdges(edgeList);
        return sg;
    }

    /** 节点详情 + 邻居 */
    public Map<String, Object> nodeDetail(String type, String id) {
        String label = safeLabel(type);
        Object lid = rawId(label, id);
        String cypher = "MATCH (n:" + label + ") WHERE n" + idPredicate(label) + " = $id "
                + "OPTIONAL MATCH (n)-[r]-(m) RETURN n, collect({rel:type(r), dir:CASE WHEN startNode(r)=n THEN 'OUT' ELSE 'IN' END, node: m}) AS neighbors";
        Map<String, Object> params = new HashMap<>();
        params.put("id", lid);
        List<Record> recs = neo4jTemplate.runReadRaw(cypher, params);
        if (recs.isEmpty()) throw new BusinessException(ResultCode.NOT_FOUND, "节点不存在");
        Record rec = recs.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("node", toGraphNode(rec.get("n").asNode()));
        List<Map<String, Object>> neighbors = new ArrayList<>();
        for (Map<String, Object> nb : rec.get("neighbors").asList(Value::asMap)) {
            neighbors.add(nb);
        }
        result.put("neighbors", neighbors);
        return result;
    }

    /* ===================== 管理端运维 ===================== */

    /** 新增节点 */
    public void createNode(GraphNodeCreateDTO dto) {
        String label = safeLabel(dto.getType());
        Map<String, Object> props = dto.getProperties() == null ? new HashMap<>() : dto.getProperties();
        neo4jTemplate.runWrite("CREATE (n:" + label + ") SET n = $props", Map.of("props", props));
    }

    /** 删除节点（DETACH DELETE） */
    public void deleteNode(String type, String id) {
        String label = safeLabel(type);
        Object lid = rawId(label, id);
        neo4jTemplate.runWrite("MATCH (n:" + label + ") WHERE n" + idPredicate(label) + " = $id DETACH DELETE n",
                Map.of("id", lid));
    }

    /** 新增关系 */
    public void createRelation(GraphRelationDTO dto) {
        String fromLabel = safeLabel(dto.getFromType());
        String toLabel = safeLabel(dto.getToType());
        Object fid = rawId(fromLabel, dto.getFromId());
        Object tid = rawId(toLabel, dto.getToId());
        String rel = safeRel(dto.getRelType());
        String cypher = "MATCH (a:" + fromLabel + "), (b:" + toLabel + ") "
                + "WHERE a" + idPredicate(fromLabel) + " = $fid AND b" + idPredicate(toLabel) + " = $tid "
                + "MERGE (a)-[:" + rel + "]->(b)";
        neo4jTemplate.runWrite(cypher, Map.of("fid", fid, "tid", tid));
    }

    /** 删除关系 */
    public void deleteRelation(GraphRelationDTO dto) {
        String fromLabel = safeLabel(dto.getFromType());
        String toLabel = safeLabel(dto.getToType());
        Object fid = rawId(fromLabel, dto.getFromId());
        Object tid = rawId(toLabel, dto.getToId());
        String rel = safeRel(dto.getRelType());
        String cypher = "MATCH (a:" + fromLabel + ")-[r:" + rel + "]->(b:" + toLabel + ") "
                + "WHERE a" + idPredicate(fromLabel) + " = $fid AND b" + idPredicate(toLabel) + " = $tid DELETE r";
        neo4jTemplate.runWrite(cypher, Map.of("fid", fid, "tid", tid));
    }

    /** 全量同步 MySQL → Neo4j */
    public int fullSync(GraphSyncDTO dto) {
        return neo4jSyncService.fullSync(dto == null || dto.getScope() == null ? "all" : dto.getScope());
    }

    /* ===================== 推荐辅助查询 ===================== */

    /**
     * 知识图谱语义候选（多跳，按 hops 取最近关联资源）—— 供个性化推荐 KG 路使用。
     * 返回 {resourceNo, title, heat, hops}
     */
    public List<Map<String, Object>> queryRelatedByResourceNo(String resourceNo, int limit) {
        try {
            String cypher = "MATCH path = (r:Resource {resource_id:$rid})-[:BELONGS_TO_KP]->(:KnowledgePoint)"
                    + "-[*1..3]-(:KnowledgePoint)<-[:BELONGS_TO_KP]-(rec:Resource) "
                    + "WHERE rec.resource_id <> $rid AND rec.status = 1 "
                    + "WITH rec, min(length(path)) AS hops "
                    + "RETURN rec.resource_id AS resourceNo, rec.title AS title, rec.heat AS heat, hops AS hops "
                    + "ORDER BY hops ASC, rec.heat DESC LIMIT $topN";
            Map<String, Object> p = new HashMap<>();
            p.put("rid", resourceNo);
            p.put("topN", limit);
            List<Map<String, Object>> out = new ArrayList<>();
            for (Record rec : neo4jTemplate.runReadRaw(cypher, p)) {
                Map<String, Object> m = new HashMap<>();
                m.put("resourceNo", rec.get("resourceNo").asString());
                m.put("title", rec.get("title").asString());
                m.put("heat", rec.get("heat").isNull() ? 0.0 : rec.get("heat").asDouble());
                m.put("hops", rec.get("hops").asLong());
                out.add(m);
            }
            return out;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 单资源语义相关（带关系类型）—— 供 /api/recommend/related 使用。
     * 返回 {resourceNo, title, heat, relation}，relation ∈ {SAME_COURSE, RELATED, PREREQUISITE}
     */
    public List<Map<String, Object>> queryRelatedWithRelation(String resourceNo, int limit) {
        try {
            String cypher = "MATCH (r:Resource {resource_id:$rid})-[:BELONGS_TO_KP]->(kp:KnowledgePoint) "
                    + "OPTIONAL MATCH (kp)<-[:TEACHES]-(c:Course)-[:TEACHES]->(kp2:KnowledgePoint) WHERE kp2 <> kp "
                    + "OPTIONAL MATCH (r2:Resource)-[:BELONGS_TO_KP]->(kp2) WHERE r2.resource_id <> $rid AND r2.status = 1 "
                    + "WITH r, collect(DISTINCT {resId:r2.resource_id, title:r2.title, heat:r2.heat}) AS sameCourse "
                    + "OPTIONAL MATCH (r)-[:BELONGS_TO_KP]->(:KnowledgePoint)-[:RELATED_TO]-(kpR:KnowledgePoint) "
                    + "OPTIONAL MATCH (rRel:Resource)-[:BELONGS_TO_KP]->(kpR) WHERE rRel.resource_id <> $rid AND rRel.status = 1 "
                    + "WITH r, sameCourse, collect(DISTINCT {resId:rRel.resource_id, title:rRel.title, heat:rRel.heat}) AS related "
                    + "OPTIONAL MATCH (r)-[:BELONGS_TO_KP]->(:KnowledgePoint)-[:PREREQUISITE]-(kpP:KnowledgePoint) "
                    + "OPTIONAL MATCH (rPre:Resource)-[:BELONGS_TO_KP]->(kpP) WHERE rPre.resource_id <> $rid AND rPre.status = 1 "
                    + "WITH r, sameCourse, related, collect(DISTINCT {resId:rPre.resource_id, title:rPre.title, heat:rPre.heat}) AS pre "
                    + "RETURN sameCourse, related, pre";
            Map<String, Object> p = new HashMap<>();
            p.put("rid", resourceNo);
            List<Record> recs = neo4jTemplate.runReadRaw(cypher, p);
            if (recs.isEmpty()) return Collections.emptyList();
            Record rec = recs.get(0);
            Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
            mergeRelation(merged, rec.get("sameCourse"), "SAME_COURSE");
            mergeRelation(merged, rec.get("related"), "RELATED");
            mergeRelation(merged, rec.get("pre"), "PREREQUISITE");
            List<Map<String, Object>> out = new ArrayList<>(merged.values());
            return out.size() > limit ? out.subList(0, limit) : out;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void mergeRelation(Map<String, Map<String, Object>> merged, Value listVal, String relation) {
        if (listVal == null || listVal.isNull()) return;
        for (Map<String, Object> m : listVal.asList(Value::asMap)) {
            String resId = String.valueOf(m.get("resId"));
            if (!merged.containsKey(resId)) {
                Map<String, Object> item = new HashMap<>();
                item.put("resourceNo", resId);
                item.put("title", m.get("title"));
                item.put("heat", m.get("heat") == null ? 0.0 : m.get("heat"));
                item.put("relation", relation);
                merged.put(resId, item);
            }
        }
    }

    private GraphNode toGraphNode(Node node) {
        String label = node.labels().iterator().next();
        String id = nodeLogicalId(node);
        String nameLabel;
        if (node.containsKey("title")) nameLabel = node.get("title").asString();
        else if (node.containsKey("name")) nameLabel = node.get("name").asString();
        else nameLabel = id;
        Double heat = node.containsKey("heat") ? node.get("heat").asDouble() : null;
        GraphNode gn = new GraphNode();
        gn.setId(id);
        gn.setType(label);
        gn.setLabel(nameLabel);
        gn.setHeat(heat);
        gn.setProperties(node.asMap(v -> v.asObject()));
        return gn;
    }

    /** 节点逻辑 id（与前端渲染约定的类型一致） */
    private String nodeLogicalId(Node node) {
        if (node.containsKey("resource_id")) return node.get("resource_id").asString();
        if (node.containsKey("id")) return String.valueOf(node.get("id").asLong());
        return String.valueOf(node.id());
    }

    private String idPredicate(String label) {
        return "resource_id".equals(label) ? ".resource_id" : ".id";
    }

    /** 解析节点 id 为原始类型：Resource 用字符串 resource_id，其余用 Long */
    private Object rawId(String label, String id) {
        if ("Resource".equals(label)) return id;
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            return id;
        }
    }

    private String safeLabel(String type) {
        if (type == null) return "Resource";
        return switch (type.toUpperCase()) {
            case "COLLEGE" -> "College";
            case "MAJOR" -> "Major";
            case "COURSE" -> "Course";
            case "KNOWLEDGE_POINT", "KNOWLEDGEPOINT", "KP" -> "KnowledgePoint";
            case "RESOURCE" -> "Resource";
            default -> type;
        };
    }

    private String safeRel(String rel) {
        if (rel == null) return "RELATED_TO";
        return rel.toUpperCase();
    }

    private String relLabel(String rel) {
        return switch (rel) {
            case "BELONGS_TO" -> "属于";
            case "TEACHES" -> "教授";
            case "CONTAINS" -> "包含";
            case "PREREQUISITE" -> "先修";
            case "RELATED_TO" -> "相关";
            case "BELONGS_TO_KP" -> "归属知识点";
            default -> rel;
        };
    }
}
