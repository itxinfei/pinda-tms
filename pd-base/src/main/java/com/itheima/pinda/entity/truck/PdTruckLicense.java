package com.itheima.pinda.entity.truck;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车辆行驶证
 */
@Data
@TableName("base_truck_license")
public class PdTruckLicense {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 车辆 base_truck.id */
    private Long truckId;

    /** 发动机编号 */
    private String engineNumber;

    /** 注册日期 */
    private LocalDate registrationDate;

    /** 强制报废日期 */
    private LocalDate mandatoryScrap;

    /** 检验有效期 */
    private LocalDate expirationDate;

    /** 整备质量(kg) */
    private BigDecimal overallQuality;

    /** 核定载质量(kg) */
    private BigDecimal allowableWeight;

    /** 外廓尺寸 */
    private String outsideDimensions;

    /** 行驶证有效期 */
    private LocalDate validityPeriod;

    /** 道路运输证号 */
    private String transportCertificateNumber;

    /** 图片 */
    private String picture;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
