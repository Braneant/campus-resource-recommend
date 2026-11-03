package com.campus.recommend.recommendation;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class BoundedTtlCacheTest {
    @Test
    void valueIsReusedUntilExpiryThenRefreshed() {
        AtomicLong clock = new AtomicLong();
        AtomicInteger loads = new AtomicInteger();
        var cache = new BoundedTtlCache<String, Integer>(2, Duration.ofSeconds(1), clock::get);
        assertEquals(1, cache.get("a", loads::incrementAndGet));
        clock.set(999_999_999);
        assertEquals(1, cache.get("a", loads::incrementAndGet));
        clock.set(1_000_000_000);
        assertEquals(2, cache.get("a", loads::incrementAndGet));
    }

    @Test
    void capacityEvictsLeastRecentlyUsedAndInvalidationLoadsFreshData() {
        AtomicInteger loads = new AtomicInteger();
        var cache = new BoundedTtlCache<String, Integer>(2, Duration.ofSeconds(30));
        cache.get("a", loads::incrementAndGet);
        cache.get("b", loads::incrementAndGet);
        cache.get("a", loads::incrementAndGet);
        cache.get("c", loads::incrementAndGet);
        assertEquals(4, cache.get("b", loads::incrementAndGet));
        cache.clear();
        assertEquals(5, cache.get("a", loads::incrementAndGet));
    }

    @Test
    void concurrentRequestsShareOneConsistentLoadedSnapshot() throws Exception {
        AtomicInteger loads = new AtomicInteger();
        var cache = new BoundedTtlCache<String, Integer>(1, Duration.ofSeconds(30));
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<java.util.concurrent.Callable<Integer>> calls = new ArrayList<>();
            for (int i = 0; i < 32; i++) calls.add(() -> cache.get("snapshot", loads::incrementAndGet));
            for (var future : executor.invokeAll(calls)) assertEquals(1, future.get());
            assertEquals(1, loads.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void failedLoadIsNotCached() {
        var cache = new BoundedTtlCache<String, Integer>(1, Duration.ofSeconds(30));
        assertThrows(IllegalStateException.class, () -> cache.get("a", () -> { throw new IllegalStateException("offline"); }));
        assertEquals(7, cache.get("a", () -> 7));
    }
}
