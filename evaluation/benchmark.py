"""Run real product-engine benchmark on the fixed offline fixture."""
import json
import os
import platform
import subprocess
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from run_java import ROOT, compile_cli, run_cli, java_tool

here = Path(__file__).resolve().parent
compile_cli(here/'src/ExperimentData.java', here/'src/BenchmarkCli.java')
rows = run_cli('com.campus.recommend.experiment.BenchmarkCli', here/'fixtures').strip().splitlines()
metrics = {}
for line in rows:
    name, p50, p95, throughput = line.split('\t')
    metrics[name] = {'p50_ms': float(p50), 'p95_ms': float(p95), 'requests_per_second': float(throughput)}
result = {'scope': 'in-process Java engine, warm cache, no HTTP/MySQL/Neo4j',
          'fixture': '192 approved resources, 20 test users, 342 training pairs',
          'concurrency': 4, 'warmup_per_mode': 100, 'requests_per_mode': 400,
          'platform': platform.platform(), 'cpu': platform.processor(), 'logical_cpus': os.cpu_count(),
          'java': subprocess.run([java_tool('java'), '-version'], capture_output=True, text=True).stderr.splitlines()[0],
          'metrics': metrics,
          'limitations': 'One sequential pair of runs; no confidence interval, GC isolation, cold-cache, SQL, graph or network measurement.'}
(here/'benchmark-results.json').write_text(json.dumps(result, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
print(json.dumps(result, ensure_ascii=False, indent=2))
