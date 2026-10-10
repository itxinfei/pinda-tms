package com.itheima.pinda.vo.base.transforCenter.business;

import com.itheima.pinda.vo.base.angency.AgencySimpleVo;
import com.itheima.pinda.vo.base.angency.AgencyVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "司机基本信息")
public class DriverVo implements Serializable {
    private static final long serialVersionUID = 6475689247966513088L;
    @Schema(description = "司机id")
    private String userId;
    @Schema(description = "司机姓名")
    private String name;
    @Schema(description = "工号")
    private String workNumber;
    @Schema(description = "所属机构信息")
    private AgencySimpleVo agency;
    @Schema(description = "手机号")
    private String mobile;
    @Schema(description = "头像")
    private String avatar;
    @Schema(description = "所属车队")
    private FleetVo fleet;
    @Schema(description = "使用车辆")
    private TruckVo truck;
    @Schema(description = "车辆线路")
    private TransportLineVo truckTransportLine;
    @Schema(description = "车辆所属车次")
    private TransportTripsVo truckTransportTrip;
    @Schema(description = "线路")
    private TransportLineVo transportLine;
    @Schema(description = "工作状态")
    private String workStatus;
    @Schema(description = "年龄")
    private Integer age;
}
