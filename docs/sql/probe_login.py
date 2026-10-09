# -*- coding: utf-8 -*-
"""登录链路探测：生成验证码 -> 从 Redis 读取验证码值 -> 调 /anno/login，观察是否 500"""
import json
import sys
import uuid
import urllib.request
import urllib.error

import redis

HOST = "192.168.20.130"
AUTH_DIRECT = f"http://{HOST}:9000"
GATEWAY_API = f"http://{HOST}:8080/api/authority"

r = redis.Redis(host=HOST, port=6379, db=0, decode_responses=True, socket_timeout=5,
                protocol=2)  # 服务端 Redis < 6.0，不支持 HELLO


def http(url, data=None, headers=None, timeout=15):
    req = urllib.request.Request(url, data=data, headers=headers or {})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            body = resp.read()
            return resp.status, body.decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        body = e.read()
        return e.code, body.decode("utf-8", "replace")


def get_captcha(base, key):
    return http(f"{base}/anno/captcha?key={key}")


def main():
    key = "probe" + uuid.uuid4().hex[:8]
    st, body = get_captcha(AUTH_DIRECT, key)
    print("captcha http:", st, "len:", len(body))

    val = r.get(f"captcha:{key}")
    print("redis captcha:%s =" % key, repr(val))
    if val is None:
        keys = r.keys("captcha*")
        print("captcha* keys:", keys[:10])
        return
    # j2cache 会把字符串序列化成带引号的 JSON，需要去掉
    val = val.strip().strip('"')

    account = sys.argv[1] if len(sys.argv) > 1 else "pinda"
    password = sys.argv[2] if len(sys.argv) > 2 else "123456"
    payload = json.dumps({"account": account, "password": password, "key": key, "code": val}).encode()
    for name, base in (("direct:9000", AUTH_DIRECT), ("gateway:8080", GATEWAY_API)):
        k2 = "probe" + uuid.uuid4().hex[:8]
        get_captcha(base if name == "direct:9000" else GATEWAY_API, k2)
        v2 = (r.get(f"captcha:{k2}") or "").strip().strip('"')
        payload = json.dumps({"account": account, "password": password, "key": k2, "code": v2}).encode()
        st, body = http(f"{base}/anno/login", data=payload,
                        headers={"Content-Type": "application/json"})
        print(f"--- login via {name} -> HTTP {st}  (captcha={v2})")
        print(body[:600])
        print()


if __name__ == "__main__":
    main()
