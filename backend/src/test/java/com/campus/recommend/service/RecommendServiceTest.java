package com.campus.recommend.service;

import com.campus.recommend.entity.FavoriteBehavior;
import com.campus.recommend.entity.LearningResource;
import com.campus.recommend.mapper.*;
import com.campus.recommend.vo.RecommendItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecommendServiceTest {
    private final RecommendService service = new RecommendService();
    private final LearningResourceMapper resources = mock(LearningResourceMapper.class);
    private final FavoriteBehaviorMapper favorites = mock(FavoriteBehaviorMapper.class);
    private final DownloadBehaviorMapper downloads = mock(DownloadBehaviorMapper.class);
    private final RatingBehaviorMapper ratings = mock(RatingBehaviorMapper.class);
    private final GraphService graph = mock(GraphService.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "resourceMapper", resources);
        ReflectionTestUtils.setField(service, "favoriteMapper", favorites);
        ReflectionTestUtils.setField(service, "downloadMapper", downloads);
        ReflectionTestUtils.setField(service, "ratingMapper", ratings);
        ReflectionTestUtils.setField(service, "graphService", graph);
        ReflectionTestUtils.setField(service, "defaultTopN", 10);
        weights(0.5, 0.3, 0.2);
        when(favorites.selectList(any())).thenReturn(List.of());
        when(downloads.selectList(any())).thenReturn(List.of());
        when(ratings.selectList(any())).thenReturn(List.of());
        when(graph.listNodes(anyString(), isNull(), anyInt())).thenReturn(List.of());
        when(graph.queryRelatedByResourceNo(anyString(), anyInt())).thenReturn(List.of());
    }

    @Test
    void sourceReflectsWeightedContributionForColdInterest() {
        when(resources.selectList(any())).thenReturn(List.of(resource(1, 0, 10), resource(2, 1, 20)));
        weights(0, 0.1, 0.9);
        RecommendItem item = service.personalRecommend(99L, 1, List.of(20L)).get(0);
        assertEquals(2L, item.getResourceId());
        assertEquals("hot", item.getSource());
    }

    @Test
    void cfReasonDescribesCosineWithoutInventingPeoplePercentages() {
        when(resources.selectList(any())).thenReturn(List.of(resource(1, 0, 10), resource(2, 1, 20)));
        when(favorites.selectList(any())).thenReturn(List.of(favorite(1, 1), favorite(2, 1), favorite(2, 2)));
        weights(1, 0, 0);
        RecommendItem item = service.personalRecommend(1L, 1).get(0);
        assertTrue(item.getReason().contains("余弦相似度"), item.getReason());
        assertFalse(item.getReason().contains("%"), item.getReason());
    }

    @Test
    void sortsBeforeRoundingCloseScores() {
        when(resources.selectList(any())).thenReturn(List.of(resource(1, 0, 10), resource(2, 0.501, 20),
                resource(3, 0.504, 30), resource(4, 1, 40)));
        weights(0, 0, 1);
        assertEquals(List.of(4L, 3L), service.personalRecommend(99L, 2).stream().map(RecommendItem::getResourceId).toList());
    }

    @Test
    void graphFailureKeepsHotAndCfAvailable() {
        when(resources.selectList(any())).thenReturn(List.of(resource(1, 0, 10), resource(2, 1, 20)));
        when(favorites.selectList(any())).thenReturn(List.of(favorite(1, 1), favorite(2, 1), favorite(2, 2)));
        when(graph.queryRelatedByResourceNo(anyString(), anyInt())).thenThrow(new IllegalStateException("offline"));
        assertEquals(2L, service.personalRecommend(1L, 1).get(0).getResourceId());
    }

    @Test
    void onlyApprovedUnseenResourcesAreReturned() {
        LearningResource pending = resource(3, 1, 30);
        pending.setStatus(0);
        when(resources.selectList(any())).thenReturn(List.of(resource(1, 0.5, 10), resource(2, 0.8, 20), pending));
        when(favorites.selectList(any())).thenReturn(List.of(favorite(1, 1)));
        assertEquals(List.of(2L), service.personalRecommend(1L, 10).stream().map(RecommendItem::getResourceId).toList());
    }

    private void weights(double cf, double kg, double hot) {
        ReflectionTestUtils.setField(service, "wCf", cf);
        ReflectionTestUtils.setField(service, "wKg", kg);
        ReflectionTestUtils.setField(service, "wHot", hot);
    }

    private static LearningResource resource(long id, double heat, long kp) {
        LearningResource r = new LearningResource();
        r.setId(id); r.setResourceNo("R" + id); r.setTitle("resource " + id);
        r.setPrimaryKpId(kp); r.setHeat(BigDecimal.valueOf(heat)); r.setStatus(1);
        r.setDownloadCount(0); r.setType(1);
        return r;
    }

    private static FavoriteBehavior favorite(long user, long resource) {
        FavoriteBehavior f = new FavoriteBehavior();
        f.setStudentId(user); f.setResourceId(resource);
        return f;
    }
}
