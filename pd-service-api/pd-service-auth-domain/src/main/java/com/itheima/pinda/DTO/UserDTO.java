package com.itheima.pinda.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户（账号）DTO：供微服务间 Feign 调用。
 * 字段按 pd-web / pd-dispatch 实际访问裁剪。
 */
@Data
@Schema(description = "用户信息")
public class UserDTO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "账号")
    private String account;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "所属组织ID")
    private Long orgId;

    @Schema(description = "岗位ID")
    private Long stationId;

    @Schema(description = "状态 true启用 false禁用")
    private Boolean status;
}
