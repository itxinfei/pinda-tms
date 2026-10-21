package com.xinfei.common.geo;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 百度地图地理服务实现（批3 收敛：原 {@code BaiduMapUtils} 与 {@code EntCoordSyncJob}
 * 两套 HTTP 调用合并至此，全仓百度 Web 服务外呼唯一出口）。
 *
 * <p><b>AK 链路</b>：静态读取器由 {@code BaiduAkBridge} 注入「参数库 sys_config →
 * Environment(-D/环境变量)」（BFF 侧覆盖为 ConfigFeign 链），与原静态工具同链；
 * 未装配时回落 JVM {@code -Dbaidu.map.ak}。</p>
 *
 * <p><b>坐标序</b>：接口契约 {@code lng,lat}；百度 Web 服务 URL 入参为 {@code lat,lng}，
 * 仅在 {@link #toBaiduOrder} 拼 URL 边界交换一次。</p>
 *
 * <p><b>行为保持（等价变换）</b>：连接/读取超时 1000/2000ms、30 分钟本地缓存、
 * {@code status!=0} 时 evict AK 并返回 null（后台改 Key 不重启生效）、日志 maskAk 脱敏。</p>
 */
@Slf4j
@Component("baiduGeoClient")
public class BaiduGeoClient implements GeoClient {

    /** 参数配置表中的百度地图服务端 AK 键名（与 BaiduAkBridge 同键） */
    public static final String CONFIG_KEY_BAIDU_AK = "baidu.map.ak";

    /**
     * AK 动态读取器（Spring 装配入口，业务代码勿调）。
     * 每次调用现取，后台改库后最长一个缓存 TTL 即生效。
     */
    private static volatile Supplier<String> akReader;

    /** 百度返回失败（status!=0）时的 AK 缓存清除动作（服务侧=DynamicConfigService.evict，BFF 侧=no-op） */
    private static volatile Consumer<String> akEvict = key -> { };

    /** Spring 装配入口（由 BaiduAkBridge 调用，业务代码勿调） */
    public static void setAkReader(Supplier<String> reader, Consumer<String> evict) {
        akReader = reader;
        akEvict = evict != null ? evict : key -> { };
    }

    /** 取当前生效的 AK：动态链路 → -D；为空表示尚未配置 */
    static String currentAk() {
        Supplier<String> reader = akReader;
        return reader != null ? reader.get() : System.getProperty(CONFIG_KEY_BAIDU_AK, "");
    }

    // 连接/读取超时（毫秒），避免外部服务抖动时请求线程被无限阻塞（原两套实现同值）
    private static final int CONNECT_TIMEOUT = 1000;
    private static final int READ_TIMEOUT = 2000;

    // 本地业务缓存：降低百度API调用频次（TTL 30分钟，零依赖，原 BaiduMapUtils 同值）
    private static final long CACHE_TTL_MS = 30 * 60 * 1000L;
    private final ConcurrentHashMap<String, CacheValue<String>> coordCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheValue<Double>> distanceCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheValue<Map<String, Object>>> reverseCache = new ConcurrentHashMap<>();

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
     * 地理编码（地址 → "lng,lat"，WGS84）。
     * 地址预处理沿原 EntCoordSyncJob 语义（去全部空白、「#」→「栋」）。
     * 百度原生返回 BD09（显式 ret_coordtype=bd09ll 声明），内部转 WGS84 作为契约坐标
     * （2026-10-17 拍板修复：此前 BD09 结果被当 WGS84 喂逆地理/区域校验，存在百米级偏差）。
     */
    @Override
    public String geocode(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }
        address = address.replaceAll("\\s*", "").replace("#", "栋");
        CacheValue<String> cached = coordCache.get(address);
        if (cached != null && cached.alive()) {
            return cached.value;
        }
        String coord = doGeocode(address);
        if (coord != null) {
            coordCache.put(address, new CacheValue<>(coord));
        }
        return coord;
    }

    private String doGeocode(String address) {
        String ak = currentAk();
        if (ak == null || ak.isEmpty()) {
            log.warn("未配置百度地图AK（参数键 {}），地理编码跳过", CONFIG_KEY_BAIDU_AK);
            return null;
        }
        String httpUrl = "http://api.map.baidu.com/geocoding/v3/?address=" + address
                + "&output=json&ret_coordtype=bd09ll&ak=" + ak;
        String json = loadJSON(httpUrl);
        Map map = JSON.parseObject(json, Map.class);
        if (map == null) {
            return null;
        }
        if ("0".equals(map.getOrDefault("status", "500").toString())) {
            Map result = (Map) map.get("result");
            Map location = (Map) result.get("location");
            double bdLng = Double.parseDouble(location.get("lng").toString());
            double bdLat = Double.parseDouble(location.get("lat").toString());
            double[] wgs = bd09ToWgs84(bdLng, bdLat);
            DecimalFormat df = new DecimalFormat("#.######");
            return df.format(wgs[0]) + "," + df.format(wgs[1]);
        }
        onBaiduFailure("地理编码");
        return null;
    }

    /**
     * 逆地理编码（"lng,lat" → addressComponent Map，WGS84 输入）。
     * 沿原 EntCoordSyncJob 语义：入参交换为百度要求的 lat,lng 拼 URL、coordtype=wgs84ll、
     * 结果附 formatted_address；新增 30 分钟本地缓存（仅成功入缓存，原无缓存调用方外呼频次下降，结果幂等不变）。
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> reverseGeocode(String lngLat) {
        if (lngLat == null || lngLat.trim().isEmpty()) {
            return null;
        }
        CacheValue<Map<String, Object>> cached = reverseCache.get(lngLat);
        if (cached != null && cached.alive()) {
            return cached.value;
        }
        Map<String, Object> result = doReverseGeocode(lngLat);
        if (result != null) {
            reverseCache.put(lngLat, new CacheValue<>(result));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> doReverseGeocode(String lngLat) {
        String ak = currentAk();
        if (ak == null || ak.isEmpty()) {
            log.warn("未配置百度地图AK（参数键 {}），逆地理编码跳过", CONFIG_KEY_BAIDU_AK);
            return null;
        }
        String location = toBaiduOrder(lngLat);
        String httpUrl = "http://api.map.baidu.com/reverse_geocoding/v3/?ak=" + ak
                + "&output=json&coordtype=wgs84ll&location=" + location;
        String json = loadJSON(httpUrl);
        Map map = JSON.parseObject(json, Map.class);
        if (map == null) {
            return null;
        }
        if ("0".equals(map.getOrDefault("status", "500").toString())) {
            Map childMap = (Map) map.get("result");
            Map areaMap = (Map) childMap.get("addressComponent");
            areaMap.put("formatted_address", childMap.getOrDefault("formatted_address", "").toString());
            return areaMap;
        }
        onBaiduFailure("逆地理编码");
        return null;
    }

    /**
     * 驾车路线距离（米，计费距离源 D-66 ②）。
     * 契约序 lng,lat（WGS84）入参，内部先转百度原生 BD09 再交换为 URL 序 lat,lng——
     * 转换后喂入 directionlite 的坐标与历史（BD09 直喂）口径等价，计费距离行为保持。
     */
    @Override
    public Double drivingDistance(String origin, String destination) {
        if (origin == null || destination == null) {
            return null;
        }
        String cacheKey = origin + "|" + destination;
        CacheValue<Double> cached = distanceCache.get(cacheKey);
        if (cached != null && cached.alive()) {
            return cached.value;
        }
        Double distance = doDrivingDistance(origin, destination);
        if (distance != null) {
            distanceCache.put(cacheKey, new CacheValue<>(distance));
        }
        return distance;
    }

    private Double doDrivingDistance(String origin, String destination) {
        String ak = currentAk();
        if (ak == null || ak.isEmpty()) {
            log.warn("未配置百度地图AK（参数键 {}），距离计算跳过", CONFIG_KEY_BAIDU_AK);
            return null;
        }
        String httpUrl = "http://api.map.baidu.com/directionlite/v1/driving?origin="
                + toBaiduOrder(toBd09(origin)) + "&destination="
                + toBaiduOrder(toBd09(destination)) + "&ak=" + ak;
        String json = loadJSON(httpUrl);
        Map map = JSON.parseObject(json, Map.class);
        if (map == null) {
            return null;
        }
        if ("0".equals(map.getOrDefault("status", "500").toString())) {
            Map childMap = (Map) map.get("result");
            JSONArray jsonArray = (JSONArray) childMap.get("routes");
            JSONObject jsonObject = (JSONObject) jsonArray.get(0);
            return Double.parseDouble(jsonObject.get("distance") == null ? "0" : jsonObject.get("distance").toString());
        }
        onBaiduFailure("驾车路线");
        return null;
    }

    /**
     * 契约序 "lng,lat" → 百度 Web 服务 URL 入参序 "lat,lng"（全仓唯一交换点）。
     * 格式非法时抛 IllegalArgumentException（与原 EntCoordSyncJob.parseCoordinate 同语义，
     * 由各调用方既有 catch 兜底）。
     */
    private static String toBaiduOrder(String lngLat) {
        String[] parts = lngLat.split(",");
        if (parts.length < 2) {
            throw new IllegalArgumentException("坐标格式必须为 'lng,lat': " + lngLat);
        }
        return parts[1] + "," + parts[0];
    }

    /** 契约序 "lng,lat"（WGS84）→ BD09 序 "lng,lat"（喂百度 directionlite 前转换） */
    private static String toBd09(String lngLat) {
        String[] parts = lngLat.split(",");
        if (parts.length < 2) {
            throw new IllegalArgumentException("坐标格式必须为 'lng,lat': " + lngLat);
        }
        double[] bd = wgs84ToBd09(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()));
        return bd[0] + "," + bd[1];
    }

    // ===== 坐标系转换（eviltransform 标准公式；与 xfwl-netty CoordTransform 同源，后续可收敛下沉 common） =====

    private static final double GCJ_A = 6378245.0;
    private static final double GCJ_EE = 0.00669342162296594323;
    private static final double BD_X_PI = Math.PI * 3000.0 / 180.0;

    private static boolean outOfChina(double lng, double lat) {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
                || (lng < 104.005 && lat > 24.632)
                || (lng < 111.667 && lat > 42.353)
                || (lng > 122.928 && lat < 29.618)
                || (lng < 120.86 && lat < 19.902)
                || (lng > 121.257 && lat < 21.234);
    }

    private static double[] wgs84ToGcj02(double lng, double lat) {
        if (outOfChina(lng, lat)) {
            return new double[]{lng, lat};
        }
        double[] delta = gcj02Offset(lng, lat);
        return new double[]{lng + delta[0], lat + delta[1]};
    }

    private static double[] gcj02ToWgs84(double lng, double lat) {
        if (outOfChina(lng, lat)) {
            return new double[]{lng, lat};
        }
        double[] delta = gcj02Offset(lng, lat);
        return new double[]{lng - delta[0], lat - delta[1]};
    }

    private static double[] gcj02ToBd09(double lng, double lat) {
        double z = Math.sqrt(lng * lng + lat * lat) + 0.00002 * Math.sin(lat * BD_X_PI);
        double theta = Math.atan2(lat, lng) + 0.000003 * Math.cos(lng * BD_X_PI);
        return new double[]{z * Math.cos(theta) + 0.0065, z * Math.sin(theta) + 0.006};
    }

    private static double[] bd09ToGcj02(double lng, double lat) {
        double x = lng - 0.0065;
        double y = lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * BD_X_PI);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * BD_X_PI);
        return new double[]{z * Math.cos(theta), z * Math.sin(theta)};
    }

    private static double[] wgs84ToBd09(double lng, double lat) {
        double[] gcj = wgs84ToGcj02(lng, lat);
        return gcj02ToBd09(gcj[0], gcj[1]);
    }

    private static double[] bd09ToWgs84(double lng, double lat) {
        double[] gcj = bd09ToGcj02(lng, lat);
        return gcj02ToWgs84(gcj[0], gcj[1]);
    }

    private static double[] gcj02Offset(double lng, double lat) {
        double dLat = transformLat(lng - 105.0, lat - 35.0);
        double dLng = transformLng(lng - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * Math.PI;
        double magic = Math.sin(radLat);
        magic = 1 - GCJ_EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((GCJ_A * (1 - GCJ_EE)) / (magic * sqrtMagic) * Math.PI);
        dLng = (dLng * 180.0) / (GCJ_A / sqrtMagic * Math.cos(radLat) * Math.PI);
        return new double[]{dLng, dLat};
    }

    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double transformLng(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;
        return ret;
    }

    /** 百度返回失败（status!=0）时清除 AK 缓存，使下一次调用立即重新读取配置（Key 轮换场景） */
    private void onBaiduFailure(String scene) {
        log.warn("百度地图{}接口返回失败，丢弃AK本地缓存以便重新读取配置", scene);
        akEvict.accept(CONFIG_KEY_BAIDU_AK);
    }

    /**
     * 调用百度服务接口，返回结果 JSON（带连接/读取超时保护）
     */
    private String loadJSON(String httpUrl) {
        StringBuilder json = new StringBuilder();
        HttpURLConnection urlConnection = null;
        try {
            java.net.URL url = URI.create(httpUrl).toURL();
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
            log.error("百度地图API URL异常: {}", maskAk(httpUrl), e);
            return "";
        } catch (IOException e) {
            log.error("百度地图API请求IO异常: {}", maskAk(httpUrl), e);
            return "";
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        log.debug("百度地图响应: {}", json.length());
        return json.toString();
    }

    /**
     * 日志脱敏：URL 中 ak 参数值替换为 ***，防真实第三方凭证进日志
     */
    static String maskAk(String url) {
        return url == null ? null : url.replaceAll("(ak=)[^&]*", "$1***");
    }
}
