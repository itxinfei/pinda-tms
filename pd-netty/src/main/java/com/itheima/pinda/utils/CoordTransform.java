package com.itheima.pinda.utils;

import java.util.Locale;

/**
 * 坐标系转换工具（WGS84 / GCJ02 / BD09）
 *
 * <p>纯 Java 实现，无任何第三方依赖。采用互联网公开的标准转换公式（eviltransform）：
 * <ul>
 *   <li>WGS84：GPS 原始坐标系，GPS/北斗终端原始输出；CGCS2000 民用坐标与 WGS84
 *       在米级以下无实际差异，本系统按 WGS84 处理。</li>
 *   <li>GCJ02：国测局火星坐标系（高德/腾讯地图）。</li>
 *   <li>BD09：百度坐标系（本系统存库统一口径，与后端 BaiduMapUtils 一致）。</li>
 * </ul>
 * 中国境外 WGS84→GCJ02 不加偏移（GCJ02 仅在中国大陆有效）；GCJ02→BD09 为纯数学
 * 公式（其中含百度固定常量偏移），境外结果无实际业务意义。本系统两个上报入口均已
 * 拒绝境外坐标，CoordTransform 实际只处理境内数据。</p>
 *
 * <p>注意：GCJ02 与 BD09 之间不可逆，反向转换为近似值；本系统设备上报只做正向转换。</p>
 *
 * @author Claude Code
 * @since 2026-10-07
 */
public final class CoordTransform {

    private CoordTransform() {
    }

    /**
     * 圆周率
     */
    private static final double PI = Math.PI;

    /**
     * 长半轴（Krasovsky 1940 / GCJ02 算法常量）
     */
    private static final double A = 6378245.0;

    /**
     * 偏心率平方
     */
    private static final double EE = 0.00669342162296594323;

    /**
     * BD09 转换常量：PI * 3000 / 180
     */
    private static final double X_PI = PI * 3000.0 / 180.0;

    /**
     * 判断坐标是否在中国境外（标准 eviltransform 判断，含边境线细化矩形）
     *
     * @param lng 经度
     * @param lat 纬度
     * @return true-境外（不加偏移）
     */
    public static boolean outOfChina(double lng, double lat) {
        // 基础包围盒
        if (lng < 72.004 || lng > 137.8347) {
            return true;
        }
        if (lat < 0.8293 || lat > 55.8271) {
            return true;
        }
        // 细化排除：边境线外但落在基础包围盒内的区域
        if (lng < 104.005 && lat > 24.632) {
            return true;
        }
        if (lng < 111.667 && lat > 42.353) {
            return true;
        }
        if (lng > 122.928 && lat < 29.618) {
            return true;
        }
        if (lng < 120.86 && lat < 19.902) {
            return true;
        }
        if (lng > 121.257 && lat < 21.234) {
            return true;
        }
        return false;
    }

    /**
     * WGS84 转 GCJ02
     *
     * @param lng WGS84 经度
     * @param lat WGS84 纬度
     * @return 长度 2 的数组：[GCJ02 经度, GCJ02 纬度]；境外原样返回
     */
    public static double[] wgs84ToGcj02(double lng, double lat) {
        if (outOfChina(lng, lat)) {
            return new double[]{lng, lat};
        }
        double dLat = transformLat(lng - 105.0, lat - 35.0);
        double dLng = transformLng(lng - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
        return new double[]{lng + dLng, lat + dLat};
    }

    /**
     * GCJ02 转 BD09
     *
     * @param lng GCJ02 经度
     * @param lat GCJ02 纬度
     * @return 长度 2 的数组：[BD09 经度, BD09 纬度]
     */
    public static double[] gcj02ToBd09(double lng, double lat) {
        double z = Math.sqrt(lng * lng + lat * lat) + 0.00002 * Math.sin(lat * X_PI);
        double theta = Math.atan2(lat, lng) + 0.000003 * Math.cos(lng * X_PI);
        double bdLng = z * Math.cos(theta) + 0.0065;
        double bdLat = z * Math.sin(theta) + 0.006;
        return new double[]{bdLng, bdLat};
    }

    /**
     * WGS84 转 BD09（先转 GCJ02 再转 BD09）
     *
     * @param lng WGS84 经度
     * @param lat WGS84 纬度
     * @return 长度 2 的数组：[BD09 经度, BD09 纬度]；境外原样返回
     */
    public static double[] wgs84ToBd09(double lng, double lat) {
        double[] gcj = wgs84ToGcj02(lng, lat);
        return gcj02ToBd09(gcj[0], gcj[1]);
    }

    /**
     * 纬度偏移量计算（GCJ02 标准公式）
     *
     * @param x 经度相对 105 的偏移
     * @param y 纬度相对 35 的偏移
     * @return 纬度偏移量
     */
    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 经度偏移量计算（GCJ02 标准公式）
     *
     * @param x 经度相对 105 的偏移
     * @param y 纬度相对 35 的偏移
     * @return 经度偏移量
     */
    private static double transformLng(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 坐标保留 6 位小数转字符串（强制 Locale.ROOT，防止部分区域小数点被格式化为逗号）
     *
     * @param value 经度或纬度
     * @return 固定 6 位小数字符串
     */
    public static String toFixed6(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }
}
