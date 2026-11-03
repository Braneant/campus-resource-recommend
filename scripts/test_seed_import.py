import unittest
from pathlib import Path


class SeedIntegrityTest(unittest.TestCase):
    def test_subject_seed_inserts_each_table_once(self):
        sql = (Path(__file__).resolve().parents[1] / 'data/02_college_major_course.sql').read_text(encoding='utf-8')
        for table in ('college', 'major', 'course', 'resource_category'):
            self.assertEqual(sql.count('INSERT INTO ' + table + ' ('), 1, table)
