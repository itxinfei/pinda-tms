# -*- coding: utf-8 -*-
"""探测 web-manager /page 的 500 真实错误体，便于定位根因。"""
import json, sys, time, uuid, urllib.error, urllib.request
import redis

HOST = "192.168.20.130"
BASE = f"http://{HOST}:8080/api"
r = redis.Redis(host=HOST, port=6379, db=0, decode_responses=True, socket_timeout=5, protocol=2)


def http(method, url, data=None, headers=None, timeout=30):
    req = urllib.request.Request(url, data=data, headers=headers or {}, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")


account = sys.argv[1] if len(sys.argv) > 1 else "pinda"
password = sys.argv[2] if len(sys.argv) > 2 else "123456"

key = "e2e" + uuid.uuid4().hex[:8]
st, _ = http("GET", f"{BASE}/authority/anno/captcha?key={key}")
code = (r.get(f"captcha:{key}") or "").strip().strip('"')
payload = json.dumps({"account": account, "password": password, "key": key, "code": code}).encode()
st, body = http("POST", f"{BASE}/authority/anno/login", data=payload, headers={"Content-Type": "application/json"})
resp = json.loads(body)
token = resp["data"]["token"]["token"]
H = {"token": token, "Content-Type": "application/json"}

print("登录 code=", resp.get("code"))
paths = [
    "/web-manager/transport-order-manager/page",
    "/web-manager/driver-job-manager/page",
    "/web-manager/transport-task-manager/page",
    "/web-manager/pickup-dispatch-task-manager/page",
]
for p in paths:
    st, body = http("POST", f"{BASE}{p}", data=json.dumps({"page": 1, "pageSize": 5}).encode(), headers=H)
    print("=" * 70)
    print(f"{p} -> HTTP {st}")
    print(body[:1500])
