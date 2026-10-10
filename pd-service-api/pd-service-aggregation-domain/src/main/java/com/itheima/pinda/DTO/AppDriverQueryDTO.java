package com.itheima.pinda.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AppDriverQueryDTO {
    @Schema(description = "当前页数")
    private Integer page = 1;
    @Schema(description = "每页条数")
    private Integer pageSize = 10;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "搜索条件")
    private String keyword;
    @Schema(description = "司机id")
    private String driverId;
}
