package com.itheima.pinda.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "寄件保存入参")
public class MailingSaveDTO {
    @Schema(description = "发件方地址簿id")
    private String sendAddress;
    @Schema(description = "收件方地址簿id")
    private String receiptAddress;
    @Schema(description = "取件时间")
    private String pickUpTime;
    @Schema(description = "取件方式")
    private Integer pickupType;
    @Schema(description = "付款方式,1.预结2到付")
    private Integer payMethod;
    @Schema(description = "物品类型")
    private String goodsType;
    @Schema(description = "物品名称")
    private String goodsName;
    @Schema(description = "物品重量")
    private String goodsWeight;
}
