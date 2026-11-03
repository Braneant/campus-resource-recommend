import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


class SeedDataTests(unittest.TestCase):
    def test_seed_primary_keys_are_unique_across_insert_statements(self):
        sql = (ROOT / 'data/02_college_major_course.sql').read_text(encoding='utf-8')
        for table in ('college', 'major', 'course', 'resource_category'):
            statements = re.findall(rf'INSERT\s+INTO\s+{table}\s*\([^;]+?VALUES\s*(.*?);', sql, re.I | re.S)
            self.assertTrue(statements, f'No INSERT for {table}')
            ids = [int(value) for statement in statements for value in re.findall(r'^\s*\((\d+)\s*,', statement, re.M)]
            self.assertEqual(len(ids), len(set(ids)), f'Duplicate {table} primary keys')

    def test_generator_writes_each_subject_insert_once(self):
        source = (ROOT / 'data/gen_write.py').read_text(encoding='utf-8')
        for table in ('college', 'major'):
            self.assertEqual(1, len(re.findall(rf'INSERT INTO {table}\s*\(', source)), table)


if __name__ == '__main__':
    unittest.main()
