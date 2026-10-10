package com.itheima.pinda.vo.base.transforCenter.business;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "车次信息")
public class TransportTripsVo implements Serializable {
    private static final long serialVersionUID = 3496683581406652811L;
    @Schema(description = "id")
    private String id;
    @Schema(description = "车次名称")
    private String name;
    @Schema(description = "发车时间")
    private String departureTime;
    @Schema(description = "到达时间")
    private String arrivalTime;
    @Schema(description = "所属线路")
    private TransportLineVo transportLine;
    @Schema(description = "周期，1为天，2为周，3为月")
    private Integer period;
    @Schema(description = "周期名称")
    private String periodName;
    @Schema(description = "所选车辆和司机")
    private List<TruckDriverVo> truckDrivers;
}
