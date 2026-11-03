package com.campus.recommend.experiment;

import com.campus.recommend.recommendation.RecommendationEngine;
import com.campus.recommend.recommendation.RecommendationEngine.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Public non-sensitive TSV inputs only. This loader never reads held-out labels. */
public record ExperimentData(List<Resource> resources, Map<Long, Map<Long, Double>> history,
                             Map<Long, List<Long>> queries, Map<Long, List<Related>> graph) {
    public static ExperimentData load(Path directory) throws IOException {
        List<Resource> resources = new ArrayList<>();
        for (String[] row : rows(directory.resolve("resources.tsv")))
            resources.add(new Resource(Long.parseLong(row[0]), "NULL".equals(row[1]) ? null : Long.valueOf(row[1]),
                    Double.parseDouble(row[2]), Integer.parseInt(row[3]), "1".equals(row[4])));
        Map<Long, Map<Long, Double>> history = new TreeMap<>();
        for (String[] row : rows(directory.resolve("train.tsv")))
            history.computeIfAbsent(Long.valueOf(row[0]), k -> new TreeMap<>()).put(Long.valueOf(row[1]), Double.valueOf(row[2]));
        Map<Long, List<Long>> queries = new TreeMap<>();
        for (String[] row : rows(directory.resolve("queries.tsv")))
            queries.put(Long.valueOf(row[0]), row[1].isBlank() ? List.of() : List.of(Long.valueOf(row[1])));
        Map<Long, Resource> byId = new HashMap<>();
        resources.forEach(r -> byId.put(r.id(), r));
        Map<Long, List<Related>> graph = new TreeMap<>();
        for (String[] row : rows(directory.resolve("graph.tsv")))
            graph.computeIfAbsent(Long.valueOf(row[0]), k -> new ArrayList<>()).add(new Related(Long.parseLong(row[1]), Integer.parseInt(row[2])));
        graph.values().forEach(list -> list.sort(Comparator.comparingInt(Related::hops)
                .thenComparing(Comparator.comparingDouble((Related r) -> byId.get(r.resourceId()).heat()).reversed())
                .thenComparingLong(Related::resourceId)));
        return new ExperimentData(List.copyOf(resources), history, queries, graph);
    }

    public SemanticLookup lookup() {
        return (id, limit) -> {
            List<Related> rows = graph.getOrDefault(id, List.of());
            return rows.subList(0, Math.min(limit, rows.size()));
        };
    }

    private static List<String[]> rows(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream().skip(1).filter(s -> !s.isBlank())
                .map(s -> s.split("\t", -1)).toList();
    }
}
