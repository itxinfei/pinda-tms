# -*- coding: utf-8 -*-
"""
校验「前端实际调用的接口」是否已在网关注册并授权给 role 100。
要点：
- 网关 AccessFilter 匹配的是「去掉路由前缀后」的路径（/authority/role/page -> /role/page）
- 匹配方式为 startsWith 前缀匹配
- 除资源表外，还需 pd_auth_role_authority 中 role 100 有 RESOURCE 授权
"""
import os
import re
import subprocess

UI_API_DIR = r"D:\MyCode\pinda-tms\pd-admin-ui\src\api"
DB_HOST = "192.168.20.130"
MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql"

# 网关 zuul 路由前缀（来自 pd-gateway-prod.yml）
ROUTE_PREFIX = {"authority", "auth", "web-manager", "base", "oms", "work", "user",
                "dispatch", "aggregation", "web-driver", "web-courier", "web-customer",
                "netty-service", "msgs"}

URL_RE = re.compile(r"url\s*:\s*[`'\"]\s*([^`'\"]+)[`'\"]")
METHOD_RE = re.compile(r"method\s*:\s*['\"](\w+)['\"]")
VAR_RE = re.compile(r"\$\{[^}]*\}")


def norm(url: str) -> str:
    url = url.strip().split("?")[0]
    parts = [p for p in url.split("/") if p != ""]
    if parts and parts[0] in ROUTE_PREFIX:
        parts = parts[1:]
    parts = [VAR_RE.sub("", p) for p in parts]
    parts = [p for p in parts if p != ""]
    return "/" + "/".join(parts)


def extract():
    items = []
    for fn in sorted(os.listdir(UI_API_DIR)):
        if not fn.endswith(".js"):
            continue
        with open(os.path.join(UI_API_DIR, fn), encoding="utf-8", errors="ignore") as f:
            src = f.read()
        for m in URL_RE.finditer(src):
            raw = m.group(1)
            if not raw.startswith("/"):
                continue
            # 就近找 method：优先取 url 之前最近一个（api 文件写法是 method 在前、url 在后）
            tail = src[m.end(): m.end() + 300]
            head = src[max(0, m.start() - 200): m.start()]
            mh = METHOD_RE.findall(head)
            mt = METHOD_RE.findall(tail)
            method = (mh[-1] if mh else (mt[0] if mt else "GET")).upper()
            items.append((fn, method, raw, norm(raw)))
    return items


def sqlq(q):
    out = subprocess.run([MYSQL, f"-h{DB_HOST}", "-uroot", "-p123456", "-D", "pd_auth", "-N", "-B", "-e", q],
                         capture_output=True, text=True)
    return [l.split("\t") for l in out.stdout.strip().splitlines() if "\t" in l]


def main():
    res = [(r[0].upper(), r[1]) for r in sqlq("SELECT method,url FROM pd_auth_resource")]
    granted = sqlq("SELECT r.method, r.url FROM pd_auth_role_authority ra "
                   "JOIN pd_auth_resource r ON r.id=ra.authority_id "
                   "WHERE ra.authority_type='RESOURCE' AND ra.role_id=100")
    granted = [(g[0].upper(), g[1]) for g in granted]
    print(f"资源表: {len(res)} 条   role100 已授权资源: {len(granted)} 条\n")

    items = extract()
    not_registered, not_granted = [], []
    for fn, method, raw, nurl in items:
        hit_res = [u for m, u in res if m == method and (nurl == u or nurl.startswith(u.rstrip("/") + "/"))]
        if not hit_res:
            not_registered.append((fn, method, raw, nurl))
            continue
        hit_grant = [u for m, u in granted if m == method and (nurl == u or nurl.startswith(u.rstrip("/") + "/"))]
        if not hit_grant:
            not_granted.append((fn, method, raw, nurl, hit_res[0]))

    ok = len(items) - len(not_registered) - len(not_granted)
    print(f"前端接口 {len(items)} 条：已注册且已授权 {ok}，未注册 {len(not_registered)}，已注册未授权 {len(not_granted)}\n")
    if not_registered:
        print("— 未注册（网关将返回 401）—")
        for fn, method, raw, nurl in not_registered:
            print(f"   {fn:<18} {method:<6} {raw:<52} -> {nurl}")
    if not_granted:
        print("\n— 已注册但未授权给 role100 —")
        for fn, method, raw, nurl, u in not_granted:
            print(f"   {fn:<18} {method:<6} {raw:<52} -> {nurl}  (资源 {u})")

    # 生成补齐 SQL
    lines = ["-- 自动生成的补齐脚本：注册缺失资源 + 授权给 role 100", "USE pd_auth;", "", "SET @now = NOW();", ""]
    for fn, method, raw, nurl in not_registered:
        code = re.sub(r"[^a-zA-Z0-9]+", "_", nurl).strip("_")[:70] + "_" + method.lower()
        lines.append(f"INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) "
                     f"VALUES ('{code}', '{nurl}', '{method}', '{nurl}', '前端{fn}', 0, @now);")
    if not_registered:
        lines.append("")
        lines.append("INSERT INTO pd_auth_role_authority (role_id, authority_id, authority_type) "
                     "SELECT 100, r.id, 'RESOURCE' FROM pd_auth_resource r "
                     "LEFT JOIN pd_auth_role_authority ra ON ra.authority_id=r.id AND ra.authority_type='RESOURCE' "
                     "AND ra.role_id=100 WHERE ra.id IS NULL;")
    p = os.path.join(os.path.dirname(os.path.abspath(__file__)), "前端缺失接口补齐.sql")
    with open(p, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print(f"\n补齐 SQL 已生成: {p}")


if __name__ == "__main__":
    main()
