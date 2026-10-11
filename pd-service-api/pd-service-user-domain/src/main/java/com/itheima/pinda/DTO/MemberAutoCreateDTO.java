package com.itheima.pinda.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 会员自动建档入参：客户 BFF 在下单/首次访问时，
 * 按认证账号手机号在 pd-user 侧 get-or-create 会员。
 *
 * <p>租户不进 DTO：Feign 出站拦截器透传 tenantId 头，pd-user 侧
 * TenantLineInnerInterceptor 自动按租户隔离（uk_tenant_phone 租户内唯一）。</p>
 */
@Data
@Schema(description = "会员自动建档入参")
public class MemberAutoCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "手机号（租户内唯一）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @Schema(description = "昵称（取认证账号姓名，可空）")
    private String name;

    @Schema(description = "头像（可空）")
    private String avatar;
}
