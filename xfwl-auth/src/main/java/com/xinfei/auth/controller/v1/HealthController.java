package com.xinfei.auth.controller.v1;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.auth.common.v1.ApiResult;
import com.xinfei.auth.common.v1.HealthVO;
import com.xinfei.auth.service.v1.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 服务健康聚合（youlai 契约风格 /api/v1/health）：
 * 列表实时探测（服务数少，直接全量返回无分页）；概览 = UP/DOWN 计数。
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    @GetMapping
    @SaCheckPermission("sys:health:list")
    public ApiResult<List<HealthVO>> list(
            @RequestParam(value = "keywords", required = false) String keywords) {
        return ApiResult.success(healthService.list(keywords));
    }

    @GetMapping("/overview")
    @SaCheckPermission("sys:health:list")
    public ApiResult<Map<String, Object>> overview(
            @RequestParam(value = "keywords", required = false) String keywords) {
        List<HealthVO> list = healthService.list(keywords);
        long up = list.stream().filter(v -> "UP".equals(v.getStatus())).count();
        return ApiResult.success(Map.of(
                "total", list.size(),
                "upCount", up,
                "downCount", list.size() - up));
    }
}
