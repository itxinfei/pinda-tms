package com.xinfei.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.OrderFee;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单附加费明细服务（POD-03 第一期）
 */
public interface IOrderFeeService extends IService<OrderFee> {

    /** 按订单查明细（create_time 升序） */
    List<OrderFee> listByOrderId(String orderId);

    /** 订单附加费合计 */
    BigDecimal sumByOrderId(String orderId);

    /**
     * 人工补录附加费（feeCode 须存在于费用项配置；来源强制=人工）
     *
     * @return 落库明细；参数非法抛 IllegalArgumentException
     */
    OrderFee addManual(OrderFee fee, Long operatorId, String operatorName);

    /**
     * 按费用项配置自动生成订单附加费明细：
     * 固定金额项=原额；按运费比例项=订单运费×ratio。同 fee_code 已有明细幂等跳过。
     *
     * @return 本次新生成的明细列表（可能为空=无可生成项或全部已存在）
     */
    List<OrderFee> calculateForOrder(Long orderId, Long operatorId, String operatorName);
}
