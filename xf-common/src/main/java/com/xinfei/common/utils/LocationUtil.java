package com.xinfei.common.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocationUtil {
    // AK 动态读取（CFG-01）：Spring 装配后由 BaiduAkBridge 注入「参数库 → Environment」链路；
    // 未注入时回落 JVM -Dbaidu.map.ak，行为与旧版等价
    private static volatile java.util.function.Supplier<String> akSupplier;

    /** Spring 装配入口（由 xf-common 自动装配调用，业务代码勿调） */
    public static void setAkSupplier(java.util.function.Supplier<String> supplier) {
        akSupplier = supplier;
    }

    private static String currentAk() {
        java.util.function.Supplier<String> supplier = akSupplier;
        return supplier != null ? supplier.get()
                : System.getProperty(com.xinfei.common.utils.EntCoordSyncJob.CONFIG_KEY_BAIDU_AK, "");
    }

    public static final String KEY_LAT = "lat";
    public static final String KEY_LNG = "lng";
    /**
     * 返回输入地址的经纬度坐标 key lng(经度),lat(纬度)
     */
    /**
     * 返回输入地址的经纬度
     * @param address 输入地址
     * @return
     */
    public static Map<String, String> getLatitude(String address) {
        try {
            // 将地址转换成utf-8的16进制
            address = URLEncoder.encode(address, "UTF-8");

            URL resjson = URI.create(String.format("http://api.map.baidu.com/geocoding/v3/?&output=json&address=%s&ak=%s", address, currentAk())).toURL();
            BufferedReader in = new BufferedReader(new InputStreamReader(resjson.openStream()));
            String res;
            StringBuilder sb = new StringBuilder("");
            while ((res = in.readLine()) != null) {
                sb.append(res.trim());
            }
            in.close();
            String str = sb.toString();
            if (str != null && !str.equals("")) {
                Map<String, String> map = null;
                int lngStart = str.indexOf("\"lng\"");
                int lngEnd = str.indexOf(",\"lat\"");
                int latEnd = str.indexOf("},\"precise");
                if (lngStart > 0 && lngEnd > 0 && latEnd > 0) {
                    String lng = str.substring(lngStart + 6, lngEnd);
                    String lat = str.substring(lngEnd + 7, latEnd);
                    map = new HashMap<>();
                    map.put(KEY_LNG, lng);
                    map.put(KEY_LAT, lat);
                    return map;
                }
            }
        } catch (Exception e) {
            log.error("地址解析失败", e);
        }
        return null;
    }
}
