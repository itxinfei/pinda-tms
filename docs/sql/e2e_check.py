# -*- coding: utf-8 -*-
"""
端到端验收脚本（走网关 8080，与浏览器完全同路径）：
  1) 取验证码(从 Redis 读值) -> POST /api/authority/anno/login
  2) 带 token 调 GET /api/authority/menu/router（菜单路由）
  3) 带 token 调若干管理端业务接口，统计 HTTP 状态与 code
用法: python e2e_check.py [account] [password]
"""
import json
import sys
import time
import urllib.error
import urllib.request
import uuid

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
    key = "e2e" + uuid.uuid4().hex[:8]
    st, _, _ = http("GET", f"{BASE}/authority/anno/captcha?key={key}")
    if st != 200:
        return None, None, st
    val = (r.get(f"captcha:{key}") or "").strip().strip('"')
    return key, val, st


account = sys.argv[1] if len(sys.argv) > 1 else "pinda"
password = sys.argv[2] if len(sys.argv) > 2 else "123456"

print("=" * 78)
print("1) 登录")
key, code, st = fresh_captcha()
print(f"   验证码: http={st} value={code}")
payload = json.dumps({"account": account, "password": password, "key": key, "code": code}).encode()
st, body, dt = http("POST", f"{BASE}/authority/anno/login", data=payload,
                    headers={"Content-Type": "application/json"})
print(f"   POST /api/authority/anno/login -> HTTP {st} ({dt:.2f}s)")
if st != 200:
    print("   BODY:", body[:400])
    sys.exit(1)
resp = json.loads(body)
if resp.get("code") != 0:
    print("   登录失败:", resp.get("msg"))
    sys.exit(1)
token = resp["data"]["token"]["token"]
user = resp["data"]["user"]
print(f"   登录成功: id={user['id']} account={user['account']} name={user['name']}")
print(f"   权限点数: {len(resp['data'].get('permissionsList', []))}")

H = {"token": token, "Content-Type": "application/json"}
print("=" * 78)
print("2) 菜单路由 /api/authority/menu/router")
st, body, dt = http("GET", f"{BASE}/authority/menu/router", headers=H)
print(f"   HTTP {st} ({dt:.2f}s)")
try:
    j = json.loads(body)
    menus = j.get("data") or []
    print(f"   code={j.get('code')} msg={j.get('msg')} 菜单条数={len(menus) if isinstance(menus, list) else type(menus)}")
    if isinstance(menus, list) and menus:
        print("   一级菜单:", [m.get("name") or m.get("title") for m in menus][:15])
except Exception as e:
    print("   解析失败:", e, body[:300])

print("=" * 78)
print("3) 管理端业务接口抽查")
checks = [
    ("GET", "/authority/role/page?current=1&size=10"),
    ("GET", "/authority/user/page?current=1&size=10"),
    ("GET", "/authority/menu/tree"),
    ("GET", "/authority/area"),
    ("GET", "/authority/org/tree"),
    ("GET", "/web-manager/transportOrder/page?current=1&size=5"),
    ("GET", "/base/area/page?current=1&size=5"),
]
ok = 0
for m, path in checks:
    st, body, dt = http(m, f"{BASE}{path}", headers=H)
    try:
        j = json.loads(body)
        code = j.get("code")
        extra = ""
        if isinstance(j.get("data"), dict) and "records" in j["data"]:
            extra = f" records={len(j['data']['records'])} total={j['data'].get('total')}"
        elif isinstance(j.get("data"), list):
            extra = f" list={len(j['data'])}"
    except Exception:
        code = "-"
        extra = body[:100].replace("\n", " ")
    flag = "OK " if st == 200 and code == 0 else "FAIL"
    if flag == "OK ":
        ok += 1
    print(f"   [{flag}] HTTP {st} {m} {path} -> code={code}{extra}")
print("=" * 78)
print(f"业务接口通过 {ok}/{len(checks)}")
