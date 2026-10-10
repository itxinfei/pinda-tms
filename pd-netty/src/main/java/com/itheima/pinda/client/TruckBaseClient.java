package com.itheima.pinda.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * pd-base 车辆心跳客户端
 *
 * <p>背景：pd-netty 的 pom 未引入 pd-service-base-api 与 spring-cloud-starter-openfeign，
 * 且本次改造禁止改 pom。因此不使用 {@code TruckFeign}，改为通过 Nacos 服务发现
 * （{@link DiscoveryClient}）拿到 pd-base 实例后，用 JDK 自带的
 * {@link RestTemplate}（SimpleClientHttpRequestFactory 基于 HttpURLConnection，
 * 无新增第三方依赖）直接发起 HTTP 调用。</p>
 *
 * <p>语义约定：上报参数 businessId 即车辆主键 id；HTTP 查询参数名保留为
 * {@code deviceGpsId} 以与 pd-base {@code TruckController} 的线上接口保持兼容。</p>
 *
 * @author Claude Code
 * @since 2026-10-07
 */
@Slf4j
@Component
public class TruckBaseClient {

    /**
     * pd-base 在 Nacos 的服务名（pd-base bootstrap.yml: spring.application.name=pd-base）
     */
    private static final String SERVICE_NAME = "pd-base";

    /**
     * 时间统一按 ISO 本地时间传输（pd-base 端用 @DateTimeFormat(iso=DATE_TIME) 接收）
     */
    private static final DateTimeFormatter ISO_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 轮询下标（多实例时简单轮询）
     */
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    private final DiscoveryClient discoveryClient;

    /**
     * RestTemplate：JDK HttpURLConnection 实现，设置短超时防止拖死消费/调度线程
     */
    private final RestTemplate restTemplate;

    public TruckBaseClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    /**
     * 上报心跳：将对应车辆置为在线并刷新心跳时间
     *
     * @param truckId       车辆主键 id（上报消息中的 businessId）
     * @param heartbeatTime 心跳时间（由 pd-netty 服务端生成，防止设备时钟造假）
     * @return true-pd-base 更新成功；false-pd-base 不可用或车辆不存在/已禁用
     */
    public boolean updateHeartbeat(String truckId, LocalDateTime heartbeatTime) {
        String baseUrl = chooseInstanceUrl();
        if (baseUrl == null) {
            return false;
        }
        String url = baseUrl + "/base/truck/heartbeat?deviceGpsId={truckId}&heartbeatTime={heartbeatTime}";
        try {
            String body = restTemplate.exchange(url, HttpMethod.PUT, null, String.class,
                    truckId, heartbeatTime.format(ISO_TIME_FORMATTER)).getBody();
            return isResultSuccess(body);
        } catch (Exception e) {
            log.warn("[GPS心跳] 调用 pd-base 心跳接口失败: truckId={}", truckId, e);
            return false;
        }
    }

    /**
     * 通知 pd-base 将心跳超时车辆置为离线
     *
     * @param threshold 离线阈值（last_heartbeat_time 早于该值的在线车辆置离线）
     * @return 受影响行数；-1 表示调用失败（调用方按降级处理）
     */
    public int markOffline(LocalDateTime threshold) {
        String baseUrl = chooseInstanceUrl();
        if (baseUrl == null) {
            return -1;
        }
        String url = baseUrl + "/base/truck/heartbeat/mark-offline?threshold={threshold}";
        try {
            String body = restTemplate.exchange(url, HttpMethod.PUT, null, String.class,
                    threshold.format(ISO_TIME_FORMATTER)).getBody();
            if (!isResultSuccess(body)) {
                return -1;
            }
            JSONObject jsonObject = JSON.parseObject(body);
            Integer affected = jsonObject.getInteger("data");
            return affected == null ? 0 : affected;
        } catch (Exception e) {
            log.warn("[GPS心跳扫描] 调用 pd-base 置离线接口失败", e);
            return -1;
        }
    }

    /**
     * 从 Nacos 选择一个 pd-base 实例（轮询），返回根地址
     *
     * @return 形如 http://192.168.20.130:8185；无可用实例返回 null
     */
    private String chooseInstanceUrl() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(SERVICE_NAME);
            if (instances == null || instances.isEmpty()) {
                log.debug("[GPS心跳] Nacos 中无可用的 {} 实例（按降级处理）", SERVICE_NAME);
                return null;
            }
            int index = Math.abs(roundRobinIndex.getAndIncrement()) % instances.size();
            ServiceInstance instance = instances.get(index);
            return "http://" + instance.getHost() + ":" + instance.getPort();
        } catch (Exception e) {
            log.warn("[GPS心跳] 从 Nacos 获取 {} 实例失败", SERVICE_NAME, e);
            return null;
        }
    }

    /**
     * 解析 pd-base 统一返回结构，code=0 为成功
     *
     * @param body 响应体
     * @return true-成功
     */
    private boolean isResultSuccess(String body) {
        if (body == null || body.isEmpty()) {
            return false;
        }
        try {
            JSONObject jsonObject = JSON.parseObject(body);
            Integer code = jsonObject.getInteger("code");
            return code != null && code == 0;
        } catch (Exception e) {
            log.warn("[GPS心跳] pd-base 返回内容解析失败: body={}", body);
            return false;
        }
    }
}
