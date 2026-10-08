# -*- coding: utf-8 -*-
"""用 pypinyin 生成区划全量拼音 + 首字母（阿里云镜像装的 pypinyin）"""
import sys, csv, io, re
from pypinyin import lazy_pinyin, Style

src, dst = sys.argv[1], sys.argv[2]

PAREN = re.compile(r"[（(]([^）)]*)[）)]")

def full(name):
    return "".join(lazy_pinyin(name)).lower()

def first(name):
    return "".join(lazy_pinyin(name, style=Style.FIRST_LETTER)).lower()

def clean(name):
    """去掉括号及其内容（含全角/半角），并把非字母字符（连字符等）剔除。
    园区类名称如 '大安经济开发区（省级）' -> '大安经济开发区'
                '化学新材料产业园-沿江街道' -> '化学新材料产业园沿江街道'"""
    s = PAREN.sub("", name)
    s = s.replace("　", "").strip()
    # 剔除所有非汉字/非字母字符（连字符、间隔号、空格等）
    s = re.sub(r"[^\w\u4e00-\u9fff]", "", s, flags=re.UNICODE)
    return s

n = 0
bad = []
with io.open(src, encoding="utf-8") as fr, \
     io.open(dst, "w", encoding="utf-8", newline="") as fw:
    fw.write("id,pinyin,first\n")
    for line in fr:
        line = line.strip().replace("\r", "")
        if not line:
            continue
        c = line.split(",", 1)
        if len(c) < 2:
            continue
        rid, raw = c[0].strip(), c[1].strip()
        name = clean(raw)
        py, fi = full(name), first(name)
        # 质量校验：必须是纯小写字母
        if not py.isalpha() or not py.isascii() or not fi.isalpha() or not fi.isascii():
            bad.append((rid, raw, py, fi))
            continue
        fw.write(u"{0},{1},{2}\n".format(rid, py, fi))
        n += 1

sys.stderr.write("generated %d rows, skipped %d\n" % (n, len(bad)))
for b in bad[:10]:
    sys.stderr.write("  SKIP %s\n" % (b,))