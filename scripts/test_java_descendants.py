import importlib.util
import unittest
import subprocess
from unittest.mock import patch
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('campus_launcher_descendants', ROOT / '.startup-safe/launch.py')
launcher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(launcher)


class JavaDescendantTests(unittest.TestCase):
    def test_owned_java_child_is_stopped_before_parent(self):
        state = {'backend': {'pid': 101, 'port': 8080}}
        calls = []
        with patch.object(launcher, 'process_matches', return_value=True), \
             patch.object(launcher, 'owned_descendants', return_value=[102]), \
             patch.object(launcher, 'write_state'), \
             patch.object(launcher.subprocess, 'run', side_effect=lambda cmd, **kw: (calls.append(cmd), subprocess.CompletedProcess(cmd, 0))[1]):
            launcher.stop_owned(state)
        self.assertEqual([['taskkill.exe', '/PID', '102', '/F'], ['taskkill.exe', '/PID', '101', '/F']], calls)

    def test_foreign_descendant_aborts_without_killing_any_process(self):
        state = {'backend': {'pid': 101, 'port': 8080}}
        with patch.object(launcher, 'process_matches', side_effect=[True, False]), \
             patch.object(launcher, 'owned_descendants', return_value=[102]), \
             patch.object(launcher, 'is_console_host', return_value=False), \
             patch.object(launcher.subprocess, 'run') as run:
            with self.assertRaisesRegex(RuntimeError, 'identity'):
                launcher.stop_owned(state)
            run.assert_not_called()

    def test_parent_already_exited_after_child_is_not_a_stop_failure(self):
        state = {'backend': {'pid': 101, 'port': 8080}}
        def terminate(command, **kwargs):
            return subprocess.CompletedProcess(command, 1 if command[2] == '101' else 0)
        with patch.object(launcher, 'process_matches', return_value=True), \
             patch.object(launcher, 'owned_descendants', return_value=[102]), \
             patch.object(launcher, 'process_exists', return_value=False), \
             patch.object(launcher, 'write_state'), \
             patch.object(launcher.subprocess, 'run', side_effect=terminate):
            launcher.stop_owned(state)
        self.assertEqual({}, state)

    def test_windows_console_host_is_left_alone(self):
        state = {'backend': {'pid': 101, 'port': 8080}}
        calls = []
        with patch.object(launcher, 'process_matches', side_effect=[True, True, False]), \
             patch.object(launcher, 'owned_descendants', return_value=[102, 103]), \
             patch.object(launcher, 'is_console_host', return_value=True), \
             patch.object(launcher, 'write_state'), \
             patch.object(launcher.subprocess, 'run', side_effect=lambda cmd, **kw: (calls.append(cmd), subprocess.CompletedProcess(cmd, 0))[1]):
            launcher.stop_owned(state)
        self.assertEqual([['taskkill.exe', '/PID', '102', '/F'], ['taskkill.exe', '/PID', '101', '/F']], calls)


if __name__ == '__main__':
    unittest.main()
