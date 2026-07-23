package com.itheima.pinda.feign.hystrix;

import com.itheima.pinda.DTO.TransportOrderDTO;
import com.itheima.pinda.DTO.TransportOrderSearchDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.TransportOrderFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 运单Feign接口熔断降级
 */
@Slf4j
@Component
public class TransportOrderFeignFallback implements TransportOrderFeign {

    @Override
    public TransportOrderDTO save(TransportOrderDTO dto) {
        log.error("远程调用 pd-work 失败: save({}), 返回null", dto);
        return null;
    }

    @Override
    public TransportOrderDTO updateById(String id, TransportOrderDTO dto) {
        log.error("远程调用 pd-work 失败: updateById({}), 返回null", id);
        return null;
    }

    @Override
    public PageResponse<TransportOrderDTO> findByPage(Integer page, Integer pageSize, String orderId, Integer status, Integer schedulingStatus) {
        log.error("远程调用 pd-work 失败: findByPage(page={}, pageSize={}, orderId={}, status={}, schedulingStatus={}), 返回空分页", page, pageSize, orderId, status, schedulingStatus);
        PageResponse<TransportOrderDTO> result = new PageResponse<>();
        result.setCounts(0L);
        result.setPagesize(pageSize);
        result.setPages(0L);
        result.setPage(page);
        result.setItems(Collections.emptyList());
        return result;
    }

    @Override
    public TransportOrderDTO findById(String id) {
        log.error("远程调用 pd-work 失败: findById({}), 返回null", id);
        return null;
    }

    @Override
    public TransportOrderDTO findByOrderId(String orderId) {
        log.error("远程调用 pd-work 失败: findByOrderId({}), 返回null", orderId);
        return null;
    }

    @Override
    public List<TransportOrderDTO> findByOrderIds(List<String> ids) {
        log.error("远程调用 pd-work 失败: findByOrderIds({}), 返回空列表", ids);
        return Collections.emptyList();
    }

    @Override
    public List<TransportOrderDTO> list(TransportOrderSearchDTO transportOrderSearchDTO) {
        log.error("远程调用 pd-work 失败: list({}), 返回空列表", transportOrderSearchDTO);
        return Collections.emptyList();
    }
}
