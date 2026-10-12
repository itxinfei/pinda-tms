package com.itheima.pinda.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 订单取消入参
 */
@Data
@Schema(description = "订单取消入参")
public class OrderCancelDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 取消原因（必填，最长 200 字）
     */
    @Schema(description = "取消原因")
    private String reason;
}
