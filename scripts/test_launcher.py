"""Launcher safety tests: no real services are started or terminated."""
import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import socket
import subprocess
import sys
import unittest
import uuid
import shutil
from unittest.mock import patch


ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location('campus_launcher', ROOT / '.startup-safe' / 'launch.py')
launcher = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(launcher)


class PortSelectionTests(unittest.TestCase):
    def test_occupied_listener_is_preserved_and_another_port_is_selected(self):
        with socket.socket() as occupied:
            occupied.bind(('127.0.0.1', 0))
            occupied.listen()
            port = occupied.getsockname()[1]
            selected = launcher.choose_port(port)
            self.assertNotEqual(port, selected)
            self.assertFalse(launcher.port_busy(selected))
            # The listener remains usable; selection must never free a busy port.
            with socket.create_connection(('127.0.0.1', port), timeout=1):
                connection, _ = occupied.accept()
                connection.close()

    def test_exhausted_range_reports_failure(self):
        with patch.object(launcher, 'port_busy', return_value=True):
            with self.assertRaisesRegex(RuntimeError, 'No available port'):
                launcher.choose_port(3000)

    def test_invalid_port_is_rejected(self):
        for port in (0, -1, 65536):
            with self.subTest(port=port), patch.object(launcher, 'port_busy', return_value=False):
                with self.assertRaises(ValueError):
                    launcher.choose_port(port)


class IdentityTests(unittest.TestCase):
    def inspect(self, command, marker):
        result = subprocess.CompletedProcess([], 0, stdout=command.encode('utf-8'), stderr=b'')
        with patch.object(launcher.subprocess, 'run', return_value=result):
            return launcher.process_matches(12345, marker)

    def test_backend_exact_owner_argument_matches(self):
        self.assertTrue(self.inspect('java.exe -Dcampus.launcher=abc123 -jar app.jar', '-Dcampus.launcher=abc123'))

    def test_frontend_exact_owner_argument_matches(self):
        self.assertTrue(self.inspect('node.exe "D:\\project\\.startup-safe\\frontend.mjs" 3000 8080 abc123', 'abc123'))

    def test_reused_pid_with_different_owner_is_rejected(self):
        self.assertFalse(self.inspect('java.exe -Dcampus.launcher=other -jar app.jar', '-Dcampus.launcher=abc123'))

    def test_marker_substring_is_rejected(self):
        for command in (
            'java.exe -Dcampus.launcher=abc123-foreign -jar app.jar',
            'node.exe frontend.mjs 3000 8080 prefixabc123',
            'node.exe frontend.mjs 3000 8080 abc123suffix',
            'node.exe --description "foreign frontend abc123 server"',
        ):
            marker = '-Dcampus.launcher=abc123' if command.startswith('java') else 'abc123'
            with self.subTest(command=command):
                self.assertFalse(self.inspect(command, marker))

    def test_denied_identity_inspection_fails_closed(self):
        result = subprocess.CompletedProcess([], 1, stdout=b'', stderr=b'access denied')
        with patch.object(launcher.subprocess, 'run', return_value=result):
            with self.assertRaisesRegex(RuntimeError, 'identity inspection'):
                launcher.process_matches(12345, 'abc123')

    def test_invalid_pid_is_not_sent_to_the_process_inspector(self):
        with patch.object(launcher.subprocess, 'run', side_effect=AssertionError('Invalid PID inspected')):
            for pid in (0, -1, '12345', True):
                with self.subTest(pid=pid):
                    self.assertFalse(launcher.process_matches(pid, 'abc123'))


class LauncherSafetyTests(unittest.TestCase):
    def setUp(self):
        # Default directory permissions also work in Windows app sandboxes;
        # mkdtemp's private ACL can prevent the restricted token reopening it.
        self.root = ROOT / '.startup-safe' / f'test-{uuid.uuid4().hex}'
        self.root.mkdir()
        def cleanup():
            if self.root.resolve().parent != (ROOT / '.startup-safe').resolve():
                raise AssertionError('Test cleanup escaped the launcher directory')
            shutil.rmtree(self.root)
        self.addCleanup(cleanup)
        self.home = self.root / '.startup-safe'
        self.home.mkdir()
        self.state = self.home / 'state.json'
        for name, value in (('ROOT', self.root), ('HOME', self.home), ('STATE', self.state)):
            mock = patch.object(launcher, name, value)
            mock.start()
            self.addCleanup(mock.stop)
        mock = patch.dict(os.environ, {
            'MYSQL_PASSWORD': 'fixture-mysql-password',
            'NEO4J_PASSWORD': 'fixture-neo4j-password',
            'JWT_SECRET': 'fixture-key-with-enough-characters-for-local-tests',
        }, clear=True)
        mock.start()
        self.addCleanup(mock.stop)

    def run_launcher(self, *args):
        output = io.StringIO()
        with patch.object(sys, 'argv', ['launch.py', *args]), contextlib.redirect_stdout(output):
            launcher.launch()
        return output.getvalue()

    def build_fixtures(self):
        for path in ('backend/target/campus-recommend-1.0.0.jar', 'frontend/node_modules/vite/dist/node/index.js'):
            target = self.root / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(b'fixture; never executed')
        self.java = self.root / 'custom tools' / 'java.exe'
        self.node = self.root / 'custom tools' / 'node.exe'
        self.java.parent.mkdir()
        self.java.touch()
        self.node.touch()
        os.environ['CAMPUS_JAVA'] = str(self.java)
        os.environ['CAMPUS_NODE'] = str(self.node)

    def version_runner(self, java='17.0.12', node='22.22.2'):
        def run(command, **kwargs):
            if command == [str(self.java), '-version']:
                return subprocess.CompletedProcess(command, 0, stdout=b'', stderr=f'openjdk version "{java}"'.encode())
            if command == [str(self.node), '--version']:
                return subprocess.CompletedProcess(command, 0, stdout=f'v{node}'.encode(), stderr=b'')
            self.fail(f'/check must only inspect tool versions: {command}')
        return run

    def test_stop_refuses_foreign_process_without_terminating_it(self):
        self.state.write_text(json.dumps({'frontend': {'pid': 12345, 'port': 3000}}), encoding='utf-8')

        def run(command, **kwargs):
            if Path(command[0]).name.lower() == 'powershell.exe':
                return subprocess.CompletedProcess(command, 0, stdout=b'node.exe foreign-vite.mjs', stderr=b'')
            self.fail(f'Foreign process must not be terminated: {command}')

        with patch.object(launcher.subprocess, 'run', side_effect=run):
            with self.assertRaisesRegex(RuntimeError, 'Refusing'):
                self.run_launcher('/stop')
        self.assertEqual(json.loads(self.state.read_text())['frontend']['pid'], 12345)

    def test_stop_with_no_state_leaves_other_processes_alone(self):
        with patch.object(launcher.subprocess, 'run', side_effect=AssertionError('Unexpected process command')):
            self.run_launcher('/stop')

    def test_verified_stop_removes_only_the_recorded_owner(self):
        self.state.write_text(json.dumps({'frontend': {'pid': 12345, 'port': 3000}}), encoding='utf-8')
        terminated = []

        def run(command, **kwargs):
            if Path(command[0]).name.lower() == 'powershell.exe':
                return subprocess.CompletedProcess(command, 0, stdout=f'node.exe frontend.mjs 3000 8080 {launcher.IDENTITY}'.encode(), stderr=b'')
            self.assertEqual(command, ['taskkill.exe', '/PID', '12345', '/F'])
            terminated.append(12345)
            return subprocess.CompletedProcess(command, 0, stdout=b'', stderr=b'')

        with patch.object(launcher.subprocess, 'run', side_effect=run):
            self.run_launcher('/stop')
        self.assertEqual(terminated, [12345])
        self.assertEqual(json.loads(self.state.read_text()), {})

    def test_corrupt_state_reports_a_recovery_message(self):
        self.state.write_text('{broken', encoding='utf-8')
        with self.assertRaisesRegex(RuntimeError, 'Invalid launcher state'):
            self.run_launcher('/stop')

    def test_missing_jar_reports_reproducible_build_command(self):
        with patch.object(launcher, 'port_busy', return_value=True):
            with self.assertRaisesRegex(RuntimeError, r'mvn -f backend/pom.xml.*package'):
                self.run_launcher('/check')

    def test_missing_frontend_dependencies_reports_npm_ci(self):
        jar = self.root / 'backend' / 'target' / 'campus-recommend-1.0.0.jar'
        jar.parent.mkdir(parents=True)
        jar.write_bytes(b'fixture jar, never executed')
        with patch.object(launcher, 'port_busy', return_value=True):
            with self.assertRaisesRegex(RuntimeError, 'npm ci'):
                self.run_launcher('/check')

    def test_check_uses_configured_tools_without_starting_services_or_writing_state(self):
        self.build_fixtures()
        os.environ['JAVA_HOME'] = str(self.root / 'wrong-java-home')
        with patch.object(launcher.subprocess, 'run', side_effect=self.version_runner()):
            output = self.run_launcher('/check')
        self.assertIn('Java 17', output)
        self.assertIn('Node 22', output)
        self.assertFalse(self.state.exists())

    def test_invalid_explicit_java_path_fails_instead_of_using_a_different_jdk(self):
        self.build_fixtures()
        os.environ['CAMPUS_JAVA'] = str(self.root / 'missing-java.exe')
        with self.assertRaisesRegex(RuntimeError, 'CAMPUS_JAVA'):
            self.run_launcher('/check')

    def test_old_java_reports_minimum_version(self):
        self.build_fixtures()
        with patch.object(launcher.subprocess, 'run', side_effect=self.version_runner(java='1.8.0_392')):
            with self.assertRaisesRegex(RuntimeError, 'Java 17'):
                self.run_launcher('/check')

    def test_old_node_reports_supported_version(self):
        self.build_fixtures()
        with patch.object(launcher.subprocess, 'run', side_effect=self.version_runner(node='18.20.8')):
            with self.assertRaisesRegex(RuntimeError, 'Node 22'):
                self.run_launcher('/check')

    def test_check_does_not_print_credentials_embedded_in_database_urls(self):
        self.build_fixtures()
        os.environ['SPRING_DATASOURCE_URL'] = 'jdbc:mysql://db-user:hidden-db-password@db.example:3307/campus'
        os.environ['NEO4J_URI'] = 'bolt://graph-user:hidden-graph-password@graph.example:7688'
        with patch.object(launcher.subprocess, 'run', side_effect=self.version_runner()):
            output = self.run_launcher('/check')
        self.assertIn('db.example:3307', output)
        self.assertIn('graph.example:7688', output)
        self.assertNotIn('hidden-db-password', output)
        self.assertNotIn('hidden-graph-password', output)

    def test_check_rejects_malformed_database_urls(self):
        self.build_fixtures()
        os.environ['NEO4J_URI'] = 'invalid-hidden-password'
        with patch.object(launcher.subprocess, 'run', side_effect=self.version_runner()):
            with self.assertRaisesRegex(RuntimeError, 'NEO4J_URI'):
                self.run_launcher('/check')

    def test_placeholder_secret_is_rejected_without_printing_it(self):
        self.build_fixtures()
        os.environ['MYSQL_PASSWORD'] = 'PUT_MYSQL_PASSWORD_HERE'
        with self.assertRaisesRegex(RuntimeError, 'MYSQL_PASSWORD') as failure:
            self.run_launcher('/check')
        self.assertNotIn('PUT_MYSQL_PASSWORD_HERE', str(failure.exception))

    def test_diagnostic_uses_remote_database_target_without_starting_a_service(self):
        os.environ['SPRING_DATASOURCE_URL'] = 'jdbc:mysql://db.example:3307/campus'
        with patch.object(launcher, 'port_busy', return_value=False), \
                patch.object(launcher.socket, 'create_connection', side_effect=OSError('not available')), \
                patch.object(launcher.subprocess, 'run', side_effect=AssertionError('Diagnostic must not start services')):
            with self.assertRaisesRegex(RuntimeError, 'db.example:3307'):
                self.run_launcher('/diag')

    def test_neo4j_start_uses_configured_home_and_its_own_java(self):
        self.build_fixtures()
        neo = self.root / 'graph tools' / 'bin' / 'neo4j.bat'
        neo.parent.mkdir(parents=True)
        neo.touch()
        os.environ['NEO4J_HOME'] = str(neo.parent.parent)
        os.environ['NEO4J_JAVA_HOME'] = str(self.root / 'graph-jdk')
        os.environ['CAMPUS_NEO4J_SERVICE'] = 'CampusGraph'
        os.environ['CAMPUS_RUNTIME_DIR'] = str(self.root / 'runtime')
        neo_ready = False

        def connect(address, **kwargs):
            if address[1] == 7687 and not neo_ready:
                raise OSError('graph not started')
            return contextlib.nullcontext()

        def run(command, **kwargs):
            nonlocal neo_ready
            if command == ['net.exe', 'start', 'CampusGraph']:
                return subprocess.CompletedProcess(command, 1, stdout=b'no installed service', stderr=b'')
            if command == ['cmd.exe', '/d', '/c', str(neo), 'start']:
                self.assertEqual(kwargs['env']['JAVA_HOME'], str(self.root / 'graph-jdk'))
                neo_ready = True
                return subprocess.CompletedProcess(command, 0, stdout=b'', stderr=b'')
            return self.version_runner()(command, **kwargs)

        with patch.object(launcher, 'port_busy', side_effect=lambda port: port == 3306 or (port == 7687 and neo_ready)), \
                patch.object(launcher.socket, 'create_connection', side_effect=connect), \
                patch.object(launcher.subprocess, 'run', side_effect=run), \
                patch.object(launcher, 'spawn', side_effect=RuntimeError('Reached isolated application spawn')):
            with self.assertRaisesRegex(RuntimeError, 'Reached isolated application spawn'):
                self.run_launcher()
        self.assertTrue(neo_ready)

    @unittest.skipUnless(os.name == 'nt', 'Local CMD configuration is Windows-specific')
    def test_local_environment_supports_tool_and_optional_mail_configuration(self):
        source = self.root / 'backend' / 'local-env.cmd'
        source.parent.mkdir()
        source.write_text('@echo off\nset "CAMPUS_JAVA=C:\\custom tools\\java.exe"\nset "SMTP_HOST=smtp.example"\nset "SMTP_PASSWORD=fixture-mail-secret"\n', encoding='ascii')
        with contextlib.redirect_stdout(io.StringIO()) as output:
            launcher.load_environment()
        self.assertEqual(os.environ.get('CAMPUS_JAVA'), 'C:\\custom tools\\java.exe')
        self.assertEqual(os.environ.get('SMTP_HOST'), 'smtp.example')
        self.assertEqual(os.environ.get('SMTP_PASSWORD'), 'fixture-mail-secret')
        self.assertNotIn('fixture-mail-secret', output.getvalue())


if __name__ == '__main__':
    unittest.main()
