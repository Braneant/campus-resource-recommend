package com.campus.recommend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.ResultCode;
import com.campus.recommend.config.Neo4jTemplate;
import com.campus.recommend.entity.*;
import com.campus.recommend.mapper.*;
import com.campus.recommend.vo.DashboardStatsVO;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据大屏统计（ECharts 聚合数据）
 */
@Slf4j
@Service
public class DashboardService {

    @Autowired
    private StudentUserMapper studentUserMapper;
    @Autowired
    private LearningResourceMapper resourceMapper;
    @Autowired
    private ResourceRequirementMapper requirementMapper;
    @Autowired
    private QaPostMapper qaPostMapper;
    @Autowired
    private Neo4jTemplate neo4jTemplate;

    /** 总览卡片 */
    public DashboardStatsVO stats() {
        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setUserCount(studentUserMapper.selectCount(null));
        vo.setResourceCount(resourceMapper.selectCount(null));
        vo.setResourcePassed(resourceMapper.selectCount(
                new LambdaQueryWrapper<LearningResource>().eq(LearningResource::getStatus, 1)));
        vo.setRequirementCount(requirementMapper.selectCount(null));
        vo.setQaCount(qaPostMapper.selectCount(null));
        try {
            vo.setKpCount(neo4jTemplate.countNodes("KnowledgePoint"));
        } catch (Exception e) {
            vo.setKpCount(0L);
        }
        return vo;
    }

    /** 资源增长趋势（折线） */
    public Map<String, Object> resourceTrend() {
        List<Map<String, Object>> rows = resourceMapper.resourceTrend();
        List<String> dates = rows.stream().map(r -> String.valueOf(r.get("dates"))).collect(Collectors.toList());
        List<Long> counts = rows.stream().map(r -> ((Number) r.get("counts")).longValue()).collect(Collectors.toList());
        Map<String, Object> m = new HashMap<>();
        m.put("dates", dates);
        m.put("counts", counts);
        return m;
    }

    /** 资源分类占比（饼图） */
    public Map<String, Object> categoryDist() {
        List<Map<String, Object>> rows = resourceMapper.categoryDist();
        List<Map<String, Object>> items = rows.stream().map(r -> {
            Map<String, Object> it = new HashMap<>();
            it.put("name", r.get("name"));
            it.put("category", r.get("category"));
            it.put("value", ((Number) r.get("value")).longValue());
            return it;
        }).collect(Collectors.toList());
        Map<String, Object> m = new HashMap<>();
        m.put("items", items);
        return m;
    }

    /** 知识点热度 Top（柱状，来自 Neo4j） */
    public Map<String, Object> kpHeat() {
        List<Map<String, Object>> items = new ArrayList<>();
        try {
            String cypher = "MATCH (k:KnowledgePoint) RETURN k.id AS kpId, k.name AS name, k.heat AS heat "
                    + "ORDER BY k.heat DESC LIMIT 20";
            for (Record rec : neo4jTemplate.runReadRaw(cypher, Collections.emptyMap())) {
                Map<String, Object> it = new HashMap<>();
                it.put("kpId", rec.get("kpId").asLong());
                it.put("name", rec.get("name").asString());
                it.put("heat", rec.get("heat").isNull() ? 0.0 : rec.get("heat").asDouble());
                items.add(it);
            }
        } catch (Exception e) {
            log.warn("知识点热度查询失败：{}", e.getMessage());
        }
        Map<String, Object> m = new HashMap<>();
        m.put("items", items);
        return m;
    }

    /** 学院资源量排行（柱状） */
    public Map<String, Object> collegeRank() {
        List<Map<String, Object>> rows = resourceMapper.collegeRank();
        List<Map<String, Object>> items = rows.stream().map(r -> {
            Map<String, Object> it = new HashMap<>();
            it.put("collegeId", r.get("collegeId"));
            it.put("name", r.get("name"));
            it.put("value", ((Number) r.get("value")).longValue());
            return it;
        }).collect(Collectors.toList());
        Map<String, Object> m = new HashMap<>();
        m.put("items", items);
        return m;
    }
}
