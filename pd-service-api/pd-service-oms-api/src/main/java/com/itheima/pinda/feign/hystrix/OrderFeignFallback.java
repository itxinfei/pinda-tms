package com.itheima.pinda.feign.hystrix;

import com.itheima.pinda.DTO.OrderDTO;
import com.itheima.pinda.DTO.OrderLocationDto;
import com.itheima.pinda.DTO.OrderSearchDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.entity.Order;
import com.itheima.pinda.feign.OrderFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 订单Feign接口熔断降级
 */
@Slf4j
@Component
public class OrderFeignFallback implements OrderFeign {

    @Override
    public OrderDTO save(OrderDTO orderDTO) {
        log.warn("远程调用 pd-oms 失败: save({}), 返回null", orderDTO);
        return null;
    }

    @Override
    public OrderDTO updateById(String id, OrderDTO orderDTO) {
        log.warn("远程调用 pd-oms 失败: updateById({}), 返回null", id);
        return null;
    }

    @Override
    public PageResponse<OrderDTO> findByPage(OrderDTO orderDTO) {
        log.warn("远程调用 pd-oms 失败: findByPage({}), 返回空分页", orderDTO);
        PageResponse<OrderDTO> result = new PageResponse<>();
        result.setCounts(0L);
        result.setPagesize(orderDTO != null ? orderDTO.getPageSize() : 10);
        result.setPages(0L);
        result.setPage(orderDTO != null ? orderDTO.getPage() : 1);
        result.setItems(Collections.emptyList());
        return result;
    }

    @Override
    public OrderDTO findById(String id) {
        log.warn("远程调用 pd-oms 失败: findById({}), 返回null", id);
        return null;
    }

    @Override
    public PageResponse<OrderDTO> pageLikeForCustomer(OrderSearchDTO orderSearchDTO) {
        log.warn("远程调用 pd-oms 失败: pageLikeForCustomer({}), 返回空分页", orderSearchDTO);
        PageResponse<OrderDTO> result = new PageResponse<>();
        result.setCounts(0L);
        result.setPagesize(orderSearchDTO != null ? orderSearchDTO.getPageSize() : 10);
        result.setPages(0L);
        result.setPage(orderSearchDTO != null ? orderSearchDTO.getPage() : 1);
        result.setItems(Collections.emptyList());
        return result;
    }

    @Override
    public List<OrderDTO> findByIds(List<String> ids) {
        log.warn("远程调用 pd-oms 失败: findByIds({}), 返回空列表", ids);
        return Collections.emptyList();
    }

    @Override
    public List<Order> list(OrderSearchDTO orderSearchDTO) {
        log.warn("远程调用 pd-oms 失败: list({}), 返回空列表", orderSearchDTO);
        return Collections.emptyList();
    }

    @Override
    public Map getOrderMsg(OrderDTO orderAddDto) {
        log.warn("远程调用 pd-oms 失败: getOrderMsg({}), 返回null", orderAddDto);
        return null;
    }

    @Override
    public OrderLocationDto saveOrUpdateLoccation(OrderLocationDto orderLocationDto) {
        log.warn("远程调用 pd-oms 失败: saveOrUpdateLoccation({}), 返回null", orderLocationDto);
        return null;
    }

    @Override
    public OrderLocationDto selectByOrderId(String orderId) {
        log.warn("远程调用 pd-oms 失败: selectByOrderId({}), 返回null", orderId);
        return null;
    }

    @Override
    public int deleteOrderLocation(OrderLocationDto orderLocationDto) {
        log.warn("远程调用 pd-oms 失败: deleteOrderLocation({}), 返回0", orderLocationDto);
        return 0;
    }

    @Override
    public OrderDTO omsSeataTest(OrderDTO orderDTO) {
        log.warn("远程调用 pd-oms 失败: omsSeataTest({}), 返回null", orderDTO);
        return null;
    }
}
