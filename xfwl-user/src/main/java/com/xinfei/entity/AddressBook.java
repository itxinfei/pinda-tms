package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员地址簿
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("member_address_book")
public class AddressBook implements Serializable {

    /**
     * 主键（雪花）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 会员ID
     */
    private Long memberId;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系人电话
     */
    private String phoneNumber;

    /**
     * 分机号
     */
    private String extensionNumber;

    /**
     * 省（行政区划码）
     */
    private Integer provinceId;

    /**
     * 市（行政区划码）
     */
    private Integer cityId;

    /**
     * 区县（行政区划码）
     */
    private Integer countyId;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 是否默认：1默认 0否
     */
    private Integer isDefault;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0未删 1已删
     */
    @TableLogic
    private Integer deleted;

    // tenantId 不声明，统一由 TenantLineInnerInterceptor 自动填充（详见 Member 实体注释）。
}
