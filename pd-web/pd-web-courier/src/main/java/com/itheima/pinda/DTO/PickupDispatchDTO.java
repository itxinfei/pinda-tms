package com.itheima.pinda.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.itheima.pinda.common.utils.DateUtils;
import com.itheima.pinda.enums.pickuptask.PickupDispatchTaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Data
public class PickupDispatchDTO implements Serializable {
    /**
     * 订单号
     */
    @Schema(description = "订单号")
    private String orderNumber;
    /**
     * 运单号
     */
    @Schema(description = "运单号")
    private String tranOrderId;

    /**
     * 任务类型，1为取件任务，2为派件任务3为取消的任务
     */
    @Schema(description = "任务类型")
    private Integer taskType;

    /**
     * 任务状态，详见取派件任务状态[1.下单2.已取件3.网点入库，待装车4.网点出库，待派送5.派送中6.已签收(或拒收)7已取消]
     */
    @Schema(description = "任务状态")
    private Integer status;

    /**
     * 收件人
     */
    @Schema(description = "收件人")
    private String receiver;

    /**
     * 发件人
     */
    @Schema(description = "发件人")
    private String sender;

    /**
     * 收件人地址
     */
    @Schema(description = "收件人地址")
    private String receiverAddress;

    /**
     * 发件人地址
     */
    @Schema(description = "发件人地址")
    private String senderAddress;

    /**
     * 目的地 取件时为发件人地址  派件是为收件人地址
     */
    @Schema(description = "目的地")
    private String address;

    /**
     * 预计取派件时间
     * 可根据当前时间计算剩余时间
     */
    @Schema(description = "预计取派件时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime estimatedTime;
    /**
     * 实际取派件时间
     */
    @Schema(description = "实际取派件时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime actualTime;

    /**
     * 剩余时间
     */
    @Schema(description = "剩余时间")
    private Long finishTime;

    @Schema(description = "主键")
    private String id;

    public PickupDispatchDTO() {

    }

    public PickupDispatchDTO(TaskPickupDispatchDTO item, Map<Long, TransportOrderDTO> tranOrderMap, Map<String, OrderDTO> orderMap, Map<Long, AreaDTO> areaMap) {
        OrderDTO order = orderMap.get(String.valueOf(item.getOrderId()));
        log.info("构建快递员取件任务：{},{}", item.getOrderId(), order);
        this.id = String.valueOf(item.getId());
        this.orderNumber = String.valueOf(item.getOrderId());
        TransportOrderDTO tranOrder = tranOrderMap.get(item.getOrderId());
        this.tranOrderId = tranOrder != null ? String.valueOf(tranOrder.getId()) : "";
        this.taskType = item.getTaskType();
        this.status = item.getStatus();
        this.receiver = order.getReceiverName();
        this.sender = order.getSenderName();
        this.estimatedTime = item.getEstimatedEndTime();
        this.actualTime = item.getActualEndTime();
        if (estimatedTime != null) {
            log.info("时间计算：{},{}", LocalDateTime.now(), estimatedTime);
            log.info("时间计算：{},{}", LocalDateTime.now(), DateUtils.getUTCTime(estimatedTime));
            java.time.Duration duration = java.time.Duration.between(LocalDateTime.now(), DateUtils.getUTCTime(estimatedTime));
            log.info("时间计算：{},{}", duration, duration.toMinutes());
            this.finishTime = duration.toMinutes();
        } else {
            this.finishTime = 0L;
        }

        // 发件人地址
        this.senderAddress = "";
        this.senderAddress += areaMap.get(parseLong(order.getSenderProvinceId())) == null ? "" : areaMap.get(parseLong(order.getSenderProvinceId())).getName();
        this.senderAddress += areaMap.get(parseLong(order.getSenderCityId())) == null ? "" : areaMap.get(parseLong(order.getSenderCityId())).getName();
        this.senderAddress += areaMap.get(parseLong(order.getSenderCountyId())) == null ? "" : areaMap.get(parseLong(order.getSenderCountyId())).getName();
        this.senderAddress += order.getSenderAddress();

        // 收件人地址
        this.receiverAddress = "";
        this.receiverAddress += areaMap.get(parseLong(order.getReceiverProvinceId())) == null ? "" : areaMap.get(parseLong(order.getReceiverProvinceId())).getName();
        this.receiverAddress += areaMap.get(parseLong(order.getReceiverCityId())) == null ? "" : areaMap.get(parseLong(order.getReceiverCityId())).getName();
        this.receiverAddress += areaMap.get(parseLong(order.getReceiverCountyId())) == null ? "" : areaMap.get(parseLong(order.getReceiverCountyId())).getName();
        this.receiverAddress += order.getReceiverAddress();

        if (taskType == PickupDispatchTaskType.PICKUP.getCode()) {
            // 发件人地址
            this.address = this.senderAddress;
        } else {
            // 收件人地址
            this.address = this.receiverAddress;
        }
    }

    private Long parseLong(String value) {
        if (StringUtils.isNotBlank(value)) {
            return Long.parseLong(value);
        }
        return null;
    }
}
