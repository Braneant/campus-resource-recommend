"""Leakage checks for the exported offline protocol (no database access)."""
import unittest

from prepare_data import split_pairs, choose_interests, training_popularity, semantic_distances


class ProtocolTest(unittest.TestCase):
    def test_pair_modalities_stay_together_and_cold_users_have_no_training_rows(self):
        pairs = {(u, r): 1.0 for u in range(1, 11) for r in range(1, 11)}
        train, test, cold = split_pairs(pairs, 20261102)
        self.assertFalse(set(train) & set(test))
        self.assertEqual(set(pairs), set(train) | set(test))
        self.assertTrue(cold)
        self.assertFalse({u for u, _ in train} & cold)
        self.assertEqual({u for u, _ in pairs}, {u for u, _ in test})

    def test_interests_use_only_profile_and_catalog_even_if_holdout_changes(self):
        users = {9: {"major_id": "2", "college_id": "1"}}
        catalog = [dict(id="4", primary_kp_id="1002", major_id="2", college_id="1", status="1"),
                   dict(id="5", primary_kp_id="1001", major_id="2", college_id="1", status="1")]
        self.assertEqual({9: 1001}, choose_interests(users, catalog, {9}))
        # There is deliberately no holdout/interaction argument in this function.
        self.assertEqual({9: 1001}, choose_interests(users, list(reversed(catalog)), {9}))

    def test_popularity_excludes_heldout_feedback(self):
        popularity = training_popularity({(1, 1): 1.0, (2, 1): 0.8, (2, 2): 0.4})
        self.assertEqual({1: 1.0, 2: 0.4 / 1.8}, popularity)
        self.assertNotIn(3, popularity)

    def test_semantic_export_keeps_cypher_relationship_unique_triangle_paths(self):
        catalog = [dict(id="1", primary_kp_id="10", status="1"),
                   dict(id="2", primary_kp_id="10", status="1"),
                   dict(id="3", primary_kp_id="20", status="1")]
        cypher = "\n".join([
            "MATCH (a:Course {id:1}), (b:KnowledgePoint {id:10}) MERGE (a)-[:TEACHES]->(b);",
            "MATCH (a:Course {id:1}), (b:KnowledgePoint {id:20}) MERGE (a)-[:TEACHES]->(b);",
            "MATCH (a:KnowledgePoint {id:10}), (b:KnowledgePoint {id:20}) MERGE (a)-[:RELATED_TO]->(b);"])
        rows = semantic_distances(catalog, cypher)
        self.assertIn((1, 2, 5), rows)  # KP10->Course1->KP20->KP10, plus two Resource edges.
        self.assertIn((1, 3, 3), rows)  # direct RELATED_TO plus two Resource edges.


if __name__ == "__main__":
    unittest.main()
