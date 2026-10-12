package com.itheima.pinda.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDTO implements Serializable {
    private static final long serialVersionUID = 3355577312030835991L;
    /**
     * id
     *
     * <p>⚠️ 过渡契约：String 主键（pd-oms 尚未切库）。String → Long 的统一收敛
     * 跟随《53-全量重构总体规划》执行，禁止单独改动类型。</p>
     */
    private String id;

    /**
     * 订单类型，1为同城订单，2为城际订单
     */
    private Integer orderType;


    /**
     * 取件类型，1为网点自寄，2为上门取件
     */
    private Integer pickupType;

    /**
     * 下单时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime createTime;

    /**
     * 客户id
     */
    private String memberId;

    /**
     * 收件人省份id
     */
    private String receiverProvinceId;

    /**
     * 收件人城市id
     */
    private String receiverCityId;

    /**
     * 收件人区县id
     */
    private String receiverCountyId;

    /**
     * 收件人详细地址
     */
    private String receiverAddress;
    /**
     * 收件人地址id
     */
    private String receiverAddressId;

    /**
     * 收件人姓名
     */
    private String receiverName;

    /**
     * 收件人电话
     */
    private String receiverPhone;

    /**
     * 发件人省份id
     */
    private String senderProvinceId;

    /**
     * 发件人城市id
     */
    private String senderCityId;

    /**
     * 发件人区县id
     */
    private String senderCountyId;

    /**
     * 发件人详细地址
     */
    private String senderAddress;

    /**
     * 发件人地址id
     */
    private String senderAddressId;

    /**
     * 发件人姓名
     */
    private String senderName;

    /**
     * 发件人电话
     */
    private String senderPhone;

    /**
     * 当前所在网点
     */
    private String currentAgencyId;

    /**
     * 付款方式,1.预结2到付
     */
    private Integer paymentMethod;

    /**
     * 付款状态,1.未付2已付
     */
    private Integer paymentStatus;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 预计到达时间
     */
    private LocalDateTime estimatedArrivalTime;

    /**
     * 距离
     */
    private BigDecimal distance;

    /**
     * 订单状态（紧凑码 1~12，见 OrderStatus）：
     * 1待取件 2已取件 3网点自寄 4网点入库 5待装车 6运输中 7网点出库 8待派送 9派送中 10已签收 11拒收 12已取消
     */
    private Integer status;

    /**
     * 取消原因（status=12 时必填）
     */
    private String cancelReason;

    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;

    /**
     * 下单时间范围起点（分页查询用：create_time >= startTime，yyyy-MM-dd HH:mm:ss）
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime startTime;

    /**
     * 下单时间范围终点（分页查询用：create_time <= endTime，yyyy-MM-dd HH:mm:ss）
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime endTime;

    /**
     * 页码
     */
    private Integer page;

    /**
     * 页尺寸
     */
    private Integer pageSize;

    private OrderCargoDto orderCargoDto;
}
