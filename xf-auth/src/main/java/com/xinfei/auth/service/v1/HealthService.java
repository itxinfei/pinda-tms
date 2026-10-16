package com.xinfei.auth.service.v1;

import com.xinfei.auth.common.v1.HealthVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 服务健康聚合（服务健康页）：从 Nacos 发现 xf-* 服务实例，
 * 并行探测各实例 /actuator/health（部署探针同款无鉴权端点，超时即 DOWN）。
 *
 * <p>JDK 21：用 HttpClient + parallelStream（IO 密集，底层虚拟线程调度），
 * 14 服务并行探测单请求墙钟 ≈ 单次探测耗时。</p>
 */
@Slf4j
@Service
public class HealthService {

    /** 单实例探测超时（与 compose HTTP 健康探针节奏同量级） */
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(2);

    private final DiscoveryClient discoveryClient;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(PROBE_TIMEOUT)
            .build();

    public HealthService(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    /** 全量服务健康列表（实时探测，不缓存——健康页要的是当下） */
    public List<HealthVO> list(String keywords) {
        List<HealthVO> list = discoveryClient.getServices().parallelStream()
                .filter(name -> name.startsWith("xf-"))
                .map(this::probe)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        list.sort(Comparator.comparing(HealthVO::getServiceName));
        if (StringUtils.hasText(keywords)) {
            String kw = keywords.trim();
            list.removeIf(v -> !v.getServiceName().contains(kw));
        }
        return list;
    }

    /** 探测单个服务的全部实例：任一实例 UP 即服务 UP */
    private HealthVO probe(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        HealthVO vo = new HealthVO();
        vo.setServiceName(serviceName);
        vo.setInstances(instances.size());
        if (instances.isEmpty()) {
            vo.setStatus("DOWN");
            vo.setInstanceList("");
            vo.setLatency(null);
            return vo;
        }
        vo.setInstanceList(instances.stream()
                .map(i -> i.getHost() + ":" + i.getPort())
                .reduce((a, b) -> a + ", " + b).orElse(""));

        long start = System.currentTimeMillis();
        boolean anyUp = false;
        for (ServiceInstance instance : instances) {
            try {
                HttpResponse<String> resp = httpClient.send(HttpRequest.newBuilder()
                                .uri(URI.create("http://" + instance.getHost() + ":" + instance.getPort()
                                        + "/actuator/health"))
                                .timeout(PROBE_TIMEOUT)
                                .GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200 && resp.body() != null && resp.body().contains("\"UP\"")) {
                    anyUp = true;
                }
            } catch (Exception e) {
                log.debug("健康探测失败: {} {} {}", serviceName, instance.getHost(), e.getMessage());
            }
        }
        vo.setLatency((int) (System.currentTimeMillis() - start));
        vo.setStatus(anyUp ? "UP" : "DOWN");
        return vo;
    }
}
