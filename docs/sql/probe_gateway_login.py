# -*- coding: utf-8 -*-
"""连打网关登录链路，观察 500/502 是否稳定复现 + 耗时"""
import json
import sys
import time
import uuid
import urllib.error
import urllib.request

import redis

HOST = "192.168.20.130"
BASE = f"http://{HOST}:8080/api/authority"

r = redis.Redis(host=HOST, port=6379, db=0, decode_responses=True, socket_timeout=5, protocol=2)


def http(url, data=None, headers=None, timeout=60):
    req = urllib.request.Request(url, data=data, headers=headers or {})
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            body = resp.read()
            return resp.status, body.decode("utf-8", "replace"), time.time() - t0
    except urllib.error.HTTPError as e:
        body = e.read()
        return e.code, body.decode("utf-8", "replace"), time.time() - t0


account = sys.argv[1] if len(sys.argv) > 1 else "pinda"
password = sys.argv[2] if len(sys.argv) > 2 else "123456"
n = int(sys.argv[3]) if len(sys.argv) > 3 else 5

for i in range(n):
    key = "probe" + uuid.uuid4().hex[:8]
    st, body, dt = http(f"{BASE}/anno/captcha?key={key}")
    val = (r.get(f"captcha:{key}") or "").strip().strip('"')
    payload = json.dumps({"account": account, "password": password, "key": key, "code": val}).encode()
    st, body, dt2 = http(f"{BASE}/anno/login", data=payload, headers={"Content-Type": "application/json"})
    print(f"[{i}] captcha={val} login_http={st} cost={dt2:.2f}s len={len(body)} :: {body[:160]}")
