package com.itheima.pinda.vo.base.transforCenter.business;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "司机驾驶证信息")
public class DriverLicenseVo implements Serializable {
    private static final long serialVersionUID = 7204484845857308415L;
    @Schema(description = "司机id")
    private String userId;
    @Schema(description = "准驾车型")
    private String allowableType;
    @Schema(description = "初次领证日期")
    private String initialCertificateDate;
    @Schema(description = "有效期限")
    private String validPeriod;
    @Schema(description = "驾驶证号")
    private String licenseNumber;
    @Schema(description = "驾龄")
    private Integer driverAge;
    @Schema(description = "驾驶证类型")
    private String licenseType;
    @Schema(description = "从业资格证信息")
    private String qualificationCertificate;
    @Schema(description = "入场证信息")
    private String passCertificate;
    @Schema(description = "图片")
    private String picture;
}
