# -*- coding: utf-8 -*-
"""从 Nacos 拉取 pd-gateway 的 Zuul 路由配置，提取真实的服务前缀集合。"""
import json
import urllib.parse
import urllib.request

NACOS = "http://192.168.20.130:8848"
GROUP = "pinda-tms"
NAMESPACE_NAME = "pinda-tms"


def http(url, data=None, headers=None, method="GET", timeout=15):
    req = urllib.request.Request(url, data=data, headers=headers or {}, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")
    except Exception as e:  # noqa
        return -1, str(e)


def post_form(url, form):
    return http(url, data=urllib.parse.urlencode(form).encode(),
                headers={"Content-Type": "application/x-www-form-urlencoded"}, method="POST")


# 1) 登录拿 token
st, body = post_form(f"{NACOS}/nacos/v1/auth/login",
                     {"username": "nacos", "password": "nacos"})
print("login:", st, body[:200])
try:
    token = json.loads(body).get("accessToken")
except Exception:
    token = None
print("token present:", bool(token))

# 2) 列出 namespace，找到 pinda-tms 的 ID
st, body = http(f"{NACOS}/nacos/v1/console/namespaces",
                headers={"Authorization": f"Bearer {token}"} if token else {})
print("\n=== namespaces ===")
print(body[:800])

ns_id = None
try:
    for ns in json.loads(body).get("data", []):
        if ns.get("namespaceName") == NAMESPACE_NAME or ns.get("namespaceId") == NAMESPACE_NAME:
            ns_id = ns.get("namespaceId")
except Exception as e:
    print("parse ns err:", e)
if not ns_id:
    ns_id = NAMESPACE_NAME  # 可能 namespaceId 直接就是字符串
print("using namespaceId:", ns_id)

# 3) 尝试常见的 gateway 配置 dataId
candidates = ["pd-gateway.yml", "pd-gateway-prod.yml", "pd-gateway-dev.yml",
              "gateway.yml", "pd-gateway.yaml"]
for data_id in candidates:
    url = (f"{NACOS}/nacos/v1/cs/configs?dataId={urllib.parse.quote(data_id)}"
           f"&group={urllib.parse.quote(GROUP)}&namespace={urllib.parse.quote(ns_id)}"
           f"&tenant={urllib.parse.quote(ns_id)}")
    if token:
        url += f"&accessToken={urllib.parse.quote(token)}"
    st, body = http(url)
    if st == 200 and body and "zuul" in body.lower():
        print(f"\n=== FOUND config {data_id} (len={len(body)}) ===")
        print(body[:3000])
        break
    else:
        print(f"-- {data_id}: st={st} len={len(body)} hasZuul={'zuul' in body.lower()}")
