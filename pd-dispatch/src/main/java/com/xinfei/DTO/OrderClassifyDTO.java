package com.xinfei.DTO;

import lombok.Builder;
import lombok.Data;

/**
 * 封装原始订单信息（调度内部分组模型）
 *
 * <p>机构ID为 disp 内部 Long 口径；订单（OrderDTO）仍走 pd-oms String 边界。</p>
 */
@Data
@Builder
public class OrderClassifyDTO {

    /**
     * 起始机构ID
     */
    private Long startOrgId;
    /**
     * 目的机构ID
     */
    private Long endOrgId;
    private Integer orderType;
    private String goodsType;
    /**
     * 当前所在机构ID
     */
    private Long currentOrgId;

    public String groupBy() {
        if (this.startOrgId != null && this.endOrgId != null && this.currentOrgId != null) {
            return this.startOrgId + "#" + this.endOrgId + "#" + this.currentOrgId;
        } else {
            return "ERROR";
        }
    }

    /**
     * 原订单
     */
    private OrderDTO order;

}
