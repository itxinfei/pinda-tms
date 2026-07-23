package com.itheima.pinda.feign.hystrix;

import com.itheima.pinda.DTO.OrderCargoDto;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.CargoFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 货物Feign接口熔断降级
 */
@Slf4j
@Component
public class CargoFeignFallback implements CargoFeign {

    @Override
    public List<OrderCargoDto> findAll(String tranOrderId, String orderId) {
        log.warn("远程调用 pd-oms 失败: findAll(tranOrderId={}, orderId={}), 返回空列表", tranOrderId, orderId);
        return Collections.emptyList();
    }

    @Override
    public OrderCargoDto save(OrderCargoDto dto) {
        log.warn("远程调用 pd-oms 失败: save({}), 返回null", dto);
        return null;
    }

    @Override
    public OrderCargoDto update(String id, OrderCargoDto dto) {
        log.warn("远程调用 pd-oms 失败: update({}), 返回null", id);
        return null;
    }

    @Override
    public Result del(String id) {
        log.warn("远程调用 pd-oms 失败: del({}), 返回失败结果", id);
        return Result.error("服务降级，del执行失败");
    }

    @Override
    public OrderCargoDto findById(String id) {
        log.warn("远程调用 pd-oms 失败: findById({}), 返回null", id);
        return null;
    }

    @Override
    public List<OrderCargoDto> list(List<String> orderIds) {
        log.warn("远程调用 pd-oms 失败: list({}), 返回空列表", orderIds);
        return Collections.emptyList();
    }
}
