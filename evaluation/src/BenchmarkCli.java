package com.campus.recommend.experiment;

import com.campus.recommend.recommendation.RecommendationEngine;
import com.campus.recommend.recommendation.BoundedTtlCache;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** In-process engine benchmark: does not measure HTTP, database, or live Neo4j. */
public final class BenchmarkCli {
    public static void main(String[] args) throws Exception {
        var data = ExperimentData.load(Path.of(args[0]));
        var engine = new RecommendationEngine();
        var cache = new BoundedTtlCache<String, RecommendationEngine.Snapshot>(1, Duration.ofMinutes(5));
        var users = new ArrayList<>(data.queries().keySet());
        var snapshot = engine.prepare(data.resources(), data.history());
        for (long id : users) {
            var a = engine.rank(snapshot, id, data.queries().get(id), data.lookup(), RecommendationEngine.Weights.DEFAULT, 10);
            var b = engine.rank(cache.get("all", () -> engine.prepare(data.resources(), data.history())), id, data.queries().get(id), data.lookup(), RecommendationEngine.Weights.DEFAULT, 10);
            if (!a.equals(b)) throw new IllegalStateException("Rankings differ");
        }
        for (String mode : List.of("baseline", "cached")) {
            for (int i=0; i<100; i++) request(mode, i, users, data, engine, cache);
            var executor = Executors.newFixedThreadPool(4);
            var start = System.nanoTime();
            var futures = new ArrayList<Future<Long>>();
            for (int i=0; i<400; i++) {
                final int n = i;
                futures.add(executor.submit(() -> { long t=System.nanoTime(); request(mode,n,users,data,engine,cache); return System.nanoTime()-t; }));
            }
            var elapsed = System.nanoTime()-start;
            var times = new ArrayList<Long>();
            for (var future : futures) times.add(future.get());
            elapsed = System.nanoTime()-start;
            executor.shutdown();
            Collections.sort(times);
            System.out.printf(Locale.ROOT, "%s\t%.3f\t%.3f\t%.2f%n", mode, times.get(199)/1e6, times.get(379)/1e6, 400e9/elapsed);
        }
    }
    private static void request(String mode, int n, List<Long> users, ExperimentData data, RecommendationEngine engine,
                                BoundedTtlCache<String, RecommendationEngine.Snapshot> cache) {
        long id=users.get(n % users.size());
        var snapshot = mode.equals("baseline") ? engine.prepare(data.resources(), data.history())
                : cache.get("all", () -> engine.prepare(data.resources(), data.history()));
        engine.rank(snapshot,id,data.queries().get(id),data.lookup(),RecommendationEngine.Weights.DEFAULT,10);
    }
}
