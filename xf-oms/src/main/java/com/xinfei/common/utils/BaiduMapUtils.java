package com.xinfei.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xinfei.common.config.dynamic.DynamicConfigService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 百度地图操作组件（CFG-01 起由静态工具改为 Spring Bean）。
 *
 * <p>AK 不再在类加载时焊死，而是经 {@link DynamicConfigService} 热读取参数配置表
 * {@code baidu.map.ak}（本地短 TTL 缓存，库为空时回落 -D / 环境变量 / yml）；
 * 百度返回 {@code status!=0}（AK 失效等）时主动 evict，下一次调用即重新读库，
 * 实现后台改 Key 不重启生效。坐标/距离结果仍保留 30 分钟本地业务缓存。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BaiduMapUtils {

    /** 参数配置表中的百度地图服务端 AK 键名 */
    public static final String CONFIG_KEY_BAIDU_AK = "baidu.map.ak";

    private final DynamicConfigService dynamicConfig;

    // 连接/读取超时（毫秒），避免外部服务抖动时请求线程被无限阻塞
    private static final int CONNECT_TIMEOUT = 1000;
    private static final int READ_TIMEOUT = 2000;

    // 地址解析结果本地缓存：降低百度API调用频次（TTL 30分钟，零依赖）
    private static final long CACHE_TTL_MS = 30 * 60 * 1000L;
    private final ConcurrentHashMap<String, CacheValue<String>> coordCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheValue<Double>> distanceCache = new ConcurrentHashMap<>();

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
     * 取当前 AK（库 → Environment）；为空表示尚未配置。
     */
    private String currentAk() {
        return dynamicConfig.get(CONFIG_KEY_BAIDU_AK);
    }

    /**
     * 百度返回失败（status!=0）时丢弃 AK 缓存，使下一次调用立即重新读库（Key 轮换场景）。
     */
    private void onBaiduFailure(String scene) {
        log.warn("百度地图{}接口返回失败，丢弃AK本地缓存以便重新读取配置", scene);
        dynamicConfig.evict(CONFIG_KEY_BAIDU_AK);
    }

    /**
     * 调用百度地图地理编码服务接口，根据地址获取坐标（纬度,经度）
     */
    public String getCoordinate(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }
        CacheValue<String> cached = coordCache.get(address);
        if (cached != null && cached.alive()) {
            return cached.value;
        }
        String coord = doGetCoordinate(address);
        if (coord != null) {
            coordCache.put(address, new CacheValue<>(coord));
        }
        return coord;
    }

    private String doGetCoordinate(String address) {
        String ak = currentAk();
        if (ak == null || ak.isEmpty()) {
            log.warn("未配置百度地图AK（参数键 {}），地理编码跳过", CONFIG_KEY_BAIDU_AK);
            return null;
        }
        String httpUrl = "http://api.map.baidu.com/geocoding/v3/?address=" + address + "&output=json&ak=" + ak;
        String json = loadJSON(httpUrl);
        Map map = JSON.parseObject(json, Map.class);
        if (map == null) {
            return null;
        }

        String status = map.get("status").toString();
        if (status.equals("0")) {
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

        onBaiduFailure("地理编码");
        return null;
    }

    /**
     * 调用百度地图驾车路线规划服务接口，根据寄件人地址和收件人地址坐标计算订单距离
     */
    public Double getDistance(String origin, String destination) {
        if (origin == null || destination == null) {
            return null;
        }
        String cacheKey = origin + "|" + destination;
        CacheValue<Double> cached = distanceCache.get(cacheKey);
        if (cached != null && cached.alive()) {
            return cached.value;
        }
        Double distance = doGetDistance(origin, destination);
        if (distance != null) {
            distanceCache.put(cacheKey, new CacheValue<>(distance));
        }
        return distance;
    }

    private Double doGetDistance(String origin, String destination) {
        String ak = currentAk();
        if (ak == null || ak.isEmpty()) {
            log.warn("未配置百度地图AK（参数键 {}），距离计算跳过", CONFIG_KEY_BAIDU_AK);
            return null;
        }
        String httpUrl = "http://api.map.baidu.com/directionlite/v1/driving?origin="
                + origin + "&destination="
                + destination + "&ak=" + ak;

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

        onBaiduFailure("驾车路线");
        return null;
    }

    /**
     * 调用百度地图驾车路线规划服务接口，根据寄件人地址和收件人地址坐标计算线路耗时（秒）
     */
    public Integer getTime(String origin, String destination) {
        String ak = currentAk();
        if (ak == null || ak.isEmpty()) {
            log.warn("未配置百度地图AK（参数键 {}），耗时计算跳过", CONFIG_KEY_BAIDU_AK);
            return null;
        }
        String httpUrl = "http://api.map.baidu.com/directionlite/v1/driving?origin="
                + origin + "&destination="
                + destination + "&ak=" + ak;

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

        onBaiduFailure("驾车路线");
        return null;
    }

    /**
     * 调用服务接口，返回百度地图服务端的结果（带连接/读取超时保护）
     */
    private String loadJSON(String httpUrl) {
        StringBuilder json = new StringBuilder();
        HttpURLConnection urlConnection = null;
        try {
            URL url = URI.create(httpUrl).toURL();
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
