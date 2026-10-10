package com.itheima.pinda.DTO.webManager;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TaskPickupDispatchQueryDTO {
    @Schema(description = "当前页数")
    private Integer page = 1;
    @Schema(description = "每页条数")
    private Integer pageSize = 10;
    @Schema(description = "任务类型，1为取件任务，2为派件任务")
    private Integer taskType;
    @Schema(description = "任务状态，1为待执行（对应 待上门和须交接）、2为进行中（该状态暂不使用，属于保留状态）、3为待确认（对应 待妥投和须交件）、4为已完成、5为已取消")
    private Integer status;
    @Schema(description = "运单id")
    private String transportOrderId;
    @Schema(description = "快递员姓名")
    private String courierName;
    @Schema(description = "发件人省份id")
    private String senderProvinceId;
    @Schema(description = "发件人城市id")
    private String senderCityId;
    @Schema(description = "发件人姓名")
    private String senderName;
    @Schema(description = "收件人省份id")
    private String receiverProvinceId;
    @Schema(description = "收件人城市id")
    private String receiverCityId;
    @Schema(description = "收件人姓名")
    private String receiverName;
}
