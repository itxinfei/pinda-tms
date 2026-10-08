# -*- coding: utf-8 -*-
"""按 6 位区划码匹配高德坐标，写入 pd_area.lng/lat
坐标源：高德开放平台行政区划查询（GCJ02）
本项目 area_code：2位省 / 4位市 / 6位县 / 9位镇
"""
import json, io, csv, sys, subprocess

GEO = r"D:/MyCode/pinda-tms/.tmp_geo/xzqh.json"
OUT = r"D:/MyCode/pinda-tms/.tmp_geo/geo.csv"

tree = json.load(open(GEO, encoding="utf-8"))

# 扁平化：6位码 -> (lng, lat)
flat = {}
def walk(n):
    code = n.get("code")
    c = n.get("center")
    if code and c and "longitude" in c:
        flat[code] = (c["longitude"], c["latitude"])
    for x in n.get("children", []):
        walk(x)
for p in tree:
    walk(p)
sys.stderr.write("坐标源有效节点: %d\n" % len(flat))

# 生成 code(2/4/6位) -> 坐标 的补充表（源里主要是 6 位，省级是 2 位）
# 源顶层 province 用 110000 这种 6 位；本项目省用 11（2 位）→ 取前缀匹配
rows = []
matched = 0

# 读库里已有的 area_code（用 mysql 导出）
MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql"
sql = "SELECT id, area_code, level FROM pd_auth.pd_area ORDER BY id"
out = subprocess.run([MYSQL, "-h", "192.168.20.130", "-uroot", "-p123456",
                      "-N", "-B", "--default-character-set=utf8", "-e", sql],
                     capture_output=True, text=True, encoding="utf-8")

def lookup(code):
    """按本项目 code 形态匹配：2位省/4位市/6位县 都能用"""
    c = code
    # 依次尝试：原码、去后缀、6位补0、4位补0、2位补0
    cands = [c]
    if len(c) == 2:   # 省 11 -> 110000
        cands.append(c + "0000")
    elif len(c) == 4: # 市 1101 -> 110100
        cands.append(c + "00")
    for x in cands:
        if x in flat:
            return flat[x]
    return None

with io.open(OUT, "w", encoding="utf-8", newline="") as fw:
    fw.write("id,lng,lat\n")
    for line in out.stdout.strip().split("\n"):
        if not line.strip():
            continue
        p = line.split("\t")
        if len(p) < 3:
            continue
        rid, code, lv = p[0], p[1], p[2]
        r = lookup(code)
        if r:
            fw.write(u"%s,%.6f,%.6f\n" % (rid, r[0], r[1]))
            matched += 1

sys.stderr.write("匹配成功: %d\n" % matched)