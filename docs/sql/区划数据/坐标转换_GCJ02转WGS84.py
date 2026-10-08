# -*- coding: utf-8 -*-
"""
GCJ02 -> WGS84 坐标转换
用途：pd_area.lng/lat 现存高德 GCJ02，需转为 WGS84（项目约定统一存 WGS84 北斗原始坐标）
算法：逆向单步迭代逼近（近似解 + 3 次迭代收敛，精度 < 1m）

参考：
- 高德 GCJ-02 火星坐标加密算法
- 逆向迭代法（业界通用，优于正向公式直接反解）
"""
import math, sys, csv, io

PI = 3.141592653589793238462
X_PI = PI * 3000.0 / 180.0
A = 6378245.0           # 克拉索夫斯基椭球长半轴
EE = 0.00669342162296594323  # 偏心率平方


def out_of_china(lng, lat):
    return not (72.004 <= lng <= 137.8347 and 0.8293 <= lat <= 55.8271)


def _transform_lat(x, y):
    ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * math.sqrt(abs(x))
    ret += (20.0 * math.sin(6.0 * x * PI) + 20.0 * math.sin(2.0 * x * PI)) * 2.0 / 3.0
    ret += (20.0 * math.sin(y * PI) + 40.0 * math.sin(y / 3.0 * PI)) * 2.0 / 3.0
    ret += (160.0 * math.sin(y / 12.0 * PI) + 320 * math.sin(y * PI / 30.0)) * 2.0 / 3.0
    return ret


def _transform_lng(x, y):
    ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * math.sqrt(abs(x))
    ret += (20.0 * math.sin(6.0 * x * PI) + 20.0 * math.sin(2.0 * x * PI)) * 2.0 / 3.0
    ret += (20.0 * math.sin(x * PI) + 40.0 * math.sin(x / 3.0 * PI)) * 2.0 / 3.0
    ret += (150.0 * math.sin(x / 12.0 * PI) + 300.0 * math.sin(x / 30.0 * PI)) * 2.0 / 3.0
    return ret


def wgs2gcj(wgs_lng, wgs_lat):
    """WGS84 -> GCJ02"""
    if out_of_china(wgs_lng, wgs_lat):
        return wgs_lng, wgs_lat
    dlat = _transform_lat(wgs_lng - 105.0, wgs_lat - 35.0)
    dlng = _transform_lng(wgs_lng - 105.0, wgs_lat - 35.0)
    radlat = wgs_lat / 180.0 * PI
    magic = math.sin(radlat)
    magic = 1 - EE * magic * magic
    sqrtmagic = math.sqrt(magic)
    dlat = (dlat * 180.0) / ((A * (1 - EE)) / (magic * sqrtmagic) * PI)
    dlng = (dlng * 180.0) / (A / sqrtmagic * math.cos(radlat) * PI)
    return wgs_lng + dlng, wgs_lat + dlat


def gcj2wgs(gcj_lng, gcj_lat):
    """GCJ02 -> WGS84（逆向迭代法，3 次收敛）"""
    if out_of_china(gcj_lng, gcj_lat):
        return gcj_lng, gcj_lat
    wgs_lng, wgs_lat = gcj_lng, gcj_lat
    for _ in range(3):
        g_lng, g_lat = wgs2gcj(wgs_lng, wgs_lat)
        wgs_lng += gcj_lng - g_lng
        wgs_lat += gcj_lat - g_lat
    return wgs_lng, wgs_lat


if __name__ == "__main__":
    src = sys.argv[1]
    dst = sys.argv[2]
    n = 0
    max_shift = 0.0
    with io.open(src, encoding="utf-8") as fr, \
         io.open(dst, "w", encoding="utf-8", newline="") as fw:
        fw.write("id,lng,lat\n")
        for line in fr:
            line = line.strip().replace("\r", "")
            if not line or line.startswith("id,"):
                continue
            p = line.split(",")
            if len(p) < 3:
                continue
            rid, lng, lat = p[0], float(p[1]), float(p[2])
            wlng, wlat = gcj2wgs(lng, lat)
            shift = math.hypot(wlng - lng, wlat - lat)
            max_shift = max(max_shift, shift)
            fw.write(u"%s,%.6f,%.6f\n" % (rid, wlng, wlat))
            n += 1
    sys.stderr.write("converted %d rows, max shift %.1f m\n" % (n, max_shift))
    # 自检：往返误差
    t = gcj2wgs(*wgs2gcj(116.407387, 39.904179))
    err = math.hypot(t[0] - 116.407387, t[1] - 39.904179)
    sys.stderr.write("round-trip error: %.6f deg (~%.2f m)\n" % (err, err * 111320))