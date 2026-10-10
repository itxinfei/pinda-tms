package com.itheima.pinda.vo.base.transforCenter.business;

import com.itheima.pinda.vo.base.angency.AgencyVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "车辆信息")
public class TruckVo implements Serializable {
    private static final long serialVersionUID = 8540398115062991784L;
    @Schema(description = "id")
    private String id;
    @Schema(description = "车辆类型")
    private TruckTypeVo truckType;
    @Schema(description = "所属车队")
    private FleetVo fleet;
    @Schema(description = "品牌")
    private String brand;
    @Schema(description = "车牌号码")
    private String licensePlate;
    @Schema(description = "GPS设备id")
    private String deviceGpsId;
    @Schema(description = "准载重量")
    private BigDecimal allowableLoad;
    @Schema(description = "准载体积")
    private BigDecimal allowableVolume;
    @Schema(description = "车辆行驶证信息id")
    private String truckLicenseId;
    @Schema(description = "所属机构信息")
    private AgencyVo agency;
    @Schema(description = "工作状态")
    private String workStatus;
    @Schema(description = "过期状态")
    private String expireStatus;
    @Schema(description = "装载状态")
    private String loadStatus;
}
