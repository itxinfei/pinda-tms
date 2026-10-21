package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单附加费明细 —— POD-03 第一期
 *
 * <p>费用实例层：与运费挂钩订单（order.id 过渡契约 String），金额快照可逐项追溯（ORD-03 口径）。
 * 第一期独立存表不改写 order.amount，展示层汇总。</p>
 */
@Data
@TableName("oms_order_fee")
public class OrderFee implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 来源：自动计算 */
    public static final int SOURCE_AUTO = 1;
    /** 来源：人工补录 */
    public static final int SOURCE_MANUAL = 2;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单id（对齐 order.id 过渡契约 String） */
    private String orderId;

    /** 费用项编码（oms_fee_item.fee_code） */
    private String feeCode;

    /** 费用项名称（落库快照，防配置改名后历史失义） */
    private String feeName;

    /** 金额（元） */
    private BigDecimal amount;

    /** 来源：1自动计算 2人工补录 */
    private Integer source;

    /** 计算依据（如：运费350×燃油5%=17.50；人工补录填事由） */
    private String calcBasis;

    /** 操作人id（人工补录时） */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 备注 */
    private String remark;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
