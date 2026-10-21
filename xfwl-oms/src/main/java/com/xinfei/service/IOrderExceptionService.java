package com.xinfei.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.OrderException;

/**
 * 订单异常记录服务（ORD-04）
 */
public interface IOrderExceptionService extends IService<OrderException> {

    /**
     * 登记订单异常（不改变订单本体状态，仅挂载异常标记）
     *
     * @param orderId       订单ID
     * @param exceptionType 异常类型：1滞留 2破损 3退回 4其他
     * @param description   异常描述
     * @param operatorId    操作人ID（登录态）
     * @return 登记成功的异常记录
     * @throws IllegalArgumentException 订单不存在或参数非法
     */
    OrderException register(Long orderId, Integer exceptionType, String description, Long operatorId);

    /**
     * 处理异常（待处理→已处理，留痕处理人/时间）
     *
     * @param id           异常记录ID
     * @param handleResult 处理结果
     * @param operatorName 处理人（登录名）
     * @return 是否处理成功；记录不存在或已处理时返回 false
     */
    boolean handle(Long id, String handleResult, String operatorName);

    /**
     * 分页查询
     *
     * @param page          页码
     * @param pageSize      页尺寸
     * @param orderId       订单ID（精确，可空）
     * @param exceptionType 异常类型（可空）
     * @param handleStatus  处理状态（可空）
     * @return 分页结果
     */
    IPage<OrderException> findByPage(Integer page, Integer pageSize, Long orderId,
                                     Integer exceptionType, Integer handleStatus);
}
