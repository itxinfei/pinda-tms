package com.xinfei.feign.carrier.fallback;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.carrier.CarrierFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 承运商 Feign 熔断降级（DSP-2 第一期）
 */
@Slf4j
@Component
public class CarrierFeignFallback implements CarrierFeign {

    @Override
    public Result save(java.util.Map<String, Object> body) {
        log.warn("远程调用 xfwl-base 失败: carrier save({}), 返回降级结果", body);
        return Result.error("服务降级，承运商保存失败");
    }

    @Override
    public Result page(Integer page, Integer pageSize, String code, String name, Integer status) {
        log.warn("远程调用 xfwl-base 失败: carrier page(page={}, pageSize={}, code={}, name={}, status={}), 返回降级结果",
                page, pageSize, code, name, status);
        return Result.error("服务降级，承运商分页查询失败");
    }

    @Override
    public Result detail(Long id) {
        log.warn("远程调用 xfwl-base 失败: carrier detail({}), 返回降级结果", id);
        return Result.error("服务降级，承运商详情查询失败");
    }

    @Override
    public Result changeStatus(Long id, Integer status) {
        log.warn("远程调用 xfwl-base 失败: carrier changeStatus({}, {}), 返回降级结果", id, status);
        return Result.error("服务降级，承运商状态变更失败");
    }

    @Override
    public Result saveTruck(java.util.Map<String, Object> body) {
        log.warn("远程调用 xfwl-base 失败: carrier saveTruck({}), 返回降级结果", body);
        return Result.error("服务降级，挂靠车辆保存失败");
    }

    @Override
    public Result saveDriver(java.util.Map<String, Object> body) {
        log.warn("远程调用 xfwl-base 失败: carrier saveDriver({}), 返回降级结果", body);
        return Result.error("服务降级，挂靠司机保存失败");
    }

    @Override
    public Result capacity(String keyword, Long truckTypeId, BigDecimal minLoad) {
        log.warn("远程调用 xfwl-base 失败: carrier capacity(keyword={}, truckTypeId={}, minLoad={}), 返回降级结果",
                keyword, truckTypeId, minLoad);
        return Result.error("服务降级，运力池查询失败");
    }
}
