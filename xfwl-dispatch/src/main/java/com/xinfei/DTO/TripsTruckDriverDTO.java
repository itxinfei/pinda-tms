package com.xinfei.DTO;

import lombok.Data;

/**
 * 车次-车辆-司机组合（调度内部模型）
 *
 * <p>tripsId/truckId 为 xfwl-base String 边界；driverId 已 Long 化（xfwl-auth 用户ID）。</p>
 */
@Data
public class TripsTruckDriverDTO {
    private String tripsId;
    private String truckId;
    private Long driverId;

    public TripsTruckDriverDTO(String tripsId, String truckId, Long driverId) {
        this.tripsId = tripsId;
        this.truckId = truckId;
        this.driverId = driverId;
    }
}
