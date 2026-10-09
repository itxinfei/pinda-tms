# -*- coding: utf-8 -*-
"""端到端验收（走网关 8080）：
  - 登录 anno/login（原始报障 500 的接口）
  - 菜单路由
  - 核心 authority 接口
  - 之前 401 的 base / web-manager 接口（已剥前缀 + 路由 stripPrefix=false）

关键请求格式（已从 Controller 源码核实）：
  - base 的 /page 为 GET + 查询参数 page/pageSize（如 GoodsTypeController/TransportLineTypeController/TruckTypeController）
  - web-manager 的 /page 为 POST + JSON body {"page":1,"pageSize":5}
    （TransportOrderController/DriverJobController/TransportTaskController/PickupDispatchTaskController
     均为 @PostMapping("/page") @RequestBody xxxVo，读取 getPage()/getPageSize()）

用法: python e2e_verify_fixed.py [account] [password]
"""
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
    key = "e2e" + uuid.uuid4().hex[:8]
    st, _, _ = http("GET", f"{BASE}/authority/anno/captcha?key={key}")
    if st != 200:
        return None, None, st
    return key, (r.get(f"captcha:{key}") or "").strip().strip('"'), st


def check(st, body):
    """成功判定: HTTP 200 且非 '未经授权'/code=401; 列表或含 records 均视为有数据。"""
    try:
        j = json.loads(body)
    except Exception:
        j = None
    if st != 200:
        return False, f"HTTP {st}"
    if isinstance(j, dict):
        c = j.get("code")
        if c not in (None, 0) and "未经授权" in str(j.get("msg", "")):
            return False, f"code={c} msg={j.get('msg')}"
        if c not in (None, 0):
            return False, f"code={c} msg={j.get('msg')}"
    return True, "OK"


def datainfo(body):
    """从响应中提取数据量信息用于打印。"""
    try:
        j = json.loads(body)
    except Exception:
        return ""
    if isinstance(j, list):
        return f" list={len(j)}"
    if isinstance(j, dict):
        if "items" in j:
            return f" items={len(j.get('items') or [])} counts={j.get('counts')} page={j.get('page')}/{j.get('pages')}"
        if isinstance(j.get("data"), dict) and "records" in j["data"]:
            return f" records={len(j['data']['records'])}"
        if isinstance(j.get("data"), list):
            return f" list={len(j['data'])}"
    return ""


account = sys.argv[1] if len(sys.argv) > 1 else "pinda"
password = sys.argv[2] if len(sys.argv) > 2 else "123456"

total_ok, total_all = 0, 0

print("=" * 78)
print("1) 登录 anno/login（原始报障 500 的接口）")
key, code, st = fresh_captcha()
print(f"   captcha http={st} code={code}")
payload = json.dumps({"account": account, "password": password, "key": key, "code": code}).encode()
st, body, dt = http("POST", f"{BASE}/authority/anno/login", data=payload,
                    headers={"Content-Type": "application/json"})
print(f"   POST /api/authority/anno/login -> HTTP {st} ({dt:.2f}s)")
ok_login = False
if st == 200:
    resp = json.loads(body)
    if resp.get("code") == 0:
        token = resp["data"]["token"]["token"]
        ok_login = True
        print(f"   登录成功: account={resp['data']['user']['account']} 权限点={len(resp['data'].get('permissionsList', []))}")
    else:
        print("   业务错误:", resp.get("msg"))
else:
    print("   BODY:", body[:300])
if not ok_login:
    print("!!! 登录失败，终止"); sys.exit(1)

H = {"token": token, "Content-Type": "application/json"}

print("=" * 78)
print("2) 菜单路由 /api/authority/menu/router")
st, body, dt = http("GET", f"{BASE}/authority/menu/router", headers=H)
try:
    j = json.loads(body); menus = j.get("data") or []
    print(f"   HTTP {st} code={j.get('code')} 菜单条数={len(menus) if isinstance(menus, list) else type(menus)}")
    if isinstance(menus, list) and menus:
        print("   一级菜单:", [m.get("name") for m in menus][:15])
except Exception as e:
    print("   解析失败:", e)
good, info = check(st, body)
total_ok += 1 if good else 0; total_all += 1

print("=" * 78)
print("3) 核心 authority 接口")
auth_checks = [
    ("GET", "/authority/role/page?current=1&size=10"),
    ("GET", "/authority/user/page?current=1&size=10"),
    ("GET", "/authority/menu/tree"),
    ("GET", "/authority/area"),
    ("GET", "/authority/org/tree"),
]
ok = 0
for m, path in auth_checks:
    st, body, dt = http(m, f"{BASE}{path}", headers=H)
    good, info = check(st, body)
    if good: ok += 1
    print(f"   [{'OK ' if good else 'FAIL'}] {m} {path} -> {info}{datainfo(body)}")
total_ok += ok; total_all += len(auth_checks)
print(f"   核心接口通过 {ok}/{len(auth_checks)}")

print("=" * 78)
print("4) 之前 401 的 base 接口（已剥 /base 前缀 + 网关 stripPrefix=false）")
print("   注意: base 的 /page 为 GET + 查询参数 page/pageSize")
base_checks = [
    ("GET", "/base/transportLine"),
    ("GET", "/base/transportLine/trips"),
    ("GET", "/base/transportLine/type/page?page=1&pageSize=10"),
    ("GET", "/base/goodsType"),
    ("GET", "/base/goodsType/page?page=1&pageSize=10"),
    ("GET", "/base/truck"),
    ("GET", "/base/truck/type/page?page=1&pageSize=10"),
    ("GET", "/base/truck/count"),
]
ok = 0
for m, path in base_checks:
    st, body, dt = http(m, f"{BASE}{path}", headers=H)
    good, info = check(st, body)
    if good: ok += 1
    print(f"   [{'OK ' if good else 'FAIL'}] {m} {path} -> {info}{datainfo(body)}")
total_ok += ok; total_all += len(base_checks)
print(f"   base 接口通过 {ok}/{len(base_checks)}")

print("=" * 78)
print("5) 之前 401 的 web-manager 接口（真实 kebab controller 路径）")
print("   注意: web-manager 的 /page 为 POST + JSON body {\"page\":1,\"pageSize\":5}")
wm_checks = [
    ("POST", "/web-manager/transport-order-manager/page"),
    ("POST", "/web-manager/driver-job-manager/page"),
    ("POST", "/web-manager/transport-task-manager/page"),
    ("POST", "/web-manager/pickup-dispatch-task-manager/page"),
]
wm_body = json.dumps({"page": 1, "pageSize": 5}).encode()
ok = 0
for m, path in wm_checks:
    st, body, dt = http(m, f"{BASE}{path}", data=wm_body, headers=H)
    good, info = check(st, body)
    if good: ok += 1
    print(f"   [{'OK ' if good else 'FAIL'}] {m} {path} -> {info}{datainfo(body)}")
total_ok += ok; total_all += len(wm_checks)
print(f"   web-manager 接口通过 {ok}/{len(wm_checks)}")

print("=" * 78)
print(f"端到端验收完成: 通过 {total_ok}/{total_all}")
print("=" * 78)
