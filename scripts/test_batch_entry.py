import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


@unittest.skipUnless(os.name == 'nt', 'CMD entry points require Windows')
class BatchEntryTests(unittest.TestCase):
    def test_start_and_stop_resolve_their_path_after_argument_scanning(self):
        with tempfile.TemporaryDirectory(prefix='campus-batch-') as directory:
            root = Path(directory)
            helper = root / '.startup-safe'
            helper.mkdir()
            (helper / 'launch.py').write_text(
                'import json, pathlib, sys\n'
                'pathlib.Path(__file__).with_name("arguments.json").write_text(json.dumps(sys.argv[1:]))\n',
                encoding='utf-8')
            for name, expected in [('一键启动.bat', ['/nopause']), ('停止服务.bat', ['/stop', '/nopause'])]:
                with self.subTest(entry=name):
                    shutil.copyfile(ROOT / name, root / name)
                    result = subprocess.run(['cmd.exe', '/d', '/c', str(root / name), '/nopause'],
                                            capture_output=True, timeout=15)
                    self.assertEqual(0, result.returncode, result.stderr.decode(errors='replace'))
                    self.assertEqual(expected, json.loads((helper / 'arguments.json').read_text()))


if __name__ == '__main__':
    unittest.main()
