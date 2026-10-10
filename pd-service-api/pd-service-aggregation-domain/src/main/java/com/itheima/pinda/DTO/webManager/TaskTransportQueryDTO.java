package com.itheima.pinda.DTO.webManager;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TaskTransportQueryDTO {
    @Schema(description = "当前页数")
    private Integer page = 1;
    @Schema(description = "每页条数")
    private Integer pageSize = 10;
    @Schema(description = "运输任务id")
    private String id;
    @Schema(description = "司机姓名")
    private String driverName;
    @Schema(description = "运输任务状态")
    private Integer status;
}
