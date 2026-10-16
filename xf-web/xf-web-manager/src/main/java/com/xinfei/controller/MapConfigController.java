package com.xinfei.controller;

import com.xinfei.common.config.dynamic.DynamicConfigService;
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
 * 管理端地图页在登录态下经本接口获取。读取链与算价侧 {@code BaiduMapUtils} 同键同源，
 * 统一走 {@link DynamicConfigService}：sys_config（键 {@code baidu.map.ak}，敏感项
 * AES 解密 + 30s 本地缓存）→ {@code -Dbaidu.map.ak} → 环境变量 {@code BAIDU_MAP_AK}
 * （后两级由 Environment 回落覆盖，SystemEnvironmentPropertySource 松弛匹配）。</p>
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

    /** sys_config 配置键（与 BaiduMapUtils.CONFIG_KEY_BAIDU_AK 同键，同源读取） */
    private static final String CONFIG_KEY = "baidu.map.ak";

    private final DynamicConfigService dynamicConfig;

    /** 出参 VO（ak 为空串表示未配置，前端据此提示"地图未配置"，不报 500） */
    @Data
    public static class MapConfigVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String ak;
    }

    @Operation(summary = "获取前端地图配置（百度地图 AK）")
    @GetMapping
    public MapConfigVO get() {
        String ak = dynamicConfig.get(CONFIG_KEY, "");
        MapConfigVO vo = new MapConfigVO();
        vo.setAk(ak == null ? "" : ak.trim());
        return vo;
    }
}
