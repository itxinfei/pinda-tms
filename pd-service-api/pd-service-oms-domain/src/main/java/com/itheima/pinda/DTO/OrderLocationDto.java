package com.itheima.pinda.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 位置信息
 */
@Schema(description = "位置信息")
@Data
public class OrderLocationDto implements Serializable {
    private static final long serialVersionUID = -8573238049526791013L;
    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;
    /**
     * 订单id
     */
    @Schema(description = "订单id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderId;
    /**
     * 发送地址坐标
     */
    @Schema(description = "发送地址坐标")
    private String sendLocation;
    /**
     * 收货地址坐标
     */
    @Schema(description = "收货地址坐标")
    private String receiveLocation;
    /**
     * 发送起始网点
     */
    @Schema(description = "发送起始网点")
    private String sendAgentId;

    /**
     * 接受的终止网点
     */
    @Schema(description = "接受的终止网点")
    private String receiveAgentId;
    /**
     * 记录状态 0：无效，1有效
     */
    @Schema(description = "记录状态")
    private String status;
}
