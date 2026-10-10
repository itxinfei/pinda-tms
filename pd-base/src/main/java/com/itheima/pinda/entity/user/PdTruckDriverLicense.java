package com.itheima.pinda.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 司机驾驶证
 */
@Data
@TableName("base_driver_license")
public class PdTruckDriverLicense {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 司机 base_truck_driver.id */
    private Long driverId;

    /** 准驾车型 */
    private String allowableType;

    /** 初次领证日期 */
    private LocalDate initialCertificateDate;

    /** 有效期限 */
    private String validPeriod;

    /** 驾驶证号 */
    private String licenseNumber;

    /** 驾龄 */
    private Integer driverAge;

    /** 驾驶证类型 */
    private String licenseType;

    /** 从业资格证 */
    private String qualificationCertificate;

    /** 入场证 */
    private String passCertificate;

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
