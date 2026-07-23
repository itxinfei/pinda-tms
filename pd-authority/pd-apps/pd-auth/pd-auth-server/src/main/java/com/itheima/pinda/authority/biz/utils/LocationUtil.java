package com.itheima.pinda.authority.biz.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * 根据地址获取经纬度工具类
 */
@Slf4j
@Component
public class LocationUtil {
    @Value("${location.ak}")
    private String ak;
    public static final String KEY_LAT = "lat";
    public static final String KEY_LNG = "lng";

    /**
     * 返回输入地址的经纬度坐标 key lng(经度),lat(纬度)
     *
     * @param address 输入地址
     * @return 经纬度映射，失败返回null
     */
    public Map<String, String> getLatitude(String address) {
        String ak = this.ak;
        if (ak == null || ak.trim().isEmpty()) {
            log.error("location.ak未配置，无法调用百度地图API");
            return null;
        }
        try {
            address = URLEncoder.encode(address, "UTF-8");

            URL resjson = new URL(String.format("https://api.map.baidu.com/geocoding/v3/?&output=json&address=%s&ak=%s", address, ak));
            try (BufferedReader in = new BufferedReader(new InputStreamReader(resjson.openStream()))) {
                String res;
                StringBuilder sb = new StringBuilder();
                while ((res = in.readLine()) != null) {
                    sb.append(res.trim());
                }
                String str = sb.toString();
                if (str != null && !str.isEmpty()) {
                    int lngStart = str.indexOf("\"lng\"");
                    int lngEnd = str.indexOf(",\"lat\"");
                    int latEnd = str.indexOf("},\"precise");
                    if (lngStart > 0 && lngEnd > 0 && latEnd > 0) {
                        String lng = str.substring(lngStart + 6, lngEnd);
                        String lat = str.substring(lngEnd + 7, latEnd);
                        Map<String, String> map = new HashMap<>();
                        map.put(KEY_LNG, lng);
                        map.put(KEY_LAT, lat);
                        return map;
                    }
                }
            }
        } catch (Exception e) {
            log.error("地址解析失败: address={}", address, e);
        }
        return null;
    }
}
