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
 * 附加费费用项配置 —— POD-03 第一期 / FA-10
 *
 * <p>费用项与费率外部化配置（禁止硬编码）；fee_code 与 RPT-01 成本项口径对齐，
 * 保证计费侧费用项与成本侧可逐项对账。第一期计费方式仅 固定金额/按运费比例 两类。</p>
 */
@Data
@TableName("oms_fee_item")
public class FeeItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 计费方式：固定金额 */
    public static final int CALC_TYPE_FIXED = 1;
    /** 计费方式：按运费比例 */
    public static final int CALC_TYPE_RATIO = 2;

    /** 状态：启用 */
    public static final int STATUS_ENABLED = 1;
    /** 状态：停用 */
    public static final int STATUS_DISABLED = 0;

    /** 费用项编码（与 RPT-01 成本项口径对齐，扩项须两端同步） */
    public static final String CODE_TOLL = "TOLL";
    public static final String CODE_LOADING = "LOADING";
    public static final String CODE_WAIT = "WAIT";
    public static final String CODE_OVERWEIGHT = "OVERWEIGHT";
    public static final String CODE_FUEL = "FUEL";
    public static final String CODE_EMPTY_RETURN = "EMPTY_RETURN";

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 费用项编码 */
    private String feeCode;

    /** 费用项名称 */
    private String feeName;

    /** 计费方式：1固定金额 2按运费比例 */
    private Integer calcType;

    /** 固定金额（元，calcType=1 必填） */
    private BigDecimal fixedAmount;

    /** 运费比例（0.0500=5%，calcType=2 必填） */
    private BigDecimal ratio;

    /** 状态：1启用 0停用 */
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
