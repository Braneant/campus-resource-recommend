"""Windows launcher with isolated ports and verified process ownership."""
import errno
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import urllib.parse
import urllib.request

try:
    import msvcrt
except ImportError:  # Pure safety checks and /check also run on Linux CI.
    msvcrt = None

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / '.startup-safe'
IDENTITY = hashlib.sha256(str(ROOT).lower().encode()).hexdigest()[:20]
STATE = HOME / 'state.json'
FLAGS = getattr(subprocess, 'CREATE_NO_WINDOW', 0)


def request(port, path):
    with urllib.request.urlopen(f'http://127.0.0.1:{port}{path}', timeout=3) as response:
        return json.load(response)


def port_busy(port):
    # Test both address families, including wildcard listeners.
    for family, address in [(socket.AF_INET, '0.0.0.0'), (socket.AF_INET6, '::')]:
        try:
            with socket.socket(family) as sock:
                if hasattr(socket, 'SO_EXCLUSIVEADDRUSE'):
                    sock.setsockopt(socket.SOL_SOCKET, socket.SO_EXCLUSIVEADDRUSE, 1)
                sock.bind((address, port))
        except OSError as exc:
            if exc.errno in (errno.EAFNOSUPPORT, errno.EPROTONOSUPPORT, errno.EADDRNOTAVAIL):
                continue
            return True
    return False


def choose_port(first):
    if not isinstance(first, int) or not 1 <= first <= 65535:
        raise ValueError('Port must be between 1 and 65535')
    last = min(first + 199, 65535)
    for port in range(first, last + 1):
        if not port_busy(port):
            return port
    raise RuntimeError(f'No available port in {first}..{last}')


def process_matches(pid, marker):
    # PID alone is insufficient: Windows can recycle it after process exit.
    if type(pid) is not int or pid <= 0:
        return False
    result = subprocess.run(['powershell.exe', '-NoProfile', '-Command',
        f'$ErrorActionPreference="Stop"; (Get-CimInstance Win32_Process -Filter "ProcessId={int(pid)}").CommandLine'],
        capture_output=True, creationflags=FLAGS, timeout=10)
    if result.returncode:
        raise RuntimeError('Process identity inspection denied; run launcher in a normal desktop session')
    # Match a complete command argument, never a substring of another owner.
    command = result.stdout.decode('utf-8', errors='replace')
    try:
        arguments = shlex.split(command, posix=False)
    except ValueError:
        return False
    return marker in [argument[1:-1] if argument.startswith('"') and argument.endswith('"')
                      else argument for argument in arguments]


def read_state():
    if not STATE.exists():
        return {}
    try:
        state = json.loads(STATE.read_text(encoding='utf-8'))
        if not isinstance(state, dict):
            raise ValueError('State must be an object')
        for name in ('backend', 'frontend'):
            if name not in state:
                continue
            item = state[name]
            if (not isinstance(item, dict) or type(item.get('pid')) is not int or item['pid'] <= 0
                    or type(item.get('port')) is not int or not 1 <= item['port'] <= 65535):
                raise ValueError('Invalid process record')
        return state
    except (ValueError, OSError) as exc:
        raise RuntimeError('Invalid launcher state in .startup-safe/state.json; inspect or back up it before retrying') from exc


def write_state(state):
    temporary = STATE.with_suffix('.tmp')
    temporary.write_text(json.dumps(state), encoding='utf-8')
    temporary.replace(STATE)


def stop_owned(state):
    # Validate all records first; a stale/reused PID must never be stopped.
    owned = []
    for name in ('frontend', 'backend'):
        item = state.get(name)
        marker = IDENTITY if name == 'frontend' else f'-Dcampus.launcher={IDENTITY}'
        if item:
            if not process_matches(item['pid'], marker):
                raise RuntimeError(f"Refusing to stop {name} PID {item['pid']}: owner identity does not match")
            owned.append((name, item))
    for name, item in owned:
        subprocess.run(['taskkill.exe', '/PID', str(item['pid']), '/F'],
            check=True, capture_output=True, creationflags=FLAGS, timeout=15)
        state.pop(name)
        write_state(state)
        print(f"[OK] Stopped owned {name} PID {item['pid']}", flush=True)
    if not owned:
        print('[OK] No owned processes recorded; shared database services are unchanged', flush=True)


def build_artifacts():
    jar = ROOT / 'backend' / 'target' / 'campus-recommend-1.0.0.jar'
    if not jar.is_file():
        raise RuntimeError('Backend JAR missing; from the repository root run: mvn -f backend/pom.xml package')
    if not (ROOT / 'frontend/node_modules/vite/dist/node/index.js').is_file():
        raise RuntimeError('Frontend dependencies missing; run: cd frontend && npm ci && npm run build')
    return jar


def load_environment(required=True):
    source = ROOT / 'backend' / 'local-env.cmd'
    if source.exists() and os.name == 'nt':
        # Let CMD parse local configuration; do not print or persist secret values.
        result = subprocess.run(f'cmd.exe /d /s /c "call "{source}" >nul && set"',
            capture_output=True, creationflags=FLAGS, timeout=15)
        if result.returncode:
            raise RuntimeError('Unable to load backend/local-env.cmd')
        import locale
        for line in result.stdout.decode(locale.getpreferredencoding(False), errors='replace').splitlines():
            key, sep, value = line.partition('=')
            key = key.upper()
            if sep and (key in ('MYSQL_PASSWORD', 'JWT_SECRET', 'JAVA_HOME')
                        or key.startswith(('CAMPUS_', 'NEO4J_', 'SPRING_', 'SMTP_', 'SECURITY_'))):
                os.environ[key] = value
    if not required:
        return
    for key in ('MYSQL_PASSWORD', 'NEO4J_PASSWORD', 'JWT_SECRET'):
        value = os.environ.get(key, '').strip()
        if not value or (value.startswith('PUT_') and value.endswith('_HERE')):
            raise RuntimeError(f'Missing or placeholder {key}; configure the environment or backend/local-env.cmd (values not logged)')


def executable(name, variable, home_variable=None):
    configured = os.environ.get(variable)
    if configured:
        path = Path(configured).expanduser()
        if not path.is_file():
            raise RuntimeError(f'{variable} must point to an existing {name} executable; see docs/quickstart.md')
        return str(path)
    if home_variable and os.environ.get(home_variable):
        suffix = '.exe' if os.name == 'nt' else ''
        path = Path(os.environ[home_variable]) / 'bin' / f'{name}{suffix}'
        if not path.is_file():
            raise RuntimeError(f'{home_variable}/bin/{name} is missing; correct {home_variable} or set {variable}')
        return str(path)
    path = shutil.which(name)
    if not path:
        raise RuntimeError(f'{name} missing from PATH; set {variable}' + (f' or {home_variable}' if home_variable else ''))
    return path


def tool_version(path, argument, pattern, minimum, label):
    try:
        result = subprocess.run([path, argument], capture_output=True, creationflags=FLAGS, timeout=15)
    except (OSError, subprocess.TimeoutExpired) as exc:
        raise RuntimeError(f'Unable to run {label}; check its configured executable') from exc
    output = (result.stdout + result.stderr).decode('utf-8', errors='replace')
    version = re.search(pattern, output)
    if result.returncode or not version or int(version.group(1)) < minimum:
        raise RuntimeError(f'{label} {minimum} or newer is required; check its configured executable')
    print(f'[OK] {label} {version.group(1)}', flush=True)


def resolve_tools():
    java = executable('java', 'CAMPUS_JAVA', 'JAVA_HOME')
    node = executable('node', 'CAMPUS_NODE')
    tool_version(java, '-version', r'version\s+"(\d+)', 17, 'Java')
    tool_version(node, '--version', r'v(\d+)\.', 22, 'Node')
    return java, node


def database_targets():
    targets = []
    for name, variable, default, schemes, default_port, service in (
        ('MySQL', 'SPRING_DATASOURCE_URL', 'jdbc:mysql://localhost:3306/campus_recommend', ('mysql',), 3306, 'MySQL80'),
        ('Neo4j', 'NEO4J_URI', 'bolt://localhost:7687', ('bolt', 'bolt+s', 'bolt+ssc', 'neo4j', 'neo4j+s', 'neo4j+ssc'), 7687, 'neo4j'),
    ):
        value = os.environ.get(variable, default)
        if name == 'MySQL' and value.startswith('jdbc:'):
            value = value[5:]
        try:
            parsed = urllib.parse.urlsplit(value)
            if parsed.scheme not in schemes or not parsed.hostname:
                raise ValueError('Invalid database URI')
            host, port = parsed.hostname, parsed.port or default_port
            if not 1 <= port <= 65535:
                raise ValueError('Invalid database port')
        except ValueError as exc:
            raise RuntimeError(f'Invalid {variable}; see docs/quickstart.md (values not logged)') from exc
        targets.append((name, host, port, os.environ.get(f'CAMPUS_{name.upper()}_SERVICE', service)))
    return targets


def listener_available(host, port):
    try:
        with socket.create_connection((host, port), timeout=2):
            return True
    except OSError:
        return False


def start_neo4j():
    home = os.environ.get('NEO4J_HOME')
    if home:
        neo = Path(home) / 'bin' / 'neo4j.bat'
        if not neo.is_file():
            raise RuntimeError('NEO4J_HOME/bin/neo4j.bat is missing; correct NEO4J_HOME or start Neo4j yourself')
    else:
        # Compatibility for the original desktop installation. New clones use
        # NEO4J_HOME or an installed service, without depending on this location.
        neo = Path(r'D:\jaeger\neo4j\neo4j-community-2026.08.1\bin\neo4j.bat')
        if not neo.is_file():
            return
    env = dict(os.environ)
    graph_java = os.environ.get('NEO4J_JAVA_HOME')
    if not graph_java and neo == Path(r'D:\jaeger\neo4j\neo4j-community-2026.08.1\bin\neo4j.bat'):
        legacy_java = Path(r'D:\jaeger\jdk-21')
        if legacy_java.is_dir():
            graph_java = str(legacy_java)
    if graph_java:
        env['JAVA_HOME'] = graph_java
    subprocess.run(['cmd.exe', '/d', '/c', str(neo), 'start'], env=env,
        capture_output=True, creationflags=FLAGS, timeout=60)


def ensure_databases(targets, diagnostic=False):
    for name, host, port, service in targets:
        available = listener_available(host, port)
        local = host in ('localhost', '127.0.0.1', '::1')
        if not available and not diagnostic and local:
            if service:
                subprocess.run(['net.exe', 'start', service], capture_output=True, creationflags=FLAGS, timeout=30)
            if name == 'Neo4j' and not listener_available(host, port):
                start_neo4j()
            deadline = time.monotonic() + 60
            while not listener_available(host, port) and time.monotonic() < deadline:
                time.sleep(1)
            available = listener_available(host, port)
        if not available:
            raise RuntimeError(f'{name} unavailable at {host}:{port}; start the database or correct its environment configuration (docs/quickstart.md)')
        print(f'[OK] {name} listener: {host}:{port}', flush=True)


def spawn(args, name):
    with (HOME / f'{name}.log').open('ab', buffering=0) as log:
        return subprocess.Popen(args, cwd=ROOT, stdout=log, stderr=log,
            creationflags=FLAGS | getattr(subprocess, 'CREATE_NEW_PROCESS_GROUP', 0))


def wait_ready(port, path, process, check, name):
    deadline = time.monotonic() + 120
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError(f'{name} exited ({process.returncode}); see .startup-safe/{name}.log')
        try:
            if check(request(port, path)):
                return
        except Exception:
            pass
        time.sleep(1)
    raise RuntimeError(f'{name} readiness timed out; see .startup-safe/{name}.log')


def main():
    if '/check' in sys.argv:
        return launch()
    if msvcrt is None:
        raise RuntimeError('Starting, stopping and /diag require Windows; /check and safety tests work on other platforms')
    HOME.mkdir(exist_ok=True)
    with (HOME / 'launcher.lock').open('a+b') as lock:
        lock.seek(0)
        if not lock.read(1):
            lock.write(b'0')
            lock.flush()
        lock.seek(0)
        try:
            msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
        except OSError:
            raise RuntimeError('Another launcher is running; wait for it to finish')
        return launch()


def launch():
    if '/stop' in sys.argv:
        return stop_owned(read_state())
    diag = '/diag' in sys.argv
    load_environment(required=not diag)
    if not diag:
        jar = build_artifacts()
        java, node = resolve_tools()
    targets = database_targets()
    if not diag:
        if '/check' in sys.argv:
            for name, host, port, _ in targets:
                print(f'[OK] {name} target: {host}:{port}', flush=True)
            print('[OK] Build artifacts, tools and environment are present; database connections are checked by /diag or start', flush=True)
            return
    state = read_state()
    print('Campus resource platform - isolated launcher', flush=True)
    ensure_databases(targets, diagnostic=diag)
    if diag:
        for name in ('backend', 'frontend'):
            item = state.get(name)
            marker = IDENTITY if name == 'frontend' else f'-Dcampus.launcher={IDENTITY}'
            if not item or not process_matches(item['pid'], marker):
                raise RuntimeError(f'{name} not running as an owned process')
        be, fe = state['backend'], state['frontend']
        if request(fe['port'], '/__campus_launcher') != {'identity': IDENTITY, 'backend': be['port']}:
            raise RuntimeError('Frontend identity/proxy mismatch')
        if request(fe['port'], '/api/resource/search?pageSize=1').get('code') != 0:
            raise RuntimeError('Database API unavailable through frontend')
        print(f"[OK] Backend {be['port']}; frontend http://127.0.0.1:{fe['port']}; API verified")
        return
    marker = f'-Dcampus.launcher={IDENTITY}'
    be = state.get('backend', {})
    reusable = be and process_matches(be['pid'], marker)
    if reusable:
        try:
            reusable = request(be['port'], '/api/resource/search?pageSize=1').get('code') == 0
        except Exception:
            reusable = False
        if not reusable:
            raise RuntimeError('Owned backend is not healthy; inspect its log or stop it safely before retrying')
    if not reusable:
        # Spring Boot nested JAR loading can fail with non-ASCII paths on Windows.
        runtime = Path(os.environ.get('CAMPUS_RUNTIME_DIR', tempfile.gettempdir())) / f'campus-launch-{IDENTITY}'
        runtime.mkdir(parents=True, exist_ok=True)
        runjar = runtime / jar.name
        if not runjar.is_file() or hashlib.sha256(runjar.read_bytes()).digest() != hashlib.sha256(jar.read_bytes()).digest():
            shutil.copyfile(jar, runjar)
        port = choose_port(8080)
        proc = spawn([java, marker, '-jar', str(runjar), f'--server.port={port}'], 'backend')
        be = {'pid': proc.pid, 'port': port}
        state['backend'] = be
        write_state(state)
        wait_ready(port, '/api/resource/search?pageSize=1', proc, lambda data: data.get('code') == 0, 'backend')
    print(f"[OK] Backend: {be['port']}", flush=True)
    fe = state.get('frontend', {})
    reusable = fe and process_matches(fe['pid'], IDENTITY)
    if reusable:
        try:
            data = request(fe['port'], '/__campus_launcher')
            reusable = data == {'identity': IDENTITY, 'backend': be['port']}
        except Exception:
            reusable = False
        if not reusable:
            raise RuntimeError('Owned frontend is not healthy or points to another backend; stop it safely before retrying')
    if not reusable:
        port = choose_port(3000)
        proc = spawn([node, str(HOME / 'frontend.mjs'), str(port), str(be['port']), IDENTITY], 'frontend')
        fe = {'pid': proc.pid, 'port': port}
        state['frontend'] = fe
        write_state(state)
        wait_ready(port, '/__campus_launcher', proc,
            lambda data: data == {'identity': IDENTITY, 'backend': be['port']}, 'frontend')
    if request(fe['port'], '/api/resource/search?pageSize=1').get('code') != 0:
        raise RuntimeError('Frontend API proxy verification failed')
    print(f"[OK] Frontend and API proxy: http://127.0.0.1:{fe['port']}", flush=True)
    print('No other project processes were stopped. Logs: .startup-safe/', flush=True)


if __name__ == '__main__':
    try:
        main()
    except Exception as exc:
        print(f'[FAIL] {exc}', file=sys.stderr, flush=True)
        sys.exit(1)
