# -*- coding: utf-8 -*-
"""列出 pinda namespace 下所有 Nacos 配置，并抓取含 zuul 路由的那个。"""
import json
import urllib.parse
import urllib.request

NACOS = "http://192.168.20.130:8848"
GROUP = "pinda-tms"
NS = "1cb93ce4-dc0e-4730-b759-d35fd7ed93c3"


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


st, body = post_form(f"{NACOS}/nacos/v1/auth/login", {"username": "nacos", "password": "nacos"})
token = json.loads(body).get("accessToken")

# 列出 config 列表 (dataId 模糊 *)
for page in range(1, 6):
    url = (f"{NACOS}/nacos/v1/cs/configs?search=accurate&dataId=&group={urllib.parse.quote(GROUP)}"
           f"&namespace={NS}&pageNo={page}&pageSize=100&accessToken={token}")
    st, body = http(url)
    try:
        d = json.loads(body)
    except Exception:
        print("parse err", body[:200]); break
    items = d.get("pageItems", [])
    print(f"--- page {page}: total={d.get('totalCount')} items={len(items)}")
    for it in items:
        did = it.get("dataId")
        print("   ", did, "| group=", it.get("group"))
    if not items:
        break

# 抓取疑似含 zuul 的 dataId：常见 gateway 配置名
print("\n=== 抓取各 candidate 内容(含 zuul 关键词) ===")
cands = ["pd-gateway.yml", "pd-gateway-prod.yml", "pd-gateway-dev.yml",
         "gateway.yml", "pd-zuul.yml", "zuul.yml", "pd-gateway"]
for did in cands:
    url = (f"{NACOS}/nacos/v1/cs/configs?dataId={urllib.parse.quote(did)}"
           f"&group={urllib.parse.quote(GROUP)}&namespace={NS}&tenant={NS}&accessToken={token}")
    st, body = http(url)
    print(f"\n### {did}: st={st} len={len(body)}")
    if st == 200 and "zuul" in body.lower():
        print(body[:4000])
