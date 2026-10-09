# -*- coding: utf-8 -*-
"""正确抽取 controller 端点: class @RequestMapping + method @Mapping 组合。
输出 resource 应使用的最终 url = 控制器全路径去掉网关前缀段(若存在)。
"""
import re, glob

SERVICES = {
    "pd-base": ("D:/MyCode/pinda-tms/pd-base/src/main/java", "base"),
    "pd-web-manager": ("D:/MyCode/pinda-tms/pd-web/pd-web-manager/src/main/java", "web-manager"),
    "pd-oms": ("D:/MyCode/pinda-tms/pd-oms/src/main/java", "oms"),
    "pd-work": ("D:/MyCode/pinda-tms/pd-work/src/main/java", "work"),
    "pd-dispatch": ("D:/MyCode/pinda-tms/pd-dispatch/src/main/java", "dispatch"),
    "pd-user": ("D:/MyCode/pinda-tms/pd-user/src/main/java", "user"),
    "pd-aggregation": ("D:/MyCode/pinda-tms/pd-aggregation/src/main/java", "aggregation"),
}

METHOD_ANN = {
    "GetMapping": "GET", "PostMapping": "POST", "PutMapping": "PUT",
    "DeleteMapping": "DELETE", "PatchMapping": "PATCH",
}

def first_class_mapping(txt):
    # 类级别 @RequestMapping("X") —— 取文件中第一个出现的 RequestMapping 作为类前缀
    m = re.search(r'@RequestMapping\(\s*["\']([^"\']+)["\']', txt)
    return m.group(1).strip() if m else ""

def method_mappings(txt):
    out = []
    # 找到所有方法级注解块
    for mm in re.finditer(r'@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|RequestMapping)\s*\(([^)]*)\)', txt):
        ann, args = mm.group(1), mm.group(2)
        pm = re.search(r'(?:value|path)\s*=\s*\{?\s*["\']([^"\']+)["\']', args)
        if not pm:
            pm = re.search(r'["\']([^"\']+)["\']', args)
        path = pm.group(1).strip() if pm else ""
        if ann == "RequestMapping":
            meth = re.search(r'RequestMethod\.(\w+)', args)
            method = meth.group(1).upper() if meth else "GET"
        else:
            method = METHOD_ANN[ann]
        out.append((method, path))
    return out

def combine(class_path, method_path):
    class_path = (class_path or "").strip().strip("/")
    method_path = (method_path or "").strip().strip("/")
    if not method_path:
        return "/" + class_path if class_path else "/"
    if class_path:
        return "/" + class_path + "/" + method_path
    return "/" + method_path

for svc, (root, prefix) in SERVICES.items():
    print(f"\n===== {svc} (gateway prefix=/{prefix}) =====")
    all_eps = []
    for f in glob.glob(root + "/**/*.java", recursive=True):
        if "controller" not in f.lower():
            continue
        txt = open(f, encoding="utf-8", errors="ignore").read()
        cp = first_class_mapping(txt)
        for method, mp in method_mappings(txt):
            full = combine(cp, mp)
            all_eps.append((method, full))
    # 去掉网关前缀段(若存在)，得到 resource.url
    fixed = set()
    for method, full in all_eps:
        parts = full.lstrip("/").split("/")
        if parts and parts[0] == prefix:
            url = "/" + "/".join(parts[1:])
        else:
            url = full
        fixed.add((method, url))
    for method, url in sorted(fixed):
        print(f"   {method:7} {url}")
    print(f"   total: {len(fixed)}")
