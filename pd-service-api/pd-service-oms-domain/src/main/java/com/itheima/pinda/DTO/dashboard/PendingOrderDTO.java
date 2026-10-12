package com.itheima.pinda.DTO.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理端工作台-待处理运单列表项（轻量投影，区别于字段庞大的 OrderDTO）。
 *
 * <p>statusText 中文状态由 pd-oms 依据《07》权威映射直接给出，前端零状态码映射，
 * 规避管理端旧五位码 enum 与新紧凑码不一致的问题。</p>
 */
@Data
@Schema(description = "工作台待处理运单")
public class PendingOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID（雪花，字符串）")
    private String id;

    @Schema(description = "订单号（展示用）")
    private String orderNo;

    @Schema(description = "寄件人")
    private String senderName;

    @Schema(description = "订单状态紧凑码（1~12）")
    private Integer status;

    @Schema(description = "订单状态中文")
    private String statusText;

    @Schema(description = "下单时间（yyyy-MM-dd HH:mm:ss）")
    private String createTime;
}
