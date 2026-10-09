# -*- coding: utf-8 -*-
"""只读分析 pd_auth_resource：表结构 + 首段分布 + 触及服务前缀的 URL。"""
import pymysql

PREFIXES = {"oms", "work", "dispatch", "base", "user", "aggregation", "auth",
            "authority", "web-manager", "web-driver", "web-courier",
            "web-customer", "netty-service"}

conn = pymysql.connect(host="192.168.20.130", port=3306, user="root",
                       password="123456", db="pd_auth", charset="utf8mb4")
cur = conn.cursor()

print("=== DESCRIBE pd_auth_resource ===")
cur.execute("DESCRIBE pd_auth_resource")
for r in cur.fetchall():
    print("  ", r)

print("\n=== total rows ===")
cur.execute("SELECT COUNT(*) FROM pd_auth_resource")
print("  ", cur.fetchone())

print("\n=== first-segment distribution (url) ===")
cur.execute("SELECT SUBSTRING_INDEX(SUBSTRING_INDEX(url,'/',2),'/',-1) AS seg, COUNT(*) c "
            "FROM pd_auth_resource GROUP BY seg ORDER BY c DESC")
for r in cur.fetchall():
    print(f"   /{r[0]}: {r[1]}")

print("\n=== URLs that START WITH a service prefix (would be stripped) ===")
cur.execute("SELECT id, method, url, name FROM pd_auth_resource")
rows = cur.fetchall()
matched = []
for (rid, method, url, name) in rows:
    parts = url.lstrip("/").split("/")
    if len(parts) >= 2 and parts[0] in PREFIXES:
        matched.append((rid, method, url, name, parts[0]))
print(f"  count={len(matched)}")
for (rid, method, url, name, pfx) in matched:
    print(f"   [{pfx}] id={rid} {method} {url}  ({name})")

# 检测剥离前缀后是否和“无前缀资源”撞车
print("\n=== collision check after stripping prefix ===")
existing = {}
for (rid, method, url, name) in rows:
    existing.setdefault(method + "|" + url, []).append(rid)
collisions = []
for (rid, method, url, name, pfx) in matched:
    stripped = "/" + "/".join(url.lstrip("/").split("/")[1:])
    key = method + "|" + stripped
    if key in existing and existing[key] != [rid]:
        collisions.append((rid, method, url, stripped, existing[key]))
print(f"  potential collisions={len(collisions)}")
for c in collisions[:50]:
    print("   ", c)
conn.close()
