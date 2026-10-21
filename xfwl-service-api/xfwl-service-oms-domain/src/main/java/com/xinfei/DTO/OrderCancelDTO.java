package com.xinfei.DTO;

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

    /**
     * 枚举理由（P0-9/FR-75 合规留痕，白名单校验：
     * CUSTOMER_CANCEL/CARGO_DAMAGE/ADDRESS_ERROR/OTHER；缺省时按 OTHER 落痕）
     */
    @Schema(description = "枚举理由（合规留痕白名单）")
    private String reasonEnum;
}
