package com.campus.recommend.config;

import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.exceptions.Neo4jException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Neo4j 操作模板：封装读/写，统一异常与结果转换。
 * - runRead：执行只读 Cypher，返回 List<Map>（基础类型，便于序列化给前端）
 * - runWrite：执行写 Cypher（MERGE/DELETE 等）
 * 所有图查询失败时抛出 BusinessException，由调用方决定是否降级（如推荐算法 KG 路失败可跳过）。
 */
@Slf4j
@Component
public class Neo4jTemplate {

    @Autowired
    private Driver driver;
    @Value("${neo4j.database:neo4j}")
    private String database;

    private SessionConfig sessionConfig() {
        return SessionConfig.forDatabase(database);
    }

    /** 只读查询 */
    public List<Map<String, Object>> runRead(String cypher, Map<String, Object> params) {
        try (Session session = driver.session(sessionConfig())) {
            org.neo4j.driver.Result result = session.run(cypher, params == null ? Map.of() : params);
            List<Map<String, Object>> list = new ArrayList<>();
            while (result.hasNext()) {
                list.add(recordToMap(result.next()));
            }
            return list;
        } catch (Neo4jException e) {
            log.warn("Neo4j 查询失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "Neo4j 查询失败：" + e.getMessage());
        }
    }

    /** 写操作（单条语句） */
    public void runWrite(String cypher, Map<String, Object> params) {
        try (Session session = driver.session(sessionConfig())) {
            session.executeWrite(tx -> {
                tx.run(cypher, params == null ? Map.of() : params);
                return null;
            });
        } catch (Neo4jException e) {
            log.warn("Neo4j 写入失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "Neo4j 写入失败：" + e.getMessage());
        }
    }

    /** 原始查询：返回 Record 列表（供图谱子图解析 Node/Relationship 对象） */
    public List<org.neo4j.driver.Record> runReadRaw(String cypher, Map<String, Object> params) {
        try (Session session = driver.session(sessionConfig())) {
            org.neo4j.driver.Result result = session.run(cypher, params == null ? Map.of() : params);
            List<org.neo4j.driver.Record> list = new ArrayList<>();
            while (result.hasNext()) {
                list.add(result.next());
            }
            return list;
        } catch (Neo4jException e) {
            log.warn("Neo4j 查询失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "Neo4j 查询失败：" + e.getMessage());
        }
    }

    /** 统计节点数量（用于大屏 kpCount 等） */
    public long countNodes(String label) {
        String cypher = "MATCH (n:" + label + ") RETURN count(n) AS c";
        List<Map<String, Object>> list = runRead(cypher, Map.of());
        if (list.isEmpty()) {
            return 0;
        }
        Number n = (Number) list.get(0).get("c");
        return n == null ? 0 : n.longValue();
    }

    /** 将一条 Record 转为普通 Map（值已转为 Java 基础类型） */
    private Map<String, Object> recordToMap(Record record) {
        Map<String, Object> map = new HashMap<>();
        for (String key : record.keys()) {
            org.neo4j.driver.Value v = record.get(key);
            map.put(key, v.isNull() ? null : v.asObject());
        }
        return map;
    }
}
