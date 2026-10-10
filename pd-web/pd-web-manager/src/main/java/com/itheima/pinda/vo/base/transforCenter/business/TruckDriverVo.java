package com.itheima.pinda.vo.base.transforCenter.business;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "车辆和司机关联信息")
public class TruckDriverVo implements Serializable {
    private static final long serialVersionUID = 6343522146170548498L;
    @Schema(description = "司机")
    private DriverVo driver;
    @Schema(description = "车辆")
    private TruckVo truck;
    @Schema(description = "线路信息")
    private TransportLineVo transportLine;
    @Schema(description = "车次信息")
    private TransportTripsVo transportTrips;
}