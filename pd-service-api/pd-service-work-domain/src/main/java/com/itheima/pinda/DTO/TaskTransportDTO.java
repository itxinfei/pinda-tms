package com.itheima.pinda.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 运输任务
 *
 * @author jpfss
 */
@Data
public class TaskTransportDTO implements Serializable {


    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    private String id;

    @Schema(description = "车次id")
    private String transportTripsId;

    @Schema(description = "起始机构id")
    private String startAgencyId;

    @Schema(description = "目的机构id")
    private String endAgencyId;

    @Schema(description = "任务状态，1为待执行（对应 待提货）、2为进行中（对应在途）、3为待确认（保留状态）、4为已完成（对应 已交付）、5为已取消")
    private Integer status;

    @Schema(description = "任务分配状态(1未分配2已分配3待人工分配)")
    private Integer assignedStatus;

    @Schema(description = "满载状态(1.半载2.满载3.空载)")
    private Integer loadingStatus;

    @Schema(description = "车辆id")
    private String truckId;

    @Schema(description = "提货凭证")
    private String cargoPickUpPicture;

    @Schema(description = "货物照片")
    private String cargoPicture;

    @Schema(description = "运回单凭证")
    private String transportCertificate;

    @Schema(description = "计划发车时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime planDepartureTime;

    @Schema(description = "实际发车时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime actualDepartureTime;

    @Schema(description = "计划到达时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime planArrivalTime;

    @Schema(description = "实际到达时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime actualArrivalTime;

    @Schema(description = "计划提货时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime planPickUpGoodsTime;

    @Schema(description = "实际提货时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime actualPickUpGoodsTime;

    @Schema(description = "计划交付时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime planDeliveryTime;

    @Schema(description = "实际交付时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime actualDeliveryTime;

    @Schema(description = "交付货物照片")
    private String deliverPicture;
    @Schema(description = "提货纬度")
    private String deliveryLatitude;
    @Schema(description = "提货经度")
    private String deliveryLongitude;
    @Schema(description = "交付纬度")
    private String deliverLatitude;
    @Schema(description = "交付经度")
    private String deliverLongitude;

    @Schema(description = "任务创建时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime createTime;

    @Schema(description = "运单id列表")
    private List<String> transportOrderIds;

    @Schema(description = "运单数量")
    private Integer transportOrderCount;

    @Schema(description = "页码")
    private Integer page;

    @Schema(description = "页尺寸")
    private Integer pageSize;

    @Schema(description = "id列表")
    List<String> ids;
}
