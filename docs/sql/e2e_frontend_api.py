# -*- coding: utf-8 -*-
"""
按「前端真实调用清单」逐条打网关：登录取 token，然后按 api/*.js 里的 method+url 逐个请求。
判定标准：HTTP 200 且 不是 401/404（业务参数错误算通过，因为说明鉴权已放行、后端已收到）
"""
import json
import sys
import urllib.error
import urllib.request
import uuid

import redis

sys.path.insert(0, r"D:\MyCode\pinda-tms\docs\sql")
from check_frontend_resources import extract, norm  # noqa: E402

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


# 1) 登录
key = "api" + uuid.uuid4().hex[:8]
http("GET", f"{BASE}/authority/anno/captcha?key={key}")
code = (r.get(f"captcha:{key}") or "").strip().strip('"')
st, body = http("POST", f"{BASE}/authority/anno/login",
                data=json.dumps({"account": "pinda", "password": "123456", "key": key, "code": code}).encode(),
                headers={"Content-Type": "application/json"})
if st != 200 or json.loads(body).get("code") != 0:
    print("登录失败:", st, body[:200]); sys.exit(1)
token = json.loads(body)["data"]["token"]["token"]
H = {"token": token, "Content-Type": "application/json"}
print("登录成功\n")

results = []
for fn, method, raw, nurl in extract():
    # 安全红线：端到端测试绝不触发写操作（DELETE/PUT 会真删真改，曾误清空角色授权表）
    if method in ("DELETE", "PUT"):
        continue
    if method == "POST" and not (raw.endswith("/page") or raw.endswith("/tree")):
        continue
    # 还原前端真实 URL（保留 ${} 的用占位值替换不了，跳过动态 id 的）
    if "${" in raw:
        continue
    url = f"{BASE}{raw}"
    data = json.dumps({}).encode() if method in ("POST", "PUT", "DELETE") else None
    st, body = http(method, url, data=data, headers=H)
    try:
        j = json.loads(body)
        code_ = j.get("code")
        msg = str(j.get("msg"))[:40]
    except Exception:
        code_, msg = "-", body[:40].replace("\n", " ")
    verdict = "PASS" if st == 200 and code_ not in (401, 404, "-") or (st == 200 and code_ in (500, -9)) else "FAIL"
    results.append((fn, method, raw, st, code_, msg))

print(f"{'文件':<18}{'方法':<7}{'URL':<50}{'HTTP':<6}{'code':<8}msg")
print("-" * 110)
bad = []
for fn, method, raw, st, code_, msg in results:
    flag = ""
    if st != 200 or code_ in (401, 404, "-") or code_ == 500:
        flag = "  <<<<"
        bad.append((fn, method, raw, st, code_, msg))
    print(f"{fn:<18}{method:<7}{raw:<50}{st:<6}{str(code_):<8}{msg[:30]}{flag}")
print("-" * 110)
print(f"合计 {len(results)} 条，异常 {len(bad)} 条")
for b in bad:
    print("  异常:", b)
