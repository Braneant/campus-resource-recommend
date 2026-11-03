package com.campus.recommend.recommendation;

import java.util.*;

/**
 * Dependency-free product algorithm, also compiled directly by evaluation and benchmark CLIs.
 * Input heat and behavioral weights are supplied by the caller: offline callers MUST use train only.
 */
public final class RecommendationEngine {
    public record Resource(long id, Long kpId, double heat, int downloadCount, boolean approved) {
        public Resource {
            if (!Double.isFinite(heat) || heat < 0) throw new IllegalArgumentException("invalid resource heat");
        }
    }

    public record Related(long resourceId, int hops) { }

    @FunctionalInterface
    public interface SemanticLookup {
        List<Related> related(long resourceId, int limit);
    }

    public record Weights(double cf, double kg, double hot) {
        public static final Weights DEFAULT = new Weights(0.5, 0.3, 0.2);
        public Weights {
            if (!Double.isFinite(cf + kg + hot) || cf < 0 || kg < 0 || hot < 0 || cf + kg + hot <= 0)
                throw new IllegalArgumentException("finite nonnegative recommendation weights required");
        }
    }

    public record Snapshot(Map<Long, Resource> resources, Map<Long, Map<Long, Double>> history,
                           Map<Long, Map<Long, Double>> similarities) { }

    /** Scores keep full precision; only the HTTP presentation adapter rounds. */
    public record ScoredResource(long resourceId, double cf, double kg, double hot, double score,
                                 String source, Long cfSeedId, double cfSimilarity,
                                 Long kgSeedId, Long interestKpId, int hops) { }

    private record CfEvidence(long seedId, double similarity, double contribution) { }
    private record KgEvidence(double score, long seedId, Long interestKpId, int hops) { }
    private static final int GRAPH_LIMIT = 100;
    private static final int INTEREST_SEEDS = 8;
    private static final int EXPAND_SEEDS = 4;

    /** One immutable catalog/behavior snapshot and precomputed weighted cosine ItemCF matrix. */
    public Snapshot prepare(Collection<Resource> resources, Map<Long, Map<Long, Double>> userScores) {
        Map<Long, Resource> catalog = new TreeMap<>();
        for (Resource resource : resources) catalog.put(resource.id(), resource);
        Map<Long, Map<Long, Double>> history = new TreeMap<>();
        userScores.forEach((user, scores) -> {
            Map<Long, Double> row = new TreeMap<>();
            scores.forEach((id, weight) -> {
                if (catalog.containsKey(id) && weight != null && Double.isFinite(weight) && weight > 0)
                    row.put(id, Math.min(1.0, weight));
            });
            if (!row.isEmpty()) history.put(user, Collections.unmodifiableMap(row));
        });
        return new Snapshot(Collections.unmodifiableMap(catalog), Collections.unmodifiableMap(history), similarities(history));
    }

    public List<ScoredResource> rank(Snapshot snapshot, long userId, List<Long> interests,
                                     SemanticLookup graph, Weights weights, int topN) {
        if (topN <= 0) return List.of();
        Map<Long, Resource> catalog = snapshot.resources();
        Map<Long, Double> hist = snapshot.history().getOrDefault(userId, Map.of());
        Map<Long, Double> numerators = new TreeMap<>(), denominators = new TreeMap<>();
        Map<Long, CfEvidence> cfEvidence = new HashMap<>();
        for (var seed : hist.entrySet()) {
            for (var neighbor : snapshot.similarities().getOrDefault(seed.getKey(), Map.of()).entrySet()) {
                long candidate = neighbor.getKey();
                if (hist.containsKey(candidate) || !catalog.get(candidate).approved()) continue;
                double similarity = neighbor.getValue();
                double contribution = similarity * seed.getValue();
                numerators.merge(candidate, contribution, Double::sum);
                denominators.merge(candidate, similarity, Double::sum);
                CfEvidence best = cfEvidence.get(candidate);
                if (best == null || contribution > best.contribution())
                    cfEvidence.put(candidate, new CfEvidence(seed.getKey(), similarity, contribution));
            }
        }
        Map<Long, Double> cfRaw = new TreeMap<>();
        numerators.forEach((id, value) -> cfRaw.put(id, value / denominators.get(id)));

        Map<Long, KgEvidence> kgEvidence = new HashMap<>();
        if (weights.kg() > 0) {
            for (long seed : hist.keySet()) expand(snapshot, graph, seed, null, 0, GRAPH_LIMIT, kgEvidence);
            Set<Long> interestSet = interests == null ? Set.of() : new HashSet<>(interests);
            List<Resource> seeds = catalog.values().stream()
                    .filter(Resource::approved).filter(r -> r.kpId() != null && interestSet.contains(r.kpId()))
                    .sorted(Comparator.comparingInt(Resource::downloadCount).reversed().thenComparingLong(Resource::id))
                    .limit(INTEREST_SEEDS).toList();
            for (Resource seed : seeds) kgEvidence.put(seed.id(), new KgEvidence(1, seed.id(), seed.kpId(), 0));
            for (int i = 0; i < Math.min(EXPAND_SEEDS, seeds.size()); i++) {
                Resource seed = seeds.get(i);
                expand(snapshot, graph, seed.id(), seed.kpId(), 1, 6, kgEvidence);
            }
        }
        Map<Long, Double> kgRaw = new TreeMap<>(), hotRaw = new TreeMap<>();
        kgEvidence.forEach((id, evidence) -> kgRaw.put(id, evidence.score()));
        catalog.values().stream().filter(Resource::approved).forEach(r -> hotRaw.put(r.id(), r.heat()));
        Map<Long, Double> cf = normalize(cfRaw), kg = normalize(kgRaw), hot = normalize(hotRaw);
        List<ScoredResource> ranked = new ArrayList<>();
        for (Resource resource : catalog.values()) {
            long id = resource.id();
            if (!resource.approved() || hist.containsKey(id)) continue;
            double c = cf.getOrDefault(id, 0.0), g = kg.getOrDefault(id, 0.0), h = hot.getOrDefault(id, 0.0);
            double wc = weights.cf() * c, wg = weights.kg() * g, wh = weights.hot() * h;
            double score = wc + wg + wh;
            if (score <= 0) continue;
            String source = wc >= wg && wc >= wh ? "cf" : wg >= wh ? "kg" : "hot";
            CfEvidence ce = cfEvidence.get(id);
            KgEvidence ge = kgEvidence.get(id);
            ranked.add(new ScoredResource(id, c, g, h, score, source,
                    ce == null ? null : ce.seedId(), ce == null ? 0 : ce.similarity(),
                    ge == null ? null : ge.seedId(), ge == null ? null : ge.interestKpId(), ge == null ? 0 : ge.hops()));
        }
        ranked.sort(Comparator.comparingDouble(ScoredResource::score).reversed().thenComparingLong(ScoredResource::resourceId));
        return List.copyOf(ranked.subList(0, Math.min(topN, ranked.size())));
    }

    private void expand(Snapshot snapshot, SemanticLookup graph, long seed, Long interest, int extraHops,
                        int limit, Map<Long, KgEvidence> evidence) {
        List<Related> related;
        try {
            related = graph.related(seed, limit);
        } catch (RuntimeException unavailable) {
            return; // KG is optional; CF and Hot remain available.
        }
        if (related == null) return;
        for (Related row : related) {
            Resource resource = snapshot.resources().get(row.resourceId());
            if (resource == null || !resource.approved() || row.hops() < 1) continue;
            int hops = row.hops() + extraHops;
            double score = 0.7 / (hops + 1) + 0.3 * resource.heat();
            KgEvidence best = evidence.get(resource.id());
            if (best == null || score > best.score()) evidence.put(resource.id(), new KgEvidence(score, seed, interest, hops));
        }
    }

    static Map<Long, Double> normalize(Map<Long, Double> raw) {
        Map<Long, Double> out = new HashMap<>();
        if (raw.isEmpty()) return out;
        double min = Collections.min(raw.values()), max = Collections.max(raw.values());
        raw.forEach((id, value) -> out.put(id, max == min ? (max == 0 ? 0.0 : 1.0) : (value - min) / (max - min)));
        return out;
    }

    private Map<Long, Map<Long, Double>> similarities(Map<Long, Map<Long, Double>> history) {
        Map<Long, Double> norms = new TreeMap<>();
        Map<Long, Map<Long, Double>> dots = new TreeMap<>();
        for (Map<Long, Double> row : history.values()) {
            List<Long> ids = new ArrayList<>(row.keySet());
            for (long id : ids) norms.merge(id, row.get(id) * row.get(id), Double::sum);
            for (int a = 0; a < ids.size(); a++) for (int b = a + 1; b < ids.size(); b++) {
                long i = ids.get(a), j = ids.get(b);
                double dot = row.get(i) * row.get(j);
                dots.computeIfAbsent(i, k -> new TreeMap<>()).merge(j, dot, Double::sum);
                dots.computeIfAbsent(j, k -> new TreeMap<>()).merge(i, dot, Double::sum);
            }
        }
        Map<Long, Map<Long, Double>> similarities = new TreeMap<>();
        dots.forEach((id, neighbors) -> {
            Map<Long, Double> row = new TreeMap<>();
            neighbors.forEach((other, dot) -> row.put(other, Math.min(1.0, dot / Math.sqrt(norms.get(id) * norms.get(other)))));
            similarities.put(id, Collections.unmodifiableMap(row));
        });
        return Collections.unmodifiableMap(similarities);
    }
}
