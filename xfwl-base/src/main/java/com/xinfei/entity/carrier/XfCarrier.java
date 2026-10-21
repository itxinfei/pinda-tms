package com.xinfei.entity.carrier;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 承运商/外协（DSP-2 外协运力池第一期）
 */
@Data
@TableName("base_carrier")
public class XfCarrier {

    /** 状态：合作中 */
    public static final int STATUS_ACTIVE = 1;
    /** 状态：暂停 */
    public static final int STATUS_SUSPENDED = 0;
    /** 状态：黑名单 */
    public static final int STATUS_BLACKLIST = 2;

    /** 结算方式：现结 */
    public static final int SETTLE_CASH = 1;
    /** 结算方式：月结 */
    public static final int SETTLE_MONTHLY = 2;
    /** 结算方式：趟结 */
    public static final int SETTLE_PER_TRIP = 3;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 承运商编码（租户内唯一） */
    private String code;

    /** 承运商名称 */
    private String name;

    /** 道路运输许可证号 */
    private String licenseNo;

    /** 许可证有效期（第一期仅展示，自动排除调度后置） */
    private LocalDate licenseExpiry;

    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String contactPhone;

    /** 结算方式 1现结 2月结 3趟结 */
    private Integer settleType;

    /** 状态 1合作中 0暂停 2黑名单 */
    private Integer status;

    /** 备注 */
    private String remark;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
