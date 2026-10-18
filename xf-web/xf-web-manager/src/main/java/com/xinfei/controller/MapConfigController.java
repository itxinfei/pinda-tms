package com.xinfei.controller;

import com.xinfei.common.config.dynamic.DynamicConfigService;
import com.xinfei.feign.ConfigFeign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;

/**
 * 地图配置 API（DSP-02 在途监控 / DSP-03 调度一张图）
 *
 * <p>百度地图 AK 属第三方凭证，禁止写进前端环境变量/仓库；由后端统一持有，
 * 管理端地图页在登录态下经本接口获取。BFF 无数据源读不了 sys_config，
 * 故浏览器端键 {@code baidu.map.ak.web} 走 {@link ConfigFeign} 读 xf-auth
 * （敏感项服务端解密，30s 侧无缓存依赖下游 fallback 兜底）；
 * 服务端键 {@code baidu.map.ak} 仅作类型不符时的降级回落，走
 * {@link DynamicConfigService}：{@code -Dbaidu.map.ak} → 环境变量
 * {@code BAIDU_MAP_AK}（compose 注入，SystemEnvironmentPropertySource 松弛匹配）。</p>
 *
 * <p>⚠️ 返回类型必须声明为 POJO 而非 {@code Map<String,String>}：
 * {@link GlobalResponseAdvice} 会把响应包装成 {@code {code:0,msg,data}} 壳，
 * Jackson 按方法声明泛型 {@code Map<String,String>} 序列化时对壳里的 Integer code
 * 做 String 强转直接 500（2026-10-16 冒烟实测）。POJO 声明类型则按运行时 Map 正常序列化。</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "地图配置API")
@RequestMapping("map-config-manager")
public class MapConfigController {

    /**
     * sys_config 配置键（与 {@code BaiduGeoClient.CONFIG_KEY_BAIDU_AK} 同键，同源读取，批3 收敛后地理 HTTP 唯一出口）。
     * <p>⚠️ 百度 AK 分类型：后端 REST（geocoding/directionlite）必须用<b>服务端</b>类型 AK；
     * 前端 BMapGL JS API 必须<b>浏览器端</b>类型 AK，一把不能两用。故本控制器优先读
     * {@link #CONFIG_KEY_BROWSER}（浏览器端键），未配置时回落到服务端键（兼容仅配一把的场景，
     * 但此时若类型是服务端，浏览器加载 JS API 会被百度拒绝，反之亦然——建议两把分别配置）。</p>
     */
    private static final String CONFIG_KEY_SERVER = "baidu.map.ak";
    private static final String CONFIG_KEY_BROWSER = "baidu.map.ak.web";

    private final DynamicConfigService dynamicConfig;
    private final ConfigFeign configFeign;

    /** 出参 VO（ak 为空串表示未配置，前端据此提示"地图未配置"，不报 500） */
    @Data
    public static class MapConfigVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String ak;
    }

    @Operation(summary = "获取前端地图配置（浏览器端类型百度地图 AK）")
    @GetMapping
    public MapConfigVO get() {
        // 浏览器端 JS API 必须用浏览器端类型 AK：优先 baidu.map.ak.web（sys_config，经 ConfigFeign 读 xf-auth）
        String ak = resolveBrowserAk();
        if (ak == null || ak.isBlank()) {
            // 降级回落服务端键：类型不符时百度会拒绝 JS API 加载，打 WARN 便于排障
            log.warn("[地图配置] 未配置浏览器端 AK（{}），已回落服务端键；若该键类型为服务端，前端 BMapGL 将被百度拒绝加载",
                    CONFIG_KEY_BROWSER);
            ak = dynamicConfig.get(CONFIG_KEY_SERVER, "");
        }
        MapConfigVO vo = new MapConfigVO();
        vo.setAk(ak == null ? "" : ak.trim());
        return vo;
    }

    /**
     * 读浏览器端 AK：ConfigFeign（sys_config，敏感项服务端解密；fallback 返回 null）
     * → 本地 DynamicConfigService（BFF 无数据源时退化为 -D/环境变量，兜底手工注入场景）。
     */
    private String resolveBrowserAk() {
        try {
            String v = configFeign.valueByKey(CONFIG_KEY_BROWSER);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        } catch (Exception e) {
            log.warn("[地图配置] Feign 读取浏览器端 AK 失败，回落本地: {}", e.getMessage());
        }
        return dynamicConfig.get(CONFIG_KEY_BROWSER, "");
    }
}
