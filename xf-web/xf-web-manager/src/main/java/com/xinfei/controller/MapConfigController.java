package com.xinfei.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 地图配置 API（DSP-02 在途监控）
 *
 * <p>百度地图 AK 属第三方凭证，禁止写进前端环境变量/仓库；由后端统一持有，
 * 管理端地图页在登录态下经本接口获取。读取链与部署注入对齐：
 * JVM 参数 -Dbaidu.map.ak（compose JAVA_TOOL_OPTIONS 统一注入）→ 环境变量 BAIDU_MAP_AK。</p>
 */
@Slf4j
@RestController
@Tag(name = "地图配置API")
@RequestMapping("map-config-manager")
public class MapConfigController {

    /** JVM 系统属性键（与 BaiduMapUtils/LocationUtil 同键，同源注入） */
    private static final String SYS_PROP_KEY = "baidu.map.ak";

    /** 环境变量键（compose .env 注入 BAIDU_MAP_AK） */
    private static final String ENV_KEY = "BAIDU_MAP_AK";

    @Operation(summary = "获取前端地图配置（百度地图 AK）")
    @GetMapping
    public Map<String, String> get() {
        String ak = System.getProperty(SYS_PROP_KEY);
        if (ak == null || ak.trim().isEmpty()) {
            ak = System.getenv(ENV_KEY);
        }
        Map<String, String> data = new HashMap<>();
        // 为空也返回结构（ak=""），前端据此提示"地图未配置"，不报 500
        data.put("ak", ak == null ? "" : ak.trim());
        return data;
    }
}
