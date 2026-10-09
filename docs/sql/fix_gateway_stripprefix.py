# -*- coding: utf-8 -*-
"""在 Nacos pd-gateway-prod.yml 的 pd-base 路由上设置 stripPrefix: false，
使 Zuul 转发完整 /base/... 路径以匹配 pd-base 前缀包含式 controller。
仅改 base 路由, 不影响其余(authority 等前缀无关式 controller)。
"""
import json, urllib.parse, urllib.request

NACOS = "http://192.168.20.130:8848"
GROUP = "pinda-tms"
NS = "1cb93ce4-dc0e-4730-b759-d35fd7ed93c3"
DATA_ID = "pd-gateway-prod.yml"


def http(url, data=None, headers=None, method="GET"):
    req = urllib.request.Request(url, data=data, headers=headers or {}, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return resp.status, resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")


st, body = http(f"{NACOS}/nacos/v1/auth/login",
                data=urllib.parse.urlencode({"username": "nacos", "password": "nacos"}).encode(),
                headers={"Content-Type": "application/x-www-form-urlencoded"}, method="POST")
token = json.loads(body).get("accessToken")
print("token:", bool(token))

# 取当前配置
url = (f"{NACOS}/nacos/v1/cs/configs?dataId={DATA_ID}&group={GROUP}"
       f"&namespace={NS}&tenant={NS}&accessToken={token}")
st, cfg = http(url)
print("GET config status:", st, "len:", len(cfg))
print("--- 当前 pd-base 段落 ---")
for line in cfg.splitlines():
    if "pd-base" in line or ("path: /base" in line) or ("serviceId: pd-base" in line):
        print("   ", line)

old_block = "    pd-base:\n      path: /base/**\n      serviceId: pd-base"
new_block = "    pd-base:\n      path: /base/**\n      serviceId: pd-base\n      stripPrefix: false"
if old_block in cfg:
    new_cfg = cfg.replace(old_block, new_block, 1)
    print("\n准备写入: 已定位 pd-base 段落并加入 stripPrefix: false")
else:
    print("\n[WARN] 未找到预期 pd-base 段落, 原始片段如下, 需手动处理:")
    print(cfg[:1500])
    raise SystemExit(1)

DRY_RUN = False
if DRY_RUN:
    print("[DRY RUN] 不写入。新配置 pd-base 段:\n", new_block)
    raise SystemExit(0)

# 发布更新
post = (f"{NACOS}/nacos/v1/cs/configs?dataId={urllib.parse.quote(DATA_ID)}"
        f"&group={urllib.parse.quote(GROUP)}&tenant={NS}&accessToken={token}")
data = urllib.parse.urlencode({"content": new_cfg}).encode()
st, body = http(post, data=data,
               headers={"Content-Type": "application/x-www-form-urlencoded"}, method="POST")
print("\nPOST publish status:", st, "resp:", body)
