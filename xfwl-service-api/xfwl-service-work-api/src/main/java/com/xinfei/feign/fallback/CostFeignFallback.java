package com.xinfei.feign.fallback;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.CostFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 趟次成本 Feign 熔断降级
 */
@Slf4j
@Component
public class CostFeignFallback implements CostFeign {

    @Override
    public Result save(Long taskTransportId, Long truckId, Long driverId, String costType,
                       BigDecimal amount, String occurTime, String voucher, String remark) {
        log.warn("远程调用 xfwl-work 失败: save(taskId={}, type={})", taskTransportId, costType);
        return Result.error("服务降级，成本录入失败");
    }

    @Override
    public Result taskDetail(Long taskId) {
        log.warn("远程调用 xfwl-work 失败: taskDetail(taskId={})", taskId);
        return Result.error("服务降级，趟成本查询失败");
    }

    @Override
    public Result page(Integer page, Integer pageSize, Long taskTransportId, Long truckId, String costType) {
        log.warn("远程调用 xfwl-work 失败: page(taskId={})", taskTransportId);
        return Result.error("服务降级，成本分页失败");
    }
}
