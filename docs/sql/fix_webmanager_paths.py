# -*- coding: utf-8 -*-
"""修正 4 个错误的 web-manager 资源 URL（之前误用 /web-manager 网关前缀 + camelCase）。
正确路径 = 实际 controller 子路径(kebab)。这些正确路径在 293 注册里已存在, 故直接删除错误资源。
"""
import pymysql

# (错误 url, 正确 url, method, 备注)
FIXES = [
    ("/transportOrder/page",      "/transport-order-manager/page",      "POST", "TransportOrderController"),
    ("/driverJob/page",           "/driver-job-manager/page",          "POST", "DriverJobController"),
    ("/taskTransport/page",       "/transport-task-manager/page",       "POST", "TransportTaskController"),
    ("/taskPickupDispatchJob/page","/pickup-dispatch-task-manager/page","POST", "PickupDispatchTaskController"),
]

DRY_RUN = False

conn = pymysql.connect(host="192.168.20.130", port=3306, user="root",
                       password="123456", db="pd_auth", charset="utf8mb4", autocommit=False)
cur = conn.cursor()

print("=== 当前错误资源 & 对应正确资源是否存在 ===")
for wrong_url, correct_url, method, note in FIXES:
    cur.execute("SELECT id FROM pd_auth_resource WHERE method=%s AND url=%s", (method, wrong_url))
    r = cur.fetchone()
    cur.execute("SELECT id FROM pd_auth_resource WHERE method=%s AND url=%s AND url<>%s", (method, correct_url, wrong_url))
    correct = cur.fetchone()
    wid = r[0] if r else None
    cid = correct[0] if correct else None
    print(f"  {note}: wrong {method} {wrong_url} id={wid} | correct {method} {correct_url} id={cid}")

if DRY_RUN:
    print("\n[DRY RUN] 未修改。")
    conn.close(); raise SystemExit(0)

print("\n=== 执行 ===")
# 备份
cur.execute("DROP TABLE IF EXISTS pd_auth_resource_bak_wmfix")
cur.execute("CREATE TABLE pd_auth_resource_bak_wmfix LIKE pd_auth_resource")
cur.execute("INSERT INTO pd_auth_resource_bak_wmfix SELECT * FROM pd_auth_resource")

for wrong_url, correct_url, method, note in FIXES:
    cur.execute("SELECT id FROM pd_auth_resource WHERE method=%s AND url=%s", (method, wrong_url))
    r = cur.fetchone()
    if not r:
        print(f"  [跳过] 未找到错误资源 {method} {wrong_url}")
        continue
    wid = r[0]
    cur.execute("SELECT id FROM pd_auth_resource WHERE method=%s AND url=%s AND id<>%s", (method, correct_url, wid))
    correct = cur.fetchone()
    if correct:
        # 正确资源已存在 -> 删除错误资源, 确保其 role100 授权落到正确资源上
        cid = correct[0]
        cur.execute("SELECT 1 FROM pd_auth_role_authority WHERE role_id=100 AND authority_id=%s", (cid,))
        if not cur.fetchone():
            cur.execute("INSERT INTO pd_auth_role_authority(role_id, authority_id) VALUES(100, %s)", (cid,))
            print(f"  role100 补充授权 -> 正确资源 {cid} {correct_url}")
        cur.execute("DELETE FROM pd_auth_role_authority WHERE authority_id=%s", (wid,))
        cur.execute("DELETE FROM pd_auth_resource WHERE id=%s", (wid,))
        print(f"  DEL 错误 {method} {wrong_url} (id={wid}); 复用正确 {method} {correct_url} (id={cid})")
    else:
        # 正确资源不存在 -> 原地改 url, 保留 id 与授权
        cur.execute("UPDATE pd_auth_resource SET url=%s WHERE id=%s", (correct_url, wid))
        print(f"  UPD id={wid} {method} {wrong_url} -> {correct_url}")

conn.commit()
print("\n完成。")
conn.close()
