package com.itheima.pinda.vo.oms;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "订单轨迹参数")
public class OrderLocusVo {
    @Schema(description = "业务id")
    private String businessId;
    @Schema(description = "开始时间")
    private String ge___time;
    @Schema(description = "结束时间")
    private String le___time;
    @Schema(description = "运输任务id")
    private String transportTaskId;
}
