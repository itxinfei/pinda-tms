# -*- coding: utf-8 -*-
"""
修复 pd_auth_resource 中带服务前缀的 URL：
网关 AccessFilter 会把请求 URI 的 /api 与第一段服务前缀剥掉后再匹配资源表，
因此资源表 URL 必须存「子路径」。base / web-manager 是真实 Zuul 路由前缀，
之前误把前缀写进 resource.url 导致系统性 401。

策略(最小改动、保留 role_authority 授权):
  - 对每个 /base/* 或 /web-manager/* 资源:
       stripped = url 去掉第一段前缀
       若已存在 (method, stripped) 的「干净」资源 -> 当前前缀资源冗余, 删除之(并清理其 role_authority)
       否则 -> 原地 UPDATE url=stripped (id 不变, 授权不动)
  - /user/* 等为 authority 子路径, 不处理。
"""
import pymysql

STRIP_PREFIXES = {"base", "web-manager"}  # 真实 Zuul 路由前缀
DRY_RUN = False  # 已通过 probe 实证：前端走 /api/base 与 /api/web-manager，剥前缀正确

conn = pymysql.connect(host="192.168.20.130", port=3306, user="root",
                       password="123456", db="pd_auth", charset="utf8mb4",
                       autocommit=False)
cur = conn.cursor()

print("=== 索引(看 url/method 是否有 UNIQUE) ===")
cur.execute("SHOW INDEX FROM pd_auth_resource")
for r in cur.fetchall():
    print("   ", r[2], r[3], "UNIQUE" if r[1] == 0 else "non-unique", "cols=", r[4])

print("\n=== DRY RUN (STRIP_PREFIXES=%s) ===" % STRIP_PREFIXES)
cur.execute("SELECT id, method, url, name FROM pd_auth_resource")
rows = cur.fetchall()

updates = []
deletes = []
existing_map = {}
for (rid, method, url, name) in rows:
    existing_map.setdefault((method, url), rid)

for (rid, method, url, name) in rows:
    parts = url.lstrip("/").split("/")
    if len(parts) >= 2 and parts[0] in STRIP_PREFIXES:
        stripped = "/" + "/".join(parts[1:])
        key = (method, stripped)
        if key in existing_map and existing_map[key] != rid:
            # 干净资源已存在 -> 删除当前前缀资源
            deletes.append((rid, method, url, name, stripped, existing_map[key]))
        else:
            updates.append((rid, method, url, stripped, name))

print(f"\n计划 UPDATE(原地改 url, 保留 id/授权): {len(updates)} 条")
for (rid, m, old, new, name) in updates:
    print(f"   UPDATE id={rid} {m} {old} -> {new}  ({name})")

print(f"\n计划 DELETE(冗余, 干净资源已存在): {len(deletes)} 条")
for (rid, m, old, name, stripped, existing_id) in deletes:
    print(f"   DELETE id={rid} {m} {old} -> 已存在 {existing_id} {m} {stripped}  ({name})")

if DRY_RUN:
    print("\n[DRY RUN] 未做任何修改。")
    conn.close()
    raise SystemExit(0)

# ---- 执行 ----
print("\n=== 执行 ===")
# 备份
cur.execute("DROP TABLE IF EXISTS pd_auth_resource_bak_prefixfix")
cur.execute("CREATE TABLE pd_auth_resource_bak_prefixfix LIKE pd_auth_resource")
cur.execute("INSERT INTO pd_auth_resource_bak_prefixfix SELECT * FROM pd_auth_resource")
print("  备份 -> pd_auth_resource_bak_prefixfix")

for (rid, m, old, new, name) in updates:
    cur.execute("UPDATE pd_auth_resource SET url=%s WHERE id=%s", (new, rid))
    print(f"   UPD id={rid} {m} {old} -> {new}")

for (rid, m, old, name, stripped, existing_id) in deletes:
    # 确保干净资源对 role100 已授权
    cur.execute("SELECT 1 FROM pd_auth_role_authority WHERE role_id=100 AND authority_id=%s", (existing_id,))
    if not cur.fetchone():
        cur.execute("INSERT INTO pd_auth_role_authority(role_id, authority_id) VALUES(100, %s)", (existing_id,))
        print(f"   role100 补充授权 -> existing {existing_id}")
    cur.execute("DELETE FROM pd_auth_role_authority WHERE authority_id=%s", (rid,))
    cur.execute("DELETE FROM pd_auth_resource WHERE id=%s", (rid,))
    print(f"   DEL id={rid} {m} {old} (保留 {existing_id} {stripped})")

conn.commit()
print("\n完成。")
conn.close()
