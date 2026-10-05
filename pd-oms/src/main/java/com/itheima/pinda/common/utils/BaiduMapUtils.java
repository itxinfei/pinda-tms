package com.itheima.pinda.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

/**
 * 百度地图操作工具类
 */
@Slf4j
public class BaiduMapUtils {
    public static void main(String[] args) {
        String origin = getCoordinate("北京市育新花园小区");
        String destination = getCoordinate("北京市百度大厦");
        Double distance = getDistance(origin, destination);
        log.debug("订单距离：{}米", distance);
        Integer time = getTime(origin, destination);
        log.debug("线路耗时{}秒", time);
    }

    // API密钥通过配置注入，禁止硬编码AK到源码
    // 可在启动时通过 -D参数传入：-Dbaidu.map.ak=xxx
    private static String AK = System.getProperty("baidu.map.ak", "");

    // 连接/读取超时（毫秒），避免外部服务抖动时请求线程被无限阻塞
    private static final int CONNECT_TIMEOUT = 1000;
    private static final int READ_TIMEOUT = 2000;

    // 地址解析结果本地缓存：降低百度API调用频次（TTL 30分钟，零依赖）
    private static final long CACHE_TTL_MS = 30 * 60 * 1000L;
    private static final ConcurrentHashMap<String, CacheValue<String>> COORD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CacheValue<Double>> DISTANCE_CACHE = new ConcurrentHashMap<>();

    private static final class CacheValue<T> {
        final T value;
        final long expireAt;
        CacheValue(T value) {
            this.value = value;
            this.expireAt = System.currentTimeMillis() + CACHE_TTL_MS;
        }
        boolean alive() {
            return System.currentTimeMillis() < expireAt;
        }
    }

    /**
     * 调用百度地图地理编码服务接口，根据地址获取坐标（经度、纬度）
     * @param address
     * @return
     */
    public static String getCoordinate(String address){
        if (address == null || address.trim().isEmpty()) {
            return null;
        }
        CacheValue<String> cached = COORD_CACHE.get(address);
        if (cached != null && cached.alive()) {
            return cached.value;
        }
        String coord = doGetCoordinate(address);
        if (coord != null) {
            COORD_CACHE.put(address, new CacheValue<>(coord));
        }
        return coord;
    }

    private static String doGetCoordinate(String address){
        String httpUrl = "http://api.map.baidu.com/geocoding/v3/?address=" + address + "&output=json&ak=" + AK;
        String json = loadJSON(httpUrl);
        Map map = JSON.parseObject(json, Map.class);
        if (map == null) {
            return null;
        }

        String status = map.get("status").toString();
        if(status.equals("0")){
            //返回结果成功，能够正常解析地址信息
            Map result = (Map) map.get("result");
            Map location = (Map) result.get("location");
            String lng = location.get("lng").toString();
            String lat = location.get("lat").toString();

            DecimalFormat df = new DecimalFormat("#.######");
            String lngStr = df.format(Double.parseDouble(lng));
            String latStr = df.format(Double.parseDouble(lat));
            return latStr + "," + lngStr;
        }

        return null;
    }

    /**
     * 调用百度地图驾车路线规划服务接口，根据寄件人地址和收件人地址坐标计算订单距离
     * @param origin
     * @param destination
     * @return
     */
    public static Double getDistance(String origin,String destination){
        if (origin == null || destination == null) {
            return null;
        }
        String cacheKey = origin + "|" + destination;
        CacheValue<Double> cached = DISTANCE_CACHE.get(cacheKey);
        if (cached != null && cached.alive()) {
            return cached.value;
        }
        Double distance = doGetDistance(origin, destination);
        if (distance != null) {
            DISTANCE_CACHE.put(cacheKey, new CacheValue<>(distance));
        }
        return distance;
    }

    private static Double doGetDistance(String origin,String destination){
        String httpUrl = "http://api.map.baidu.com/directionlite/v1/driving?origin="
                +origin+"&destination="
                +destination+"&ak=" + AK;

        String json = loadJSON(httpUrl);
        Map map = JSON.parseObject(json, Map.class);
        if (map == null) {
            return null;
        }
        if ("0".equals(map.getOrDefault("status", "500").toString())) {
            Map childMap = (Map) map.get("result");
            JSONArray jsonArray = (JSONArray) childMap.get("routes");
            JSONObject jsonObject = (JSONObject) jsonArray.get(0);
            double distance = Double.parseDouble(jsonObject.get("distance") == null ? "0" : jsonObject.get("distance").toString());
            return distance;
        }

        return null;
    }

    /**
     * 调用百度地图驾车路线规划服务接口，根据寄件人地址和收件人地址坐标计算线路耗时
     * @param origin
     * @param destination
     * @return
     */
    public static Integer getTime(String origin,String destination){
        String httpUrl = "http://api.map.baidu.com/directionlite/v1/driving?origin="
                +origin+"&destination="
                +destination+"&ak=" + AK;

        String json = loadJSON(httpUrl);
        Map map = JSON.parseObject(json, Map.class);
        if (map == null) {
            return null;
        }
        if ("0".equals(map.getOrDefault("status", "500").toString())) {
            Map childMap = (Map) map.get("result");
            JSONArray jsonArray = (JSONArray) childMap.get("routes");
            JSONObject jsonObject = (JSONObject) jsonArray.get(0);
            int time = Integer.parseInt(jsonObject.get("duration") == null ? "0" : jsonObject.get("duration").toString());
            return time;
        }

        return null;
    }

    /**
     * 调用服务接口，返回百度地图服务端的结果（带连接/读取超时保护）
     * @param httpUrl
     * @return
     */
    public static String loadJSON(String httpUrl){
        StringBuilder json = new StringBuilder();
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(httpUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);
            urlConnection.setRequestMethod("GET");
            try (BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    json.append(inputLine);
                }
            }
        } catch (MalformedURLException e) {
            log.error("百度地图API URL异常: {}", httpUrl, e);
            return "";
        } catch (IOException e) {
            log.error("百度地图API请求IO异常: {}", httpUrl, e);
            return "";
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        log.debug(json.toString());
        return json.toString();
    }
}
