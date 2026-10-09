# -*- coding: utf-8 -*-
"""探测 base / web-manager 接口的真实网关路径：分别试 /api/base/... 与 /api/authority/base/...，
看哪个返回 code=0，从而确定前端实际调用约定（决定资源 URL 是否要剥前缀）。"""
import json, sys, time, uuid, urllib.error, urllib.request
import redis

HOST = "192.168.20.130"
BASE = f"http://{HOST}:8080/api"
r = redis.Redis(host=HOST, port=6379, db=0, decode_responses=True, socket_timeout=5, protocol=2)


def http(method, url, data=None, headers=None, timeout=30):
    req = urllib.request.Request(url, data=data, headers=headers or {}, method=method)
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read().decode("utf-8", "replace"), time.time() - t0
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace"), time.time() - t0


def fresh_captcha():
    key = "prb" + uuid.uuid4().hex[:8]
    st, _, _ = http("GET", f"{BASE}/authority/anno/captcha?key={key}")
    if st != 200:
        return None, None, st
    val = (r.get(f"captcha:{key}") or "").strip().strip('"')
    return key, val, st


account = sys.argv[1] if len(sys.argv) > 1 else "pinda"
password = sys.argv[2] if len(sys.argv) > 2 else "123456"

key, code, st = fresh_captcha()
payload = json.dumps({"account": account, "password": password, "key": key, "code": code}).encode()
st, body, _ = http("POST", f"{BASE}/authority/anno/login", data=payload,
                  headers={"Content-Type": "application/json"})
print("login:", st)
resp = json.loads(body)
if resp.get("code") != 0:
    print("登录失败:", resp.get("msg")); sys.exit(1)
token = resp["data"]["token"]["token"]
H = {"token": token, "Content-Type": "application/json"}

# 同一业务接口，试两种网关路径
probes = [
    ("base.transportLine", "GET", "/transportLine"),   # 子路径形式
    ("base.transportLine", "GET", "/base/transportLine"),
    ("base.transportLine", "GET", "/authority/base/transportLine"),
    ("web-manager.transportOrder", "POST", "/transportOrder/page?current=1&size=5"),
    ("web-manager.transportOrder", "POST", "/web-manager/transportOrder/page?current=1&size=5"),
]
for label, m, path in probes:
    for prefix in ("",):
        full = f"{BASE}{path}"
        st, body, dt = http(m, full, headers=H)
        try:
            j = json.loads(body); c = j.get("code"); msg = j.get("msg")
        except Exception:
            c = "-"; msg = body[:60]
        print(f"[{label}] {m} {path} -> HTTP {st} code={c} msg={msg} ({dt:.2f}s)")
