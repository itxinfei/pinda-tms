package com.itheima.pinda.DTO;

import com.itheima.pinda.common.utils.DatePatterns;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class CustomerOrderDTO extends OrderDTO {

    /**
     * 通用时间格式（与全局 DatePatterns.DATE_TIME 对齐）。
     * 替代旧 com.itheima.pinda.utils.DateUtils.format / DEFAULT_DATE_TIME_FORMAT。
     */
    private static final DateTimeFormatter COMMON_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(DatePatterns.DATE_TIME);

    /**
     * 收件人省份
     */
    private String receiverProvince;

    /**
     * 收件人城市
     */
    private String receiverCity;

    /**
     * 收件人区县
     */
    private String receiverCounty;

    /**
     * 收件人详细全地址
     */
    private String receiverFullAddress;

    /**
     * 发件人省份
     */
    private String senderProvince;

    /**
     * 发件人城市
     */
    private String senderCity;

    /**
     * 发件人区县
     */
    private String senderCounty;

    /**
     * 发件人详细全地址
     */
    private String senderFullAddress;

    /**
     * 派送完成时间
     */
    private LocalDateTime actualDispathedTime;

    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;

    /**
     * 货品数量
     */

    private int quantity;
    /**
     * 运单id
     */
    private String tranOrderId;

    private String goodsType;
    private String goodsName;
    private BigDecimal goodsWeight;

    private String commonTimeStr;

    /**
     * 最后更新路由信息
     */
    private RouteDTO routeDTO;

    public CustomerOrderDTO(OrderDTO orderDTO, Map<Long, AreaDTO> areaMap, Map<String, OrderCargoDto> cargoMap, Map<String, TransportOrderDTO> transportOrderMap, Map<String, TaskPickupDispatchDTO> taskPickupDispatchDTOPullMap, Map<String, TaskPickupDispatchDTO> taskPickupDispatchDTOPushMap) {
        BeanUtils.copyProperties(orderDTO, this);
        this.receiverProvince = areaMap.get(parseLong(this.getReceiverProvinceId())) != null ? areaMap.get(parseLong(this.getReceiverProvinceId())).getName() : "";
        this.receiverCity = areaMap.get(parseLong(this.getReceiverCityId())) != null ? areaMap.get(parseLong(this.getReceiverCityId())).getName() : "";
        this.receiverCounty = areaMap.get(parseLong(this.getReceiverCountyId())) != null ? areaMap.get(parseLong(this.getReceiverCountyId())).getName() : "";
        this.receiverFullAddress = this.receiverProvince + this.receiverCity + this.receiverCounty + this.getReceiverAddress();

        this.senderProvince = areaMap.get(parseLong(this.getSenderProvinceId())) != null ? areaMap.get(parseLong(this.getSenderProvinceId())).getName() : "";
        this.senderCity = areaMap.get(parseLong(this.getSenderCityId())) != null ? areaMap.get(parseLong(this.getSenderCityId())).getName() : "";
        this.senderCounty = areaMap.get(parseLong(this.getSenderCountyId())) != null ? areaMap.get(parseLong(this.getSenderCountyId())).getName() : "";
        this.senderFullAddress = this.senderProvince + this.senderCity + this.senderCounty + this.getSenderAddress();

        this.actualDispathedTime = taskPickupDispatchDTOPushMap.get(this.getId()) != null ? taskPickupDispatchDTOPushMap.get(this.getId()).getActualEndTime() : null;
        this.cancelTime = taskPickupDispatchDTOPullMap.get(this.getId()) != null ? taskPickupDispatchDTOPullMap.get(this.getId()).getCancelTime() : null;

        // 运单号会下发给 App，雪花 ID 保持 String；先判空避免出现 "null" 字符串
        TransportOrderDTO tranOrder = transportOrderMap.get(this.getId());
        this.tranOrderId = tranOrder != null && tranOrder.getOrderId() != null
                ? String.valueOf(tranOrder.getOrderId())
                : null;

        if (!CollectionUtils.isEmpty(cargoMap) && cargoMap.get(this.getId()) != null) {
            OrderCargoDto cargo = cargoMap.get(this.getId());
            this.quantity = (cargo.getQuantity() != null ? cargo.getQuantity() : 0);
            this.goodsType = cargo.getGoodsTypeId();
            this.goodsName = cargo.getName();
            this.goodsWeight = cargo.getWeight();
        }

        if (this.getCreateTime() != null) {
            commonTimeStr = "下单时间：" + this.getCreateTime().format(COMMON_TIME_FORMATTER);
        }
        if (this.getActualDispathedTime() != null) {
            commonTimeStr = "签收时间：" + this.getActualDispathedTime().format(COMMON_TIME_FORMATTER);
        }
    }

    private Long parseLong(Object value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        return Long.parseLong(value.toString());
    }
}
