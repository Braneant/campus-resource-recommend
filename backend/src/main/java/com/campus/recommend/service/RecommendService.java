package com.campus.recommend.service;

import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.ResultCode;
import com.campus.recommend.entity.*;
import com.campus.recommend.mapper.*;
import com.campus.recommend.recommendation.BoundedTtlCache;
import com.campus.recommend.recommendation.RecommendationEngine;
import com.campus.recommend.recommendation.RecommendationEngine.*;
import com.campus.recommend.vo.GraphNode;
import com.campus.recommend.vo.RecommendExplainVO;
import com.campus.recommend.vo.RecommendItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/** MySQL/Neo4j adapters around the same pure Java algorithm used by offline evaluation. */
@Slf4j
@Service
public class RecommendService {
    @Autowired private FavoriteBehaviorMapper favoriteMapper;
    @Autowired private DownloadBehaviorMapper downloadMapper;
    @Autowired private RatingBehaviorMapper ratingMapper;
    @Autowired private LearningResourceMapper resourceMapper;
    @Autowired private GraphService graphService;

    @Value("${recommend.weight.cf:0.5}") private double wCf;
    @Value("${recommend.weight.kg:0.3}") private double wKg;
    @Value("${recommend.weight.hot:0.2}") private double wHot;
    @Value("${recommend.top-n:10}") private int defaultTopN;

    private final RecommendationEngine engine = new RecommendationEngine();
    // One shared immutable snapshot; no per-user ranking cache. Changes become visible within 30s.
    private final BoundedTtlCache<String, Dataset> snapshots = new BoundedTtlCache<>(1, Duration.ofSeconds(30));
    private final BoundedTtlCache<String, List<Related>> graphRows = new BoundedTtlCache<>(512, Duration.ofSeconds(15));
    private final BoundedTtlCache<String, Map<Long, String>> kpNames = new BoundedTtlCache<>(1, Duration.ofSeconds(30));

    private record Dataset(Snapshot snapshot, Map<Long, LearningResource> resources, Map<String, Long> byNo) { }

    public List<RecommendItem> personalRecommend(Long userId, Integer topN) {
        return personalRecommend(userId, topN, null);
    }

    public List<RecommendItem> personalRecommend(Long userId, Integer topN, List<Long> interests) {
        Dataset data = dataset();
        return rank(data, userId, interests, count(topN)).stream().map(s -> item(data, s)).toList();
    }

    public List<RecommendItem> hotRecommend(Integer topN, Long collegeId) {
        Dataset data = dataset();
        Collection<Resource> catalog = data.snapshot().resources().values().stream()
                .filter(r -> collegeId == null || collegeId.equals(data.resources().get(r.id()).getCollegeId())).toList();
        Snapshot snapshot = engine.prepare(catalog, Map.of());
        return engine.rank(snapshot, -1, List.of(), (id, limit) -> List.of(), new Weights(0, 0, 1), count(topN))
                .stream().map(s -> item(data, s)).toList();
    }

    public List<RecommendItem> relatedRecommend(String resourceNo, Integer topN) {
        Dataset data = dataset();
        Long sourceId = data.byNo().get(resourceNo);
        if (sourceId == null && resourceNo != null && resourceNo.matches("\\d+")) {
            try { sourceId = Long.parseLong(resourceNo); } catch (NumberFormatException ignored) { }
        }
        LearningResource source = data.resources().get(sourceId);
        String key = source == null ? resourceNo : source.getResourceNo();
        List<Map<String, Object>> rows;
        try {
            rows = graphService.queryRelatedWithRelation(key, count(topN));
        } catch (RuntimeException unavailable) {
            log.warn("语义推荐图谱不可用，返回空关联列表");
            return List.of();
        }
        List<RecommendItem> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long id = data.byNo().get(String.valueOf(row.get("resourceNo")));
            LearningResource resource = data.resources().get(id);
            if (resource == null || !Integer.valueOf(1).equals(resource.getStatus())) continue;
            String relation = String.valueOf(row.get("relation"));
            RecommendItem item = details(resource);
            item.setSource("kg");
            item.setRecommendScore(switch (relation) { case "SAME_COURSE" -> 0.9; case "PREREQUISITE" -> 0.8; case "RELATED" -> 0.7; default -> 0.6; });
            String label = switch (relation) { case "SAME_COURSE" -> "同课程"; case "PREREQUISITE" -> "先修/后续"; case "RELATED" -> "横向相关"; default -> "语义相关"; };
            item.setReason("与《" + (source == null ? "" : source.getTitle()) + "》存在「" + label + "」知识关联");
            result.add(item);
        }
        return result;
    }

    /** Explains the baseline request (without explicit interestKpIds), using identical candidate normalization. */
    public RecommendExplainVO explain(Long userId, Long resourceId) {
        Dataset data = dataset();
        LearningResource resource = data.resources().get(resourceId);
        if (resource == null) throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在");
        ScoredResource scored = rank(data, userId, List.of(), data.resources().size()).stream()
                .filter(s -> s.resourceId() == resourceId).findFirst().orElse(null);
        RecommendExplainVO result = new RecommendExplainVO();
        result.setResourceId(resourceId);
        result.setTitle(resource.getTitle());
        List<String> sources = new ArrayList<>();
        Map<String, Double> breakdown = new LinkedHashMap<>();
        double cf = scored == null ? 0 : scored.cf(), kg = scored == null ? 0 : scored.kg(), hot = scored == null ? 0 : scored.hot();
        if (wCf * cf > 0) sources.add("cf");
        if (wKg * kg > 0) sources.add("kg");
        if (wHot * hot > 0) sources.add("hot");
        breakdown.put("cf", round2(cf)); breakdown.put("kg", round2(kg)); breakdown.put("hot", round2(hot));
        breakdown.put("cfContribution", round2(wCf * cf)); breakdown.put("kgContribution", round2(wKg * kg));
        breakdown.put("hotContribution", round2(wHot * hot)); breakdown.put("final", round2(scored == null ? 0 : scored.score()));
        result.setSources(sources);
        result.setScoreBreakdown(breakdown);
        result.setReason(scored == null ? "该资源当前没有可用的推荐分数（已交互、未通过审核或缺少信号）" : reason(data, scored));
        return result;
    }

    /** Optional immediate invalidation for write paths; TTL remains the bounded default freshness policy. */
    public void invalidateSnapshot() {
        snapshots.clear();
        graphRows.clear();
        kpNames.clear();
    }

    private int count(Integer topN) {
        return topN == null || topN < 1 ? defaultTopN : topN;
    }

    private Dataset dataset() {
        return snapshots.get("catalog", () -> {
            Map<Long, LearningResource> resources = new TreeMap<>();
            Map<String, Long> byNo = new HashMap<>();
            for (LearningResource resource : resourceMapper.selectList(null)) {
                resources.put(resource.getId(), resource);
                byNo.put(resource.getResourceNo(), resource.getId());
            }
            Map<Long, Map<Long, Double>> users = new TreeMap<>();
            for (FavoriteBehavior f : favoriteMapper.selectList(null)) put(users, f.getStudentId(), f.getResourceId(), 1);
            for (DownloadBehavior d : downloadMapper.selectList(null)) put(users, d.getStudentId(), d.getResourceId(), 0.8);
            for (RatingBehavior r : ratingMapper.selectList(null)) put(users, r.getStudentId(), r.getResourceId(), (r.getScore() == null ? 0 : r.getScore()) / 5.0);
            List<Resource> catalog = resources.values().stream().map(r -> new Resource(r.getId(), r.getPrimaryKpId(),
                    r.getHeat() == null ? 0 : Math.max(0, r.getHeat().doubleValue()),
                    r.getDownloadCount() == null ? 0 : r.getDownloadCount(), Integer.valueOf(1).equals(r.getStatus()))).toList();
            return new Dataset(engine.prepare(catalog, users), Map.copyOf(resources), Map.copyOf(byNo));
        });
    }

    private static void put(Map<Long, Map<Long, Double>> users, Long user, Long resource, double value) {
        if (user != null && resource != null) users.computeIfAbsent(user, k -> new TreeMap<>()).merge(resource, value, Double::sum);
    }

    private List<ScoredResource> rank(Dataset data, Long user, List<Long> interests, int topN) {
        return engine.rank(data.snapshot(), user == null ? -1 : user, interests, (seed, limit) -> {
            LearningResource resource = data.resources().get(seed);
            return graphRows.get(resource.getResourceNo() + ":" + limit, () -> {
                List<Related> related = new ArrayList<>();
                for (Map<String, Object> row : graphService.queryRelatedByResourceNo(resource.getResourceNo(), limit)) {
                    Long id = data.byNo().get(String.valueOf(row.get("resourceNo")));
                    if (id != null && row.get("hops") instanceof Number hops) related.add(new Related(id, hops.intValue()));
                }
                return List.copyOf(related);
            });
        }, new Weights(wCf, wKg, wHot), topN);
    }

    private RecommendItem item(Dataset data, ScoredResource scored) {
        RecommendItem result = details(data.resources().get(scored.resourceId()));
        result.setRecommendScore(round2(scored.score()));
        result.setSource(scored.source());
        result.setReason(reason(data, scored));
        return result;
    }

    private RecommendItem details(LearningResource resource) {
        RecommendItem result = new RecommendItem();
        result.setResourceId(resource.getId()); result.setResourceNo(resource.getResourceNo()); result.setTitle(resource.getTitle());
        result.setType(resource.getType()); result.setCoverUrl(resource.getCoverUrl()); result.setAuthor(resource.getAuthor());
        result.setAvgRating(resource.getAvgRating() == null ? null : resource.getAvgRating().doubleValue());
        return result;
    }

    private String reason(Dataset data, ScoredResource score) {
        if ("cf".equals(score.source())) {
            LearningResource seed = data.resources().get(score.cfSeedId());
            return "与你交互过的《" + (seed == null ? "" : seed.getTitle()) + "》行为向量余弦相似度为 "
                    + String.format(Locale.ROOT, "%.4f", score.cfSimilarity()) + "（ItemCF）";
        }
        if ("kg".equals(score.source())) {
            if (score.interestKpId() != null) {
                String name = knowledgePointNames().getOrDefault(score.interestKpId(), "知识点 " + score.interestKpId());
                return score.hops() == 0 ? "因为你关注了知识点《" + name + "》，推荐该知识点下的资源"
                        : "与知识点《" + name + "》下的资源存在知识链关联（" + score.hops() + " 跳）";
            }
            LearningResource seed = data.resources().get(score.kgSeedId());
            return "与你交互过的《" + (seed == null ? "" : seed.getTitle()) + "》存在知识链关联（" + score.hops() + " 跳）";
        }
        LearningResource resource = data.resources().get(score.resourceId());
        return "热门资源，综合热度信号，评分 " + (resource.getAvgRating() == null ? "—" : resource.getAvgRating());
    }

    private Map<Long, String> knowledgePointNames() {
        try {
            return kpNames.get("names", () -> {
                Map<Long, String> names = new HashMap<>();
                for (GraphNode node : graphService.listNodes("KNOWLEDGE_POINT", null, 300)) {
                    try { names.put(Long.valueOf(node.getId()), node.getLabel()); } catch (NumberFormatException ignored) { }
                }
                return Map.copyOf(names);
            });
        } catch (RuntimeException unavailable) {
            return Map.of();
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
