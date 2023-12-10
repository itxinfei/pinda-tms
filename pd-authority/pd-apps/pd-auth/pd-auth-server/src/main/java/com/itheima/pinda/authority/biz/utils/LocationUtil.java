package com.itheima.pinda.authority.biz.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Component
public class LocationUtil {
    @Value("${location.ak}")
    private String AK;
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
    public Map<String, String> getLatitude(String address) {
        try {
            // 将地址转换成utf-8的16进制
            address = URLEncoder.encode(address, "UTF-8");

            URL resjson = new URL(String.format("http://api.map.baidu.com/geocoding/v3/?&output=json&address=%s&ak=%s", address, AK));
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
            e.printStackTrace();
        }
        return null;
    }
}
