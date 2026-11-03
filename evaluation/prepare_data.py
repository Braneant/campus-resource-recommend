"""Export only public seed metadata and train-only feedback. Never connects to a DB.

This is a random user-item pair holdout, not a chronological production experiment.
All modalities of a pair are held out together. Cold users are selected by user ID
before splitting; their profile-based interest is independent of all interactions.
"""
from __future__ import annotations

from collections import defaultdict
import csv
import hashlib
import io
import json
from pathlib import Path
import random
import re

ROOT = Path(__file__).resolve().parents[1]
FIXTURES = ROOT / "evaluation/fixtures"
SEED = 20261102


def sql_rows(path: Path, table: str) -> list[dict[str, str]]:
    columns = None
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        insert = re.match(r"INSERT INTO (\w+) \((.*?)\) VALUES", line)
        if insert:
            columns = [c.strip() for c in insert.group(2).split(",")] if insert.group(1) == table else None
        elif columns and line.startswith("("):
            # The versioned seed format has one complete SQL tuple per line.
            value = line.rstrip().removesuffix(",").removesuffix(";")[1:-1]
            fields = next(csv.reader(io.StringIO(value), quotechar="'", escapechar="\\", skipinitialspace=True))
            if len(fields) != len(columns):
                raise ValueError(f"Malformed {table} tuple: expected {len(columns)} fields, got {len(fields)}")
            rows.append(dict(zip(columns, fields)))
    return rows


def split_pairs(pairs: dict[tuple[int, int], float], seed: int):
    users = sorted({u for u, _ in pairs})
    rng = random.Random(seed)
    cold = set(rng.sample(users, max(1, len(users) // 5)))
    train, test = {}, {}
    for user in users:
        ids = sorted(r for u, r in pairs if u == user)
        rng.shuffle(ids)
        heldout = set(ids if user in cold else ids[:max(1, len(ids) // 5)])
        for resource in ids:
            (test if resource in heldout else train)[user, resource] = pairs[user, resource]
    return train, test, cold


def choose_interests(users, catalog, cold):
    interests = {}
    for user in sorted(cold):
        profile = users[user]
        approved = [r for r in catalog if r["status"] == "1" and r["primary_kp_id"] != "NULL"]
        major = [r for r in approved if r["major_id"] == profile["major_id"]]
        college = [r for r in approved if r["college_id"] == profile["college_id"]]
        options = major or college or approved
        interests[user] = min(int(r["primary_kp_id"]) for r in options)
    return interests


def training_popularity(train):
    totals = defaultdict(float)
    for (_, resource), weight in sorted(train.items()):
        totals[resource] += weight
    maximum = max(totals.values(), default=1)
    return {resource: value / maximum for resource, value in totals.items()}


def semantic_distances(catalog, cypher):
    # Same bounded topology as GraphService: Resource->KP, undirected 1..3 edges
    # between KPs, then KP<-Resource. Edge properties/heat are never imported.
    adjacency = defaultdict(list)
    edges = set()
    for a_type, a_id, b_type, b_id, relation in re.findall(
            r"MATCH \(\w+:(\w+) \{id:(\d+)\}\), \(\w+:(\w+) \{id:(\d+)\}\) MERGE \(\w+\)-\[:(\w+)\]->\(\w+\);", cypher):
        a, b = (a_type, int(a_id)), (b_type, int(b_id))
        edge = (a, b, relation)
        if edge not in edges:
            edges.add(edge)
            adjacency[a].append((b, edge))
            adjacency[b].append((a, edge))
    kp_distances = {}
    for kp in sorted({int(r["primary_kp_id"]) for r in catalog if r["primary_kp_id"] != "NULL"}):
        start = ("KnowledgePoint", kp)
        distance = {}

        def trails(node, used_edges, depth):
            if depth >= 3:
                return
            for neighbor, edge in adjacency[node]:
                if edge in used_edges:
                    continue
                hops = depth + 1
                if neighbor[0] == "KnowledgePoint":
                    distance[neighbor[1]] = min(distance.get(neighbor[1], 99), hops + 2)
                trails(neighbor, used_edges | {edge}, hops)

        # Cypher permits repeated nodes, but a path cannot reuse a relationship.
        # Thus triangle paths can connect two resources attached to the same KP.
        trails(start, set(), 0)
        kp_distances[kp] = distance
    rows = []
    for source in catalog:
        if source["primary_kp_id"] == "NULL":
            continue
        distances = kp_distances[int(source["primary_kp_id"])]
        for target in catalog:
            if target["status"] == "1" and int(target["id"]) != int(source["id"]):
                hops = distances.get(int(target["primary_kp_id"]))
                if hops:
                    rows.append((int(source["id"]), int(target["id"]), hops))
    return sorted(rows)


def write_tsv(name, header, rows):
    FIXTURES.mkdir(parents=True, exist_ok=True)
    with (FIXTURES / name).open("w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f, delimiter="\t", lineterminator="\n")
        writer.writerow(header)
        writer.writerows(rows)


def export():
    catalog = sql_rows(ROOT / "data/04_resource.sql", "learning_resource")
    users = {int(u["id"]): u for u in sql_rows(ROOT / "data/05_user.sql", "student_user")}
    approved = {int(r["id"]) for r in catalog if r["status"] == "1"}
    pairs = defaultdict(float)
    downloads = []
    modality_counts = {}
    for table, weight in [("favorite_behavior", 1.0), ("download_behavior", 0.8), ("rating_behavior", None)]:
        rows = sql_rows(ROOT / "data/06_behavior.sql", table)
        modality_counts[table] = len(rows)
        for row in rows:
            user, resource = int(row["student_id"]), int(row["resource_id"])
            if resource not in approved:
                continue
            score = weight if weight is not None else int(row["score"]) / 5
            pairs[user, resource] += score
            if table == "download_behavior":
                downloads.append((user, resource))
    pairs = {key: min(1.0, value) for key, value in pairs.items() if value > 0}
    train, test, cold = split_pairs(pairs, SEED)
    assert not set(train) & set(test)
    assert not cold & {u for u, _ in train}
    heat = training_popularity(train)
    download_counts = defaultdict(int)
    for pair in downloads:
        if pair in train:
            download_counts[pair[1]] += 1
    interests = choose_interests(users, catalog, cold)
    write_tsv("resources.tsv", ["id", "kp", "train_heat", "train_downloads", "approved"],
              [(r["id"], r["primary_kp_id"], format(heat.get(int(r["id"]), 0), ".17g"), download_counts[int(r["id"])], int(r["status"] == "1")) for r in catalog])
    write_tsv("train.tsv", ["user", "resource", "weight"], [(u, r, format(w, ".17g")) for (u, r), w in sorted(train.items())])
    write_tsv("test.tsv", ["user", "resource", "group"], [(u, r, "cold" if u in cold else "warm") for u, r in sorted(test)])
    write_tsv("queries.tsv", ["user", "interest_kp"], [(u, interests.get(u, "")) for u in sorted({u for u, _ in test})])
    write_tsv("graph.tsv", ["source", "target", "hops"], semantic_distances(catalog, (ROOT / "data/neo4j_init.cypher").read_text(encoding="utf-8")))
    sources = ["data/04_resource.sql", "data/05_user.sql", "data/06_behavior.sql", "data/neo4j_init.cypher"]
    manifest = dict(seed=SEED, split="per-user random pair holdout: cold 20% users, warm 20% pairs",
                    catalog=len(approved), users=len({u for u, _ in test}), cold_users=sorted(cold),
                    train_pairs=len(train), test_pairs=len(test), input_modalities=modality_counts,
                    train_heat="sum capped implicit feedback / maximum, training pairs only",
                    interest_policy="smallest approved KP in profile major, else college, else catalog; no behavior/labels",
                    source_sha256={name: hashlib.sha256((ROOT / name).read_bytes()).hexdigest() for name in sources})
    manifest["fixture_sha256"] = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(FIXTURES.glob("*.tsv"))}
    (FIXTURES / "manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return manifest


if __name__ == "__main__":
    print(json.dumps(export(), indent=2, ensure_ascii=False))
