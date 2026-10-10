package com.itheima.pinda.vo.base.transforCenter.business;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "车辆行驶证信息")
public class TruckLicenseVo implements Serializable {
    @Schema(description = "id")
    private String id;
    @Schema(description = "车辆信息")
    private TruckVo truck;
    @Schema(description = "发动机编号")
    private String engineNumber;
    @Schema(description = "注册时间,格式:yyyy-MM-dd HH:mm:ss")
    private String registrationDate;
    @Schema(description = "国家强制报废日期,格式:yyyy-MM-dd HH:mm:ss")
    private String mandatoryScrap;
    @Schema(description = "检验有效期,格式:yyyy-MM-dd HH:mm:ss")
    private String expirationDate;
    @Schema(description = "整备质量")
    private BigDecimal overallQuality;
    @Schema(description = "核定载质量")
    private BigDecimal allowableWeight;
    @Schema(description = "外廓尺寸")
    private String outsideDimensions;
    @Schema(description = "行驶证有效期,格式:yyyy-MM-dd HH:mm:ss")
    private String validityPeriod;
    @Schema(description = "道路运输证号")
    private String transportCertificateNumber;
    @Schema(description = "图片信息")
    private String picture;
}
