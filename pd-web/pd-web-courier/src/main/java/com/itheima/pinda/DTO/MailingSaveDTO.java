package com.itheima.pinda.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "预估总价模型")
public class MailingSaveDTO {
    /**
     * 付款方式,1.预结2到付
     */
    @Schema(description = "付款方式 1预结 2到付")
    private Integer payMethod;
    @Schema(description = "物品类型")
    private String goodsType;
    @Schema(description = "物品名称")
    private String goodsName;
    @Schema(description = "物品重量")
    private String goodsWeight;
    @Schema(description = "物品体积")
    private String goodsVolume;
    @Schema(description = "订单号")
    private String orderNumber;
}
