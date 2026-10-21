package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客户价格协议 —— FIN-1.1 / POD-06 客户侧费率卡
 *
 * <p>按生效日期版本化，同一客户可存多版本；计费取"业务日期命中版本"而非最新版本。
 * 计费衔接：{@code OrderServiceImpl#calculateAmount} Drools 之前查协议，命中走协议价、未命中走默认规则。</p>
 */
@Data
@TableName("oms_price_agreement")
public class PriceAgreement implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 计价方式：按票 */
    public static final int PRICE_TYPE_PER_WAYBILL = 1;
    /** 计价方式：按吨 */
    public static final int PRICE_TYPE_PER_TON = 2;
    /** 计价方式：按方（预留，暂同按票口径） */
    public static final int PRICE_TYPE_PER_CUBIC = 3;

    /** 状态：生效 */
    public static final int STATUS_ENABLED = 1;
    /** 状态：停用 */
    public static final int STATUS_DISABLED = 0;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 客户id（月结/企业客户，对齐 order.member_id；散客 GUEST 不建协议） */
    private String memberId;

    /** 线路id（空=不限） */
    private Long lineId;

    /** 货物类型id（空=不限） */
    private Long goodsTypeId;

    /** 重量段下限 kg（含） */
    private BigDecimal weightMin;

    /** 重量段上限 kg（含，空=不封顶） */
    private BigDecimal weightMax;

    /** 计价方式：1按票 2按吨 3按方 */
    private Integer priceType;

    /** 单价（按票=元/票，按吨=元/吨） */
    private BigDecimal price;

    /** 月结折扣（0.90=9折，1.00=无折扣） */
    private BigDecimal discount;

    /** 生效日期（含） */
    private LocalDate effectiveFrom;

    /** 失效日期（含，空=长期有效） */
    private LocalDate effectiveTo;

    /** 状态：1生效 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
