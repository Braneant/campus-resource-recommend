"""Compile and execute the product engine, then score its rankings against separate labels."""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import math
import platform
from pathlib import Path
import sys
from collections import defaultdict

# Also support embedded/isolated Python installations that omit the script directory.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from prepare_data import export, FIXTURES
from run_java import ROOT, ENGINE, compile_cli, run_cli


def metrics(rankings, labels, users, k, catalog):
    recalls, ndcgs, counts = [], [], []
    covered = set()
    for user in users:
        ranking = rankings.get(user, [])[:k]
        truth = labels[user]
        recalls.append(len(set(ranking) & truth) / len(truth))
        dcg = sum(1 / math.log2(i + 2) for i, item in enumerate(ranking) if item in truth)
        ideal = sum(1 / math.log2(i + 2) for i in range(min(k, len(truth))))
        ndcgs.append(dcg / ideal)
        covered.update(ranking)
        counts.append(len(ranking))
    return dict(users=len(users), recall_at_k=sum(recalls) / len(users), ndcg_at_k=sum(ndcgs) / len(users),
                catalog_coverage=len(covered) / catalog, recommended_catalog_items=len(covered),
                mean_list_length=sum(counts) / len(users))


def evaluate(k=10):
    manifest = export()
    compile_cli(ROOT / "evaluation/src/ExperimentData.java", ROOT / "evaluation/src/EvaluationCli.java")
    raw = run_cli("com.campus.recommend.experiment.EvaluationCli", FIXTURES, k)
    (ROOT / "evaluation/rankings.tsv").write_text(raw.replace("\r\n", "\n"), encoding="utf-8")
    labels, groups, training = defaultdict(set), defaultdict(set), defaultdict(set)
    for row in csv.DictReader((FIXTURES / "test.tsv").open(encoding="utf-8"), delimiter="\t"):
        user = int(row["user"])
        labels[user].add(int(row["resource"]))
        groups[row["group"]].add(user)
    for row in csv.DictReader((FIXTURES / "train.tsv").open(encoding="utf-8"), delimiter="\t"):
        training[int(row["user"])].add(int(row["resource"]))
    approved = {int(r["id"]) for r in csv.DictReader((FIXTURES / "resources.tsv").open(encoding="utf-8"), delimiter="\t") if r["approved"] == "1"}
    models = defaultdict(dict)
    for row in csv.DictReader(io.StringIO(raw), delimiter="\t"):
        user = int(row["user"])
        ranked = [int(item) for item in row["ranked_resources"].split(",") if item]
        assert len(ranked) == len(set(ranked)) and len(ranked) <= k
        assert set(ranked) <= approved - training[user], "candidate/seen-item protocol violation"
        models[row["model"]][user] = ranked
    results = {}
    for name, rankings in models.items():
        assert set(rankings) == set(labels), "all methods must evaluate the same users"
        results[name] = {group: metrics(rankings, labels, sorted(users), k, manifest["catalog"])
                         for group, users in {"all": set(labels), **groups}.items()}
    report = dict(k=k, seed=manifest["seed"], protocol=manifest, metrics=results,
                  engine_sha256={p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(ENGINE.glob("*.java"))},
                  python=platform.python_version(),
                  limitations=["Synthetic versioned seed data: 20 users, 192 approved items; not evidence of real learning outcomes.",
                               "One fixed random pair split; no temporal validation or significance claim.",
                               "Cold interest is a fixed profile/catalog proxy, not a surveyed preference.",
                               "ItemCF/KG may return fewer than K items without signal; no hidden popularity padding.",
                               "Neo4j topology is exported from seed Cypher; no live database or graph-query latency is measured."])
    (ROOT / "evaluation/results.json").write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(results, indent=2))
    return report


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--k", type=int, default=10)
    arguments = parser.parse_args()
    if arguments.k < 1:
        parser.error("K must be positive")
    evaluate(arguments.k)
