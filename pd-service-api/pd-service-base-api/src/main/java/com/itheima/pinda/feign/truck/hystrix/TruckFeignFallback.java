package com.itheima.pinda.feign.truck.hystrix;

import com.itheima.pinda.DTO.truck.TruckDto;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.feign.truck.TruckFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 车辆Feign接口熔断降级
 */
@Slf4j
@Component
public class TruckFeignFallback implements TruckFeign {

    @Override
    public TruckDto saveTruck(TruckDto dto) {
        log.warn("远程调用 pd-base 失败: saveTruck({}), 返回null", dto);
        return null;
    }

    @Override
    public TruckDto fineById(String id) {
        log.warn("远程调用 pd-base 失败: fineById({}), 返回null", id);
        return null;
    }

    @Override
    public PageResponse<TruckDto> findByPage(Integer page, Integer pageSize, String truckTypeId, String licensePlate, String fleetId) {
        log.warn("远程调用 pd-base 失败: findByPage(page={}, pageSize={}, truckTypeId={}, licensePlate={}, fleetId={}), 返回空分页", page, pageSize, truckTypeId, licensePlate, fleetId);
        PageResponse<TruckDto> result = new PageResponse<>();
        result.setCounts(0L);
        result.setPagesize(pageSize);
        result.setPages(0L);
        result.setPage(page);
        result.setItems(Collections.emptyList());
        return result;
    }

    @Override
    public List<TruckDto> findAll(List<String> ids, String fleetId) {
        log.warn("远程调用 pd-base 失败: findAll(ids={}, fleetId={}), 返回空列表", ids, fleetId);
        return Collections.emptyList();
    }

    @Override
    public TruckDto update(String id, TruckDto dto) {
        log.warn("远程调用 pd-base 失败: update({}), 返回null", id);
        return null;
    }

    @Override
    public Integer count(String fleetId) {
        log.warn("远程调用 pd-base 失败: count(fleetId={}), 返回0", fleetId);
        return 0;
    }

    @Override
    public Result disable(String id) {
        log.warn("远程调用 pd-base 失败: disable({}), 返回失败结果", id);
        return Result.error("服务降级，disable执行失败");
    }
}
