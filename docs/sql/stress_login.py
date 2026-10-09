# -*- coding: utf-8 -*-
"""压测网关登录：连续 N 次（每次新验证码），统计 500/非200 次数与耗时"""
import json, sys, uuid, urllib.request, urllib.error
import redis

HOST = "192.168.20.130"
GW = f"http://{HOST}:8080/api/authority"
r = redis.Redis(host=HOST, port=6379, db=0, decode_responses=True, socket_timeout=5, protocol=2)

def http(url, data=None, headers=None, timeout=20):
    req = urllib.request.Request(url, data=data, headers=headers or {})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")
    except Exception as e:
        return -1, f"{type(e).__name__}:{e}"

def main():
    n = int(sys.argv[1]) if len(sys.argv) > 1 else 20
    acct = sys.argv[2] if len(sys.argv) > 2 else "pinda"
    pwd = sys.argv[3] if len(sys.argv) > 3 else "123456"
    fail = 0
    codes = {}
    for i in range(n):
        key = "stress" + uuid.uuid4().hex[:8]
        st_c, _ = http(f"{GW}/anno/captcha?key={key}")
        val = (r.get(f"captcha:{key}") or "").strip().strip('"')
        if not val:
            codes.setdefault("NO_CAPTCHA", 0); codes["NO_CAPTCHA"] += 1; fail += 1
            print(f"#{i+1} captcha missing"); continue
        payload = json.dumps({"account": acct, "password": pwd, "key": key, "code": val}).encode()
        st, body = http(f"{GW}/anno/login", data=payload, headers={"Content-Type": "application/json"})
        codes[st] = codes.get(st, 0) + 1
        ok = st == 200 and '"code":0' in body.replace(" ", "")
        if not ok:
            fail += 1
            print(f"#{i+1} HTTP {st} body={body[:200]}")
    print(f"\n总次数={n} 失败={fail}")
    print("状态码分布:", codes)

if __name__ == "__main__":
    main()
