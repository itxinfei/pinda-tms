package com.xinfei.auth.controller.v1;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.xinfei.auth.common.v1.ApiResult;
import com.xinfei.auth.config.StpInterfaceImpl;
import com.xinfei.auth.license.LicenseInfo;
import com.xinfei.auth.service.v1.LicenseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 授权管理接口（youlai 契约 /api/v1/license）。
 *
 * <p>网关白名单不含本路径，故 {@code /status} 也要登录态；{@code /activate} 与
 * {@code DELETE /license} 额外要求超管角色（{@link StpInterfaceImpl#SUPER_ROLE}）。
 * 授权真伪只由后端判定，前端不参与验签。</p>
 */
@RestController
@RequestMapping("/license")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;

    /** 查询当前授权状态（登录态可查，授权页/水印唯一数据源）。 */
    @GetMapping("/status")
    public ApiResult<LicenseInfo> status() {
        return ApiResult.success(licenseService.status());
    }

    /** 激活（校验 + 机器绑定 + 落盘），仅超管。 */
    @PostMapping("/activate")
    @SaCheckRole(StpInterfaceImpl.SUPER_ROLE)
    public ApiResult<LicenseInfo> activate(@Valid @RequestBody ActivateRequest request) {
        return ApiResult.success(licenseService.activate(request.licenseKey()));
    }

    /** 清除落盘授权码（回到试用态），仅超管。环境变量托管时由 Service 拒绝。 */
    @DeleteMapping
    @SaCheckRole(StpInterfaceImpl.SUPER_ROLE)
    public ApiResult<LicenseInfo> clear() {
        return ApiResult.success(licenseService.clear());
    }

    /** 激活请求体。 */
    public record ActivateRequest(@NotBlank(message = "请输入授权码") String licenseKey) {
    }
}
