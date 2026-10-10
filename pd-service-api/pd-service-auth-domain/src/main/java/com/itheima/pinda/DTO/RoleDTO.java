package com.itheima.pinda.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色 DTO：供微服务间 Feign 调用，仅取展示所需的 id、name。
 */
@Data
@Schema(description = "角色信息")
public class RoleDTO {

    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色名称")
    private String name;
}
