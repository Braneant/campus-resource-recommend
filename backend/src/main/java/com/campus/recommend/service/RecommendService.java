package com.campus.recommend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.recommend.common.BusinessException;
import com.campus.recommend.common.ResultCode;
import com.campus.recommend.entity.*;
import com.campus.recommend.mapper.*;
import com.campus.recommend.vo.RecommendExplainVO;
import com.campus.recommend.vo.RecommendItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐服务（系统核心）：三路混合推荐 + min-max 归一化 + 加权融合 + 可解释推荐
 *
 * 三路来源：
 *  - cf（ItemCF 协同过滤）：基于 favorite/download/rating 三张行为表构建「用户-资源」隐式反馈矩阵，
 *        计算资源-资源余弦相似度，再按用户历史资源加权预测候选资源分（详见 buildCfScores）。
 *  - kg（知识图谱语义关联）：取用户历史资源所属知识点，沿 Neo4j 多跳（同课程/先修/相关）汇聚关联资源（buildKgScores）。
 *  - hot（热度加权）：按 learning_resource.heat 取全站/同学院热门（buildHotScores），解决冷启动。
 *
 * 融合：三路结果各自 min-max 归一化 → 加权求和 score = wCf·sCf + wKg·sKg + wHot·sHot
 *      （默认权重 0.5/0.3/0.2，可配置）→ 排序取 TopN。
 * 可解释：按 dominant 来源套规则模板生成 reason（不接大模型）。
 */
@Slf4j
@Service
public class RecommendService {

    @Autowired
    private FavoriteBehaviorMapper favoriteMapper;
    @Autowired
    private DownloadBehaviorMapper downloadMapper;
    @Autowired
    private RatingBehaviorMapper ratingMapper;
    @Autowired
    private LearningResourceMapper resourceMapper;
    @Autowired
    private GraphService graphService;

    /** 三路权重（可在 application.yml 的 recommend.weight 调整） */
    @Value("${recommend.weight.cf:0.5}")
    private double wCf;
    @Value("${recommend.weight.kg:0.3}")
    private double wKg;
    @Value("${recommend.weight.hot:0.2}")
    private double wHot;
    @Value("${recommend.top-n:10}")
    private int defaultTopN;

    /* ====================================================================== */
    /* 1. 个性化推荐（三路混合 + 加权融合）                                     */
    /* ====================================================================== */
    public List<RecommendItem> personalRecommend(Long userId, Integer topN) {
        topN = topN == null || topN < 1 ? defaultTopN : topN;

        // 全部资源（用于标题/属性补全，状态不限）
        List<LearningResource> all = resourceMapper.selectList(null);
        Map<Long, LearningResource> allRes = all.stream().collect(Collectors.toMap(LearningResource::getId, r -> r, (a, b) -> a));
        Map<String, LearningResource> resByNo = all.stream().collect(Collectors.toMap(LearningResource::getResourceNo, r -> r, (a, b) -> a));
        // 已通过资源集合
        Set<Long> passedIds = all.stream().filter(r -> r.getStatus() != null && r.getStatus() == 1)
                .map(LearningResource::getId).collect(Collectors.toSet());

        // 构建「用户-资源」隐式反馈矩阵
        Map<Long, Map<Long, Double>> userRes = buildUserResourceScores();

        // CF 路
        Map<Long, Double> cfRaw = new HashMap<>();
        Map<Long, String> cfBestTitle = new HashMap<>();
        Map<Long, Double> cfBestSim = new HashMap<>();
        Map<Long, Double> userHist = userRes.getOrDefault(userId, Collections.emptyMap());
        if (!userHist.isEmpty()) {
            Map<Long, Map<Long, Double>> sim = buildItemSimilarity(userRes);
            computeCfPrediction(userHist, sim, allRes, cfRaw, cfBestTitle, cfBestSim);
        }

        // KG 路
        Map<Long, Double> kgRaw = new HashMap<>();
        Map<Long, String> kgTitle = new HashMap<>();
        Map<Long, Integer> kgHops = new HashMap<>();
        if (!userHist.isEmpty()) {
            // 取历史资源的 resource_no
            List<String> histNos = userHist.keySet().stream()
                    .map(allRes::get).filter(Objects::nonNull)
                    .map(LearningResource::getResourceNo).collect(Collectors.toList());
            for (String no : histNos) {
                String histTitle = allRes.values().stream()
                        .filter(r -> no.equals(r.getResourceNo())).findFirst()
                        .map(LearningResource::getTitle).orElse("");
                for (Map<String, Object> c : graphService.queryRelatedByResourceNo(no, topN * 2)) {
                    LearningResource lr = resByNo.get(String.valueOf(c.get("resourceNo")));
                    if (lr == null) continue;
                    String rno = lr.getResourceNo();
                    long hops = ((Number) c.get("hops")).longValue();
                    // 多历史触发时保留最小 hops
                    if (!kgHops.containsKey(lr.getId()) || hops < kgHops.get(lr.getId())) {
                        double heat = c.get("heat") == null ? 0.0 : ((Number) c.get("heat")).doubleValue();
                        // 综合分：越近 hops 越高 + 热度贡献
                        double raw = 0.7 * (1.0 / (hops + 1)) + 0.3 * heat;
                        kgRaw.put(lr.getId(), raw);
                        kgTitle.put(lr.getId(), histTitle);
                        kgHops.put(lr.getId(), (int) hops);
                    }
                }
            }
        }

        // Hot 路
        Map<Long, Double> hotRaw = buildHotScores(null);

        // 候选并集
        Set<Long> candidates = new HashSet<>();
        candidates.addAll(cfRaw.keySet());
        candidates.addAll(kgRaw.keySet());
        candidates.addAll(hotRaw.keySet());

        // 各路 min-max 归一化
        Map<Long, Double> cfN = minMax(cfRaw);
        Map<Long, Double> kgN = minMax(kgRaw);
        Map<Long, Double> hotN = minMax(hotRaw);

        List<RecommendItem> items = new ArrayList<>();
        for (Long rid : candidates) {
            if (!passedIds.contains(rid)) continue;          // 仅推荐已通过资源
            if (userHist.containsKey(rid)) continue;          // 排除用户已交互资源
            double sCf = cfN.getOrDefault(rid, 0.0);
            double sKg = kgN.getOrDefault(rid, 0.0);
            double sHot = hotN.getOrDefault(rid, 0.0);
            double finalScore = wCf * sCf + wKg * sKg + wHot * sHot;
            if (finalScore <= 0) continue;
            String source = dominant(sCf, sKg, sHot);
            LearningResource r = allRes.get(rid);
            RecommendItem it = new RecommendItem();
            it.setResourceId(rid);
            it.setResourceNo(r.getResourceNo());
            it.setTitle(r.getTitle());
            it.setType(r.getType());
            it.setCoverUrl(r.getCoverUrl());
            it.setAuthor(r.getAuthor());
            it.setAvgRating(r.getAvgRating() == null ? null : r.getAvgRating().doubleValue());
            it.setRecommendScore(round2(finalScore));
            it.setSource(source);
            it.setReason(buildReason(source, r, sCf, sKg, sHot, cfBestTitle, cfBestSim, kgTitle, kgHops));
            items.add(it);
        }
        items.sort((a, b) -> Double.compare(b.getRecommendScore(), a.getRecommendScore()));
        return items.size() > topN ? items.subList(0, topN) : items;
    }

    /* ====================================================================== */
    /* 2. 热门推荐                                                            */
    /* ====================================================================== */
    public List<RecommendItem> hotRecommend(Integer topN, Long collegeId) {
        topN = topN == null || topN < 1 ? defaultTopN : topN;
        Map<Long, Double> hotRaw = buildHotScores(collegeId);
        Map<Long, Double> hotN = minMax(hotRaw);
        // 直接按热度原始值排序更直观
        List<Map.Entry<Long, Double>> sorted = new ArrayList<>(hotRaw.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<RecommendItem> items = new ArrayList<>();
        int n = Math.min(topN, sorted.size());
        for (int i = 0; i < n; i++) {
            Long rid = sorted.get(i).getKey();
            LearningResource r = resourceMapper.selectById(rid);
            if (r == null) continue;
            RecommendItem it = new RecommendItem();
            it.setResourceId(rid);
            it.setResourceNo(r.getResourceNo());
            it.setTitle(r.getTitle());
            it.setType(r.getType());
            it.setCoverUrl(r.getCoverUrl());
            it.setAuthor(r.getAuthor());
            it.setAvgRating(r.getAvgRating() == null ? null : r.getAvgRating().doubleValue());
            it.setRecommendScore(round2(hotN.getOrDefault(rid, sorted.get(i).getValue())));
            it.setSource("hot");
            it.setReason("热门资源，下载量高、评分 " + (r.getAvgRating() == null ? "—" : r.getAvgRating()) +
                    (collegeId != null ? "（同学院）" : "（全站）"));
            items.add(it);
        }
        return items;
    }

    /* ====================================================================== */
    /* 3. 知识图谱语义相关（单资源）                                           */
    /* ====================================================================== */
    public List<RecommendItem> relatedRecommend(String resourceNo, Integer topN) {
        topN = topN == null || topN < 1 ? defaultTopN : topN;
        LearningResource src = resourceMapper.selectOne(
                new LambdaQueryWrapper<LearningResource>().eq(LearningResource::getResourceNo, resourceNo));
        String srcTitle = src != null ? src.getTitle() : "";
        List<Map<String, Object>> rels = graphService.queryRelatedWithRelation(resourceNo, topN);
        Map<String, LearningResource> resByNo = resourceMapper.selectList(null).stream()
                .collect(Collectors.toMap(LearningResource::getResourceNo, r -> r, (a, b) -> a));
        List<RecommendItem> items = new ArrayList<>();
        for (Map<String, Object> c : rels) {
            LearningResource r = resByNo.get(String.valueOf(c.get("resourceNo")));
            if (r == null || r.getStatus() == null || r.getStatus() != 1) continue;
            String relation = String.valueOf(c.get("relation"));
            RecommendItem it = new RecommendItem();
            it.setResourceId(r.getId());
            it.setResourceNo(r.getResourceNo());
            it.setTitle(r.getTitle());
            it.setType(r.getType());
            it.setCoverUrl(r.getCoverUrl());
            it.setAuthor(r.getAuthor());
            it.setAvgRating(r.getAvgRating() == null ? null : r.getAvgRating().doubleValue());
            it.setRecommendScore(round2(relationWeight(relation)));
            it.setSource("kg");
            it.setReason("与《" + srcTitle + "》存在「" + relationLabelZh(relation) + "」知识关联");
            items.add(it);
        }
        return items;
    }

    /* ====================================================================== */
    /* 4. 单条推荐解释                                                        */
    /* ====================================================================== */
    public RecommendExplainVO explain(Long userId, Long resourceId) {
        LearningResource target = resourceMapper.selectById(resourceId);
        if (target == null) throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在");

        Map<Long, Map<Long, Double>> userRes = buildUserResourceScores();
        Map<Long, Double> userHist = userRes.getOrDefault(userId, Collections.emptyMap());
        Map<Long, Double> cfRaw = new HashMap<>();
        Map<Long, String> cfBestTitle = new HashMap<>();
        Map<Long, Double> cfBestSim = new HashMap<>();
        if (!userHist.isEmpty()) {
            Map<Long, Map<Long, Double>> sim = buildItemSimilarity(userRes);
            computeCfPrediction(userHist, sim, resourceMapper.selectList(null).stream()
                    .collect(Collectors.toMap(LearningResource::getId, r -> r, (a, b) -> a)), cfRaw, cfBestTitle, cfBestSim);
        }
        Map<Long, Double> kgRaw = new HashMap<>();
        Map<Long, String> kgTitle = new HashMap<>();
        Map<Long, Integer> kgHops = new HashMap<>();
        Map<String, LearningResource> resByNo = resourceMapper.selectList(null).stream()
                .collect(Collectors.toMap(LearningResource::getResourceNo, r -> r, (a, b) -> a));
        if (!userHist.isEmpty()) {
            List<String> histNos = userHist.keySet().stream().map(id -> resByNo.values().stream()
                            .filter(r -> r.getId().equals(id)).findFirst().map(LearningResource::getResourceNo).orElse(null))
                    .filter(Objects::nonNull).collect(Collectors.toList());
            for (String no : histNos) {
                for (Map<String, Object> c : graphService.queryRelatedByResourceNo(no, 20)) {
                    LearningResource lr = resByNo.get(String.valueOf(c.get("resourceNo")));
                    if (lr == null || !lr.getId().equals(resourceId)) continue;
                    long hops = ((Number) c.get("hops")).longValue();
                    double heat = c.get("heat") == null ? 0.0 : ((Number) c.get("heat")).doubleValue();
                    kgRaw.put(resourceId, 0.7 * (1.0 / (hops + 1)) + 0.3 * heat);
                    kgTitle.put(resourceId, resByNo.values().stream().filter(r -> no.equals(r.getResourceNo()))
                            .findFirst().map(LearningResource::getTitle).orElse(""));
                    kgHops.put(resourceId, (int) hops);
                }
            }
        }
        Map<Long, Double> hotRaw = buildHotScores(null);

        Map<Long, Double> cfN = minMax(cfRaw);
        Map<Long, Double> kgN = minMax(kgRaw);
        Map<Long, Double> hotN = minMax(hotRaw);
        double sCf = cfN.getOrDefault(resourceId, 0.0);
        double sKg = kgN.getOrDefault(resourceId, 0.0);
        double sHot = hotN.getOrDefault(resourceId, 0.0);
        double finalScore = wCf * sCf + wKg * sKg + wHot * sHot;

        List<String> sources = new ArrayList<>();
        if (sCf > 0) sources.add("cf");
        if (sKg > 0) sources.add("kg");
        if (sHot > 0) sources.add("hot");

        RecommendExplainVO vo = new RecommendExplainVO();
        vo.setResourceId(resourceId);
        vo.setTitle(target.getTitle());
        vo.setSources(sources);
        StringBuilder reason = new StringBuilder();
        if (sKg > 0) reason.append("与你看过的《").append(kgTitle.getOrDefault(resourceId, ""))
                .append("》存在知识链关联（kg）；");
        if (sHot > 0) reason.append("同时是").append(target.getCollegeId() != null ? "同学院" : "全站")
                .append("热门资源（hot）；");
        if (sCf > 0) reason.append("协同过滤也给出相似度信号（cf）");
        if (reason.length() == 0) reason.append("暂无足够行为/图谱信号，按热度兜底推荐");
        vo.setReason(reason.toString());
        Map<String, Double> breakdown = new HashMap<>();
        breakdown.put("cf", round2(sCf));
        breakdown.put("kg", round2(sKg));
        breakdown.put("hot", round2(sHot));
        breakdown.put("final", round2(finalScore));
        vo.setScoreBreakdown(breakdown);
        return vo;
    }

    /* ====================================================================== */
    /* 内部：CF 三表 → 隐式反馈矩阵                                            */
    /* ====================================================================== */

    /**
     * 构建「用户-资源」隐式反馈分值矩阵。
     * 权重：收藏 1.0、下载 0.8、评分 score/5.0；同用户对同资源多行为求和后截断到 [0,1]。
     */
    private Map<Long, Map<Long, Double>> buildUserResourceScores() {
        Map<Long, Map<Long, Double>> m = new HashMap<>();
        // 收藏
        for (FavoriteBehavior f : favoriteMapper.selectList(null)) {
            putScore(m, f.getStudentId(), f.getResourceId(), 1.0);
        }
        // 下载
        for (DownloadBehavior d : downloadMapper.selectList(null)) {
            putScore(m, d.getStudentId(), d.getResourceId(), 0.8);
        }
        // 评分（显式）
        for (RatingBehavior r : ratingMapper.selectList(null)) {
            double s = (r.getScore() == null ? 0 : r.getScore()) / 5.0;
            putScore(m, r.getStudentId(), r.getResourceId(), s);
        }
        // 截断 [0,1]
        for (Map<Long, Double> res : m.values()) {
            for (Map.Entry<Long, Double> e : res.entrySet()) {
                double v = Math.min(1.0, e.getValue());
                res.put(e.getKey(), v);
            }
        }
        return m;
    }

    private void putScore(Map<Long, Map<Long, Double>> m, Long uid, Long rid, double score) {
        m.computeIfAbsent(uid, k -> new HashMap<>())
                .merge(rid, score, Double::sum);
    }

    /**
     * 基于「用户-资源」矩阵构建资源-资源余弦相似度 sim(i,j)。
     * sim = Σ_u(r_ui·r_uj) / (√(Σr_ui²)·√(Σr_uj²))
     */
    private Map<Long, Map<Long, Double>> buildItemSimilarity(Map<Long, Map<Long, Double>> userRes) {
        // cooc[i][j] = Σ r_ui·r_uj ；norm[i] = Σ r_ui²
        Map<Long, Map<Long, Double>> cooc = new HashMap<>();
        Map<Long, Double> norm = new HashMap<>();
        for (Map<Long, Double> res : userRes.values()) {
            List<Long> items = new ArrayList<>(res.keySet());
            for (Long i : items) norm.merge(i, res.get(i) * res.get(i), Double::sum);
            for (int a = 0; a < items.size(); a++) {
                for (int b = a + 1; b < items.size(); b++) {
                    Long i = items.get(a), j = items.get(b);
                    double prod = res.get(i) * res.get(j);
                    cooc.computeIfAbsent(i, k -> new HashMap<>()).merge(j, prod, Double::sum);
                    cooc.computeIfAbsent(j, k -> new HashMap<>()).merge(i, prod, Double::sum);
                }
            }
        }
        // 计算余弦相似度
        Map<Long, Map<Long, Double>> sim = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> e : cooc.entrySet()) {
            Long i = e.getKey();
            double ni = Math.sqrt(norm.getOrDefault(i, 0.0));
            Map<Long, Double> row = new HashMap<>();
            for (Map.Entry<Long, Double> e2 : e.getValue().entrySet()) {
                Long j = e2.getKey();
                double nj = Math.sqrt(norm.getOrDefault(j, 0.0));
                if (ni > 0 && nj > 0) {
                    row.put(j, e2.getValue() / (ni * nj));
                }
            }
            sim.put(i, row);
        }
        return sim;
    }

    /**
     * 基于用户历史资源与各资源相似度，预测候选资源分（CF 原始分，区间约 [0,1]）。
     * 同时记录每个候选贡献最大的历史资源标题与相似度（用于可解释）。
     */
    private void computeCfPrediction(Map<Long, Double> userHist,
                                     Map<Long, Map<Long, Double>> sim,
                                     Map<Long, LearningResource> allRes,
                                     Map<Long, Double> cfRaw,
                                     Map<Long, String> cfBestTitle,
                                     Map<Long, Double> cfBestSim) {
        Map<Long, Double> num = new HashMap<>();
        Map<Long, Double> den = new HashMap<>();
        for (Map.Entry<Long, Double> h : userHist.entrySet()) {
            Long i = h.getKey();
            double hi = h.getValue();
            Map<Long, Double> sims = sim.get(i);
            if (sims == null) continue;
            for (Map.Entry<Long, Double> e : sims.entrySet()) {
                Long c = e.getKey();
                if (userHist.containsKey(c)) continue; // 排除已交互
                double s = e.getValue();
                num.merge(c, s * hi, Double::sum);
                den.merge(c, s, Double::sum);
                // 记录最强贡献者
                if (cfBestSim.get(c) == null || s > cfBestSim.get(c)) {
                    cfBestSim.put(c, s);
                    LearningResource hr = allRes.get(i);
                    cfBestTitle.put(c, hr != null ? hr.getTitle() : "");
                }
            }
        }
        for (Map.Entry<Long, Double> e : num.entrySet()) {
            double d = den.getOrDefault(e.getKey(), 0.0);
            cfRaw.put(e.getKey(), d > 0 ? e.getValue() / d : 0.0);
        }
    }

    /** Hot 路：取已通过资源 heat 作为原始分（可限定同学院） */
    private Map<Long, Double> buildHotScores(Long collegeId) {
        LambdaQueryWrapper<LearningResource> qw = new LambdaQueryWrapper<>();
        qw.eq(LearningResource::getStatus, 1);
        if (collegeId != null) qw.eq(LearningResource::getCollegeId, collegeId);
        List<LearningResource> list = resourceMapper.selectList(qw);
        Map<Long, Double> m = new HashMap<>();
        for (LearningResource r : list) {
            m.put(r.getId(), r.getHeat() == null ? 0.0 : r.getHeat().doubleValue());
        }
        return m;
    }

    /* ====================================================================== */
    /* 内部：归一化 / 融合 / 可解释                                            */
    /* ====================================================================== */

    /** min-max 归一化到 [0,1]（全相等时非零置 1） */
    private Map<Long, Double> minMax(Map<Long, Double> raw) {
        Map<Long, Double> out = new HashMap<>();
        if (raw.isEmpty()) return out;
        double min = Collections.min(raw.values());
        double max = Collections.max(raw.values());
        if (max == min) {
            for (Map.Entry<Long, Double> e : raw.entrySet()) out.put(e.getKey(), max == 0 ? 0.0 : 1.0);
        } else {
            for (Map.Entry<Long, Double> e : raw.entrySet()) out.put(e.getKey(), (e.getValue() - min) / (max - min));
        }
        return out;
    }

    /** 选取主导来源 */
    private String dominant(double cf, double kg, double hot) {
        double max = Math.max(Math.max(cf, kg), hot);
        if (max <= 0) return "hot";
        if (max == cf) return "cf";
        if (max == kg) return "kg";
        return "hot";
    }

    /** 生成可解释理由（规则模板） */
    private String buildReason(String source, LearningResource r, double sCf, double sKg, double sHot,
                               Map<Long, String> cfBestTitle, Map<Long, Double> cfBestSim,
                               Map<Long, String> kgTitle, Map<Long, Integer> kgHops) {
        return switch (source) {
            case "cf" -> {
                String t = cfBestTitle.getOrDefault(r.getId(), "");
                double sim = cfBestSim.getOrDefault(r.getId(), 0.0);
                yield "因为你与《" + t + "》兴趣相似，" + (int) (sim * 100) + "% 的同类同学也选择了《" + r.getTitle() + "》";
            }
            case "kg" -> {
                String t = kgTitle.getOrDefault(r.getId(), "");
                int hops = kgHops.getOrDefault(r.getId(), 1);
                yield "与你看过的《" + t + "》存在知识链关联（" + hops + " 跳），属于相关/先修知识点推荐";
            }
            default -> "热门资源，下载量高、评分 " + (r.getAvgRating() == null ? "—" : r.getAvgRating());
        };
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private double relationWeight(String relation) {
        return switch (relation) {
            case "SAME_COURSE" -> 0.9;
            case "PREREQUISITE" -> 0.8;
            case "RELATED" -> 0.7;
            default -> 0.6;
        };
    }

    private String relationLabelZh(String relation) {
        return switch (relation) {
            case "SAME_COURSE" -> "同课程";
            case "PREREQUISITE" -> "先修/后续";
            case "RELATED" -> "横向相关";
            default -> "语义相关";
        };
    }
}
