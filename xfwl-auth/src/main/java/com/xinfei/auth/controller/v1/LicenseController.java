package com.xinfei.auth.controller.v1;

import com.xinfei.auth.common.v1.ApiResult;
import com.xinfei.auth.license.LicenseInfo;
import com.xinfei.auth.service.v1.LicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 授权状态接口（youlai 契约 /api/v1/license/status）。
 *
 * <p>故意<b>不加</b> {@code @SaCheckPermission}——匿名可访问，
 * 以便登录前/未授权时页面也能拉到状态展示提示。仅返回状态与天数，
 * 不泄露 IP、业务数据等敏感信息。</p>
 */
@RestController
@RequestMapping("/license")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;

    @GetMapping("/status")
    public ApiResult<LicenseInfo> status() {
        return ApiResult.success(licenseService.getStatus());
    }
}
