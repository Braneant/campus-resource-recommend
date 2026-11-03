package com.campus.recommend.recommendation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationEngineTest {
    private final RecommendationEngine engine = new RecommendationEngine();

    @Test
    void normalizationPreservesZerosAndMapsConstantPositiveSignalToOne() {
        assertEquals(Map.of(1L, 0.0, 2L, 1.0, 3L, 0.5),
                RecommendationEngine.normalize(Map.of(1L, 2.0, 2L, 6.0, 3L, 4.0)));
        assertEquals(Map.of(1L, 1.0, 2L, 1.0), RecommendationEngine.normalize(Map.of(1L, 0.8, 2L, 0.8)));
        assertEquals(Map.of(1L, 0.0), RecommendationEngine.normalize(Map.of(1L, 0.0)));
    }

    @Test
    void coldUserCanUseExplicitInterestWithoutBehavior() {
        var snapshot = engine.prepare(List.of(resource(1, 10, 1), resource(2, 20, 0.5)), Map.of());
        var result = engine.rank(snapshot, 99, List.of(20L), (id, limit) -> List.of(),
                new RecommendationEngine.Weights(0, 1, 0), 10);
        assertEquals(List.of(2L), result.stream().map(RecommendationEngine.ScoredResource::resourceId).toList());
        assertEquals(20L, result.get(0).interestKpId());
    }

    @Test
    void weightedContributionDeterminesSourceAndFullPrecisionBreaksNearTies() {
        var snapshot = engine.prepare(List.of(resource(1, 10, 0), resource(2, 20, 0.501),
                resource(3, 20, 0.504), resource(4, 30, 1)), Map.of());
        var result = engine.rank(snapshot, 99, List.of(20L), (id, limit) -> List.of(),
                new RecommendationEngine.Weights(0, 0.1, 0.9), 10);
        assertEquals(List.of(4L, 3L, 2L), result.stream().map(RecommendationEngine.ScoredResource::resourceId).toList());
        assertEquals("hot", result.get(1).source());
        assertEquals(0.5536, result.get(1).score(), 1e-12);
    }

    @Test
    void tiesHaveStableAscendingResourceOrder() {
        var snapshot = engine.prepare(List.of(resource(17, 10, 1), resource(1, 20, 1)), Map.of());
        var result = engine.rank(snapshot, 99, List.of(), (id, limit) -> List.of(),
                new RecommendationEngine.Weights(0, 0, 1), 10);
        assertEquals(List.of(1L, 17L), result.stream().map(RecommendationEngine.ScoredResource::resourceId).toList());
    }

    @Test
    void cosinePredictionUsesTrainingUsersAndFiltersHistoryAndUnapprovedItems() {
        var snapshot = engine.prepare(List.of(resource(1, 10, 0), resource(2, 20, 1),
                        new RecommendationEngine.Resource(3, 30L, 1, 0, false)),
                Map.of(1L, Map.of(1L, 0.8), 2L, Map.of(1L, 1.0, 2L, 1.0, 3L, 1.0)));
        var result = engine.rank(snapshot, 1, List.of(), (id, limit) -> List.of(),
                new RecommendationEngine.Weights(1, 0, 0), 10);
        assertEquals(List.of(2L), result.stream().map(RecommendationEngine.ScoredResource::resourceId).toList());
        assertEquals(1 / Math.sqrt(1.64), result.get(0).cfSimilarity(), 1e-12);
        assertEquals(1L, result.get(0).cfSeedId());
    }

    @Test
    void unavailableGraphDoesNotAbortOtherRoutes() {
        var snapshot = engine.prepare(List.of(resource(1, 10, 0), resource(2, 20, 1)), Map.of(1L, Map.of(1L, 1.0)));
        var result = engine.rank(snapshot, 1, List.of(), (id, limit) -> { throw new IllegalStateException("offline"); },
                RecommendationEngine.Weights.DEFAULT, 10);
        assertEquals(List.of(2L), result.stream().map(RecommendationEngine.ScoredResource::resourceId).toList());
        assertEquals("hot", result.get(0).source());
    }

    @Test
    void snapshotCopiesInputsSoLaterMutationDoesNotChangeRanking() {
        Map<Long, Double> row = new java.util.HashMap<>(Map.of(1L, 1.0));
        var snapshot = engine.prepare(List.of(resource(1, 10, 0), resource(2, 20, 1)), Map.of(1L, row));
        row.put(2L, 1.0);
        assertEquals(2L, engine.rank(snapshot, 1, List.of(), (id, limit) -> List.of(),
                RecommendationEngine.Weights.DEFAULT, 1).get(0).resourceId());
    }

    private static RecommendationEngine.Resource resource(long id, long kp, double heat) {
        return new RecommendationEngine.Resource(id, kp, heat, 0, true);
    }
}
