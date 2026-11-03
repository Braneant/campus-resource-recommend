package com.campus.recommend.experiment;

import com.campus.recommend.recommendation.RecommendationEngine;
import com.campus.recommend.recommendation.RecommendationEngine.*;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class EvaluationCli {
    public static void main(String[] args) throws Exception {
        ExperimentData data = ExperimentData.load(Path.of(args[0]));
        int k = Integer.parseInt(args[1]);
        RecommendationEngine engine = new RecommendationEngine();
        Snapshot snapshot = engine.prepare(data.resources(), data.history());
        Map<String, Weights> models = new LinkedHashMap<>();
        models.put("Hot", new Weights(0, 0, 1));
        models.put("ItemCF", new Weights(1, 0, 0));
        models.put("KG", new Weights(0, 1, 0));
        models.put("Hybrid", Weights.DEFAULT);
        System.out.println("model\tuser\tranked_resources");
        for (var model : models.entrySet()) for (var query : data.queries().entrySet()) {
            String ranking = engine.rank(snapshot, query.getKey(), query.getValue(), data.lookup(), model.getValue(), k)
                    .stream().map(r -> Long.toString(r.resourceId())).collect(Collectors.joining(","));
            System.out.println(model.getKey() + "\t" + query.getKey() + "\t" + ranking);
        }
    }
}
