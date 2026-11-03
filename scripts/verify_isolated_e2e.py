"""Mutating smoke test; run only against a disposable loopback demo database."""
import json
import os
import sys
import urllib.request
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from local_http import local_opener, validate_local_base_url

base = validate_local_base_url(os.environ['CAMPUS_ISOLATED_URL'])
if os.environ.get('CAMPUS_ISOLATED_DISPOSABLE') != 'yes':
    raise SystemExit('Requires a disposable loopback deployment and CAMPUS_ISOLATED_DISPOSABLE=yes')
opener = local_opener()


def call(path, data=None, token=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    request = urllib.request.Request(base + path, data=json.dumps(data).encode() if data is not None else None, headers=headers)
    with opener.open(request, timeout=30) as response:
        result = json.load(response)
    if result.get('code') != 0:
        raise RuntimeError(f'{path} returned code={result.get("code")}: {result.get("message")}')
    return result.get('data')


student = call('/api/auth/login', {'username': 'linhaoran', 'password': '123456'})['token']
admin = call('/api/auth/login', {'username': 'admin', 'password': 'admin123'})['token']
resources = call('/api/resource/search?pageSize=1')
assert resources and resources.get('total', 0) > 0, 'No resources found'
recommendations = call('/api/recommend/personal?topN=10', token=student)
assert isinstance(recommendations, list) and recommendations, 'No recommendations'
nodes = call('/api/graph/nodes?limit=1')
assert isinstance(nodes, list) and nodes, 'No graph nodes'
pending = call('/api/admin/resource/audit?status=0&pageSize=10', token=admin)
items = pending.get('list') or pending.get('records') or []
assert items, 'No pending disposable resource available to approve'
resource_id = items[0]['id']
call(f'/api/admin/resource/audit/{resource_id}/approve', {}, admin)
logs = call('/api/admin/graph/sync/logs', token=admin)
matches = [entry for entry in logs if entry['resourceId'] == resource_id]
assert matches and matches[0]['syncStatus'] == 1, f'Graph sync did not succeed: {matches[:1]}'
print(json.dumps({'login': 'ok', 'resources': 'ok', 'recommendations': len(recommendations),
                  'graph_nodes': len(nodes), 'approved_resource': resource_id,
                  'sync_status': matches[0]['syncStatus']}, ensure_ascii=False))
