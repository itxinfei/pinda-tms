package com.itheima.pinda.vo.base.transforCenter.business;

import com.itheima.pinda.vo.base.angency.AgencySimpleVo;
import com.itheima.pinda.vo.base.userCenter.SysUserVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "车队信息")
public class FleetVo implements Serializable {
    private static final long serialVersionUID = -1658601432859261332L;
    @Schema(description = "id")
    private String id;
    @Schema(description = "车队名称")
    private String name;
    @Schema(description = "车队编号")
    private String fleetNumber;
    @Schema(description = "所属机构")
    private AgencySimpleVo agency;
    @Schema(description = "负责人")
    private SysUserVo manager;
    @Schema(description = "车辆总数")
    private Integer truckCount;
    @Schema(description = "司机总数")
    private Integer driverCount;
    @Schema(description = "车队司机")
    private List<SysUserVo> drivers;
    @Schema(description = "车队车辆")
    private List<TruckVo> trucks;
}
