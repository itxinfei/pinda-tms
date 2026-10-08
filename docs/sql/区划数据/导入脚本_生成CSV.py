import json, os, csv

BASE = os.path.dirname(os.path.abspath(__file__))
OUT_CSV = os.path.join(BASE, "area_level4.csv")

provinces = json.load(open(os.path.join(BASE, "provinces.json"), encoding="utf-8"))
cities     = json.load(open(os.path.join(BASE, "cities.json"), encoding="utf-8"))
areas      = json.load(open(os.path.join(BASE, "areas.json"), encoding="utf-8"))
streets    = json.load(open(os.path.join(BASE, "streets.json"), encoding="utf-8"))

rows = []          # new_id, parent_id, name, area_code, city_code, level, short_name
id_map = {}        # 官方 code -> 本项目 id

# id 规则: level * 1000000 + 序号（保证可读且唯一）
def gen_id(level, seq):
    return level * 1000000 + seq

# ---- level 0: 省 ----
for i, p in enumerate(sorted(provinces, key=lambda x: x["code"]), 1):
    nid = gen_id(0, i)
    id_map[p["code"]] = nid
    rows.append((nid, None, p["name"], p["code"], p["code"], 0, p["name"]))

# ---- level 1: 市 ----
for i, c in enumerate(sorted(cities, key=lambda x: x["code"]), 1):
    pid = id_map.get(c["provinceCode"])
    if pid is None:
        continue
    nid = gen_id(1, i)
    id_map[c["code"]] = nid
    rows.append((nid, pid, c["name"], c["code"], c["code"], 1, c["name"]))

# ---- level 2: 县 ----
for i, a in enumerate(sorted(areas, key=lambda x: x["code"]), 1):
    pid = id_map.get(a["cityCode"])
    if pid is None:
        continue
    nid = gen_id(2, i)
    id_map[a["code"]] = nid
    rows.append((nid, pid, a["name"], a["code"], a["cityCode"], 2, a["name"]))

# ---- level 3: 镇/街道 ----
for i, s in enumerate(sorted(streets, key=lambda x: x["code"]), 1):
    pid = id_map.get(s["areaCode"])
    if pid is None:
        continue
    nid = gen_id(3, i)
    id_map[s["code"]] = nid
    rows.append((nid, pid, s["name"], s["code"], s["cityCode"], 3, s["name"]))

with open(OUT_CSV, "w", encoding="utf-8", newline="") as fh:
    w = csv.writer(fh)
    w.writerow(["id", "parent_id", "name", "area_code", "city_code", "level", "short_name"])
    w.writerows(rows)

print(f"总条数: {len(rows)}")
from collections import Counter
c = Counter(r[5] for r in rows)
for lv in sorted(c):
    print(f"  level={lv}: {c[lv]} 条")
print(f"孤儿节点(父不存在): {sum(1 for r in rows if r[1] is not None and r[1] not in set(id_map.values()))}")
print(f"CSV -> {OUT_CSV}")