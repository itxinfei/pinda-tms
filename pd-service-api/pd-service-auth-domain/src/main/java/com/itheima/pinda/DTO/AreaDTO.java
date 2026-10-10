package com.itheima.pinda.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 行政区划 DTO：供微服务间 Feign 调用。
 * 字段按 pd-web / pd-dispatch 实际访问裁剪（仅 id、name）。
 */
@Data
@Schema(description = "行政区划信息")
public class AreaDTO {

    @Schema(description = "行政区划ID（国标区划码）")
    private Long id;

    @Schema(description = "行政区划名称")
    private String name;
}
