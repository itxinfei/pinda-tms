package com.xinfei.vo.oms;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xinfei.vo.base.AreaSimpleVo;
import com.xinfei.vo.work.TaskPickupDispatchVo;
import com.xinfei.vo.work.TransportOrderVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "订单信息")
public class OrderVo implements Serializable {
    private static final long serialVersionUID = 1713530076914843839L;
    @Schema(description = "id")
    private String id;

    @Schema(description = "订单类型，1为同城订单，2为城际订单")
    private Integer orderType;

    @Schema(description = "取件类型，1为网点自寄，2为上门取件")
    private Integer pickupType;

    @Schema(description = "下单时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime createTime;

    // 说明：memberId 为客户ID（与订单服务 OrderDTO.memberId 对应），如需完整客户模型可扩展 CustomerVo 关联
    @Schema(description = "客户id")
    private String memberId;

    @Schema(description = "收件人省份")
    private AreaSimpleVo receiverProvince;

    @Schema(description = "收件人城市")
    private AreaSimpleVo receiverCity;

    @Schema(description = "收件人区县")
    private AreaSimpleVo receiverCounty;

    @Schema(description = "收件人详细地址")
    private String receiverAddress;

    @Schema(description = "收件人姓名")
    private String receiverName;

    @Schema(description = "收件人电话")
    private String receiverPhone;

    @Schema(description = "发件人省份")
    private AreaSimpleVo senderProvince;

    @Schema(description = "发件人城市")
    private AreaSimpleVo senderCity;

    @Schema(description = "发件人区县")
    private AreaSimpleVo senderCounty;

    @Schema(description = "发件人详细地址")
    private String senderAddress;

    @Schema(description = "发件人姓名")
    private String senderName;

    @Schema(description = "发件人电话")
    private String senderPhone;

    @Schema(description = "付款方式,1.预结2到付")
    private Integer paymentMethod;

    @Schema(description = "付款状态,1.未付2已付")
    private Integer paymentStatus;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "预计到达时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime estimatedArrivalTime;

    @Schema(description = "距离，单位：公里")
    private BigDecimal distance;

    @Schema(description = "订单状态（紧凑码 1~12）：1待取件 2已取件 3网点自寄 4网点入库 5待装车 6运输中 7网点出库 8待派送 9派送中 10已签收 11拒收 12已取消")
    private Integer status;

    @Schema(description = "取消原因（status=12 时有值）")
    private String cancelReason;

    @Schema(description = "取消时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime cancelTime;

    @Schema(description = "页码")
    private Integer page;

    @Schema(description = "页尺寸")
    private Integer pageSize;

    @Schema(description = "取件信息")
    private TaskPickupDispatchVo taskPickup;

    @Schema(description = "派件信息")
    private TaskPickupDispatchVo taskDispatch;

    @Schema(description = "运单信息")
    private TransportOrderVo transportOrder;
}
