"""Benchmark a disposable local backend. Never use production credentials or data."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import json
import math
import os
import platform
import statistics
import time
import urllib.request
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
from local_http import local_opener, validate_local_base_url

opener = local_opener()


def call(base, path, token=None, data=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    request = urllib.request.Request(base + path, data=json.dumps(data).encode() if data else None, headers=headers)
    with opener.open(request, timeout=30) as response:
        result = json.load(response)
    if result.get('code') != 0:
        raise RuntimeError('API returned a nonzero status: ' + str(result.get('code')))
    return result['data']


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--base', required=True)
    parser.add_argument('--iterations', type=int, default=200)
    parser.add_argument('--concurrency', type=int, default=4)
    parser.add_argument('--warmup', type=int, default=20)
    parser.add_argument('--confirm-disposable', action='store_true', help='confirm this is a disposable isolated instance')
    args = parser.parse_args()
    if not args.confirm_disposable or args.iterations < 1 or args.concurrency < 1 or args.warmup < 0:
        raise SystemExit('Confirm a disposable instance and use positive request/concurrency counts')
    args.base = validate_local_base_url(args.base)
    token = call(args.base, '/api/auth/login', data={
        'username': os.environ['CAMPUS_BENCH_USER'], 'password': os.environ['CAMPUS_BENCH_PASSWORD']})['token']

    def sample(_):
        begin = time.perf_counter()
        call(args.base, '/api/recommend/personal?topN=10', token)
        return (time.perf_counter() - begin) * 1000

    for _ in range(args.warmup): sample(0)
    start = time.perf_counter()
    with ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        timings = sorted(pool.map(sample, range(args.iterations)))
    elapsed = time.perf_counter() - start
    print(json.dumps({'scope': 'HTTP request through application; application cache may avoid MySQL or Neo4j',
                      'platform': platform.platform(), 'logical_cpus': os.cpu_count(),
                      'requests': len(timings), 'concurrency': args.concurrency,
                      'warmup': args.warmup, 'p50_ms': statistics.median(timings),
                      'p95_ms': timings[math.ceil(.95 * len(timings)) - 1],
                      'throughput_per_s': len(timings) / elapsed}, ensure_ascii=False, indent=2))


if __name__ == '__main__': main()
