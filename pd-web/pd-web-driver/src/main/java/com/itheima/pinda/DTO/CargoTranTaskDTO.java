package com.itheima.pinda.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.itheima.pinda.vo.AgencyVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargoTranTaskDTO implements Serializable {
    /**
     * 起点机构
     */
    @Schema(description = "起点")
    private AgencyDTO startAgency;


    /**
     * 目的机构
     */
    @Schema(description = "目的机构地址")
    private AgencyDTO endAgency;

    /**
     * id
     */
    @Schema(description = "id")
    private String id;


    /**
     * 任务编号
     */
    @Schema(description = "任务编号")
    private String taskNo;

    /**
     * 关联运单id
     */
    @Schema(description = "关联运单id")
    private String tranOrderId;

    /**
     * 车次id
     */
    @Schema(description = "车次id")
    private String transportTripsId;

    /**
     * 车次
     */
//    @Schema(description = "车次")
//    private String trainNumber;

    /**
     * 运输任务状态(1.待人工调度2.待提货3.待发车4.在途6.已到达7.已交付)
     */
    @Schema(description = "运输任务状态(1.待人工调度2.待提货3.待发车4.在途6.已到达7.已交付)")
    private Integer status;

    /**
     * 满载状态(1.半载2.满载3.空载)
     */
    @Schema(description = "满载状态(1.半载2.满载3.空载)")
    private Integer loadingStatus;

    /**
     * 司机id
     */
    @Schema(description = "司机id")
    private String driver;

    /**
     * 车辆id
     */
    @Schema(description = "车辆id")
    private String truckId;

    /**
     * 运单数量
     */
    @Schema(description = "运单数量")
    private Integer tranOrderNum;

    /**
     * 提货凭证
     */
    @Schema(description = "提货凭证")
    private String cargoPickUpPicture;

    /**
     * 货物照片
     */
    @Schema(description = "货物照片")
    private String cargoPicture;

    /**
     * 运回单凭证
     */
    @Schema(description = "运回单凭证")
    private String transportCertificate;
    /**
     * 货物照片
     */
    @Schema(description = "货物照片")
    private String deliverPicture;

    /**
     * 计划发车时间
     */
    @Schema(description = "计划发车时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime planDepartureTime;

    /**
     * 实际发车时间
     */
    @Schema(description = "实际发车时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime actualDepartureTime;

    /**
     * 计划到达时间
     */
    @Schema(description = "计划到达时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime planArrivalTime;

    /**
     * 实际到达时间
     */
    @Schema(description = "实际到达时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime actualArrivalTime;

    /**
     * 计划提货时间
     */
    @Schema(description = "计划提货时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime planPickUpGoodsTime;

    /**
     * 实际提货时间
     */
    @Schema(description = "实际提货时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime actualPickUpGoodsTime;

    /**
     * 计划交付时间
     */
    @Schema(description = "计划交付时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime planDeliveryTime;

    /**
     * 实际交付时间
     */
    @Schema(description = "实际交付时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime actualDeliveryTime;

    @Schema(description = "是否显示")
    private boolean disable;

    public CargoTranTaskDTO(DriverJobDTO item, Map<Long, TaskTransportDTO> taskTransportDTOMap, Map<String, AgencyVo> agencyMap) {
        AgencyVo agencyVoStart = agencyMap.get(idStr(item.getStartOrgId()));
        AgencyVo agencyVoEnd = agencyMap.get(idStr(item.getEndOrgId()));
        TaskTransportDTO taskTransportDTO = taskTransportDTOMap.get(item.getTaskTransportId());
        this.cargoPickUpPicture = taskTransportDTO.getPickupPicture();
        this.cargoPicture = taskTransportDTO.getCargoPicture();
        this.transportCertificate = taskTransportDTO.getCertificatePicture();
        this.deliverPicture = taskTransportDTO.getDeliverPicture();
        this.taskNo = idStr(taskTransportDTO.getId());
        this.transportTripsId = idStr(taskTransportDTO.getTripsId());
        this.truckId = idStr(taskTransportDTO.getTruckId());
        this.planPickUpGoodsTime = taskTransportDTO.getPlanPickUpTime();
        this.actualPickUpGoodsTime = taskTransportDTO.getActualPickUpTime();
        this.planDeliveryTime = taskTransportDTO.getPlanDeliveryTime();
        this.actualDeliveryTime = taskTransportDTO.getActualDeliveryTime();
        this.planDepartureTime = taskTransportDTO.getPlanDepartureTime();
        this.tranOrderNum = taskTransportDTO.getTransportOrderCount();

        this.driver = idStr(item.getDriverId());

        this.status = item.getStatus();
        this.id = idStr(item.getId());
        this.actualDepartureTime = item.getActualDepartureTime();
        this.planArrivalTime = item.getPlanArrivalTime();
        this.actualArrivalTime = item.getActualArrivalTime();

        this.startAgency = new AgencyDTO(agencyVoStart);
        this.endAgency = new AgencyDTO(agencyVoEnd);
    }

    /**
     * 面向 App 的 ID 字段保持 String（雪花 ID 超过 JS 安全整数上限），work DTO 为 Long，在调用边界转换；null 透传。
     */
    private static String idStr(Long id) {
        return id == null ? null : String.valueOf(id);
    }
}
