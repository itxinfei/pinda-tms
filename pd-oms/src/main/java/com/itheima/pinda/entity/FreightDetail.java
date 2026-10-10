package com.itheima.pinda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运费明细
 *
 * <p>按费用项记录订单计费结果，落库后可对账、可追溯。
 * 最小闭环阶段每个订单先落一条"运费"明细（订单总价），
 * 后续 Drools 规则扩展后再拆分首重/续重/保价/上楼等费用项。</p>
 */
@Data
@TableName("oms_freight_detail")
public class FreightDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 费用项：运费（默认费用项，总价先落此条目）
     */
    public static final String FEE_ITEM_FREIGHT = "FREIGHT";

    /**
     * 费用项名称：运费
     */
    public static final String FEE_ITEM_NAME_FREIGHT = "运费";

    /**
     * 计量单位：票（按票计价）
     */
    public static final String UNIT_TICKET = "票";

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 运单ID
     */
    private Long transportOrderId;

    /**
     * 费用项编码：FREIGHT-运费；后续扩展 FIRST/FIRST_CONTINUED/INSURED/UPSTAIRS 等
     */
    private String feeItem;

    /**
     * 费用项名称
     */
    private String feeItemName;

    /**
     * 数量（重量/件数/票数等）
     */
    private BigDecimal quantity;

    /**
     * 计量单位
     */
    private String unit;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 该表仅记录创建，不做更新与逻辑删除（与 DDL 一致）
     */
    private Long createBy;
    private LocalDateTime createTime;
}
