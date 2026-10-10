package com.itheima.pinda.DTO.webManager;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TransportOrderQueryDTO {
    @Schema(description = "当前页数")
    private Integer page = 1;
    @Schema(description = "每页条数")
    private Integer pageSize = 10;
    @Schema(description = "运单id")
    private String id;
    @Schema(description = "运单状态(1.新建 2.已装车，发往x转运中心 3.到达 4.到达终端网点)")
    private Integer status;
    @Schema(description = "发件人省份id")
    private String senderProvinceId;
    @Schema(description = "发件人城市id")
    private String senderCityId;
    @Schema(description = "发件人区县id")
    private String senderCountyId;
    @Schema(description = "发件人姓名")
    private String senderName;
    @Schema(description = "发件人电话")
    private String senderPhone;
    @Schema(description = "收件人省份id")
    private String receiverProvinceId;
    @Schema(description = "收件人城市id")
    private String receiverCityId;
    @Schema(description = "收件人姓名")
    private String receiverName;
    @Schema(description = "收件人区县id")
    private String receiverCountyId;
    @Schema(description = "收件人电话")
    private String receiverPhone;
}
