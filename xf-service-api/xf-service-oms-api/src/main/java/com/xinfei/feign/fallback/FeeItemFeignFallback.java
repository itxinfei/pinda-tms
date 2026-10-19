package com.xinfei.feign.fallback;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.FeeItemFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 附加费费用项配置 Feign 熔断降级
 */
@Slf4j
@Component
public class FeeItemFeignFallback implements FeeItemFeign {

    @Override
    public Result save(Map<String, Object> body) {
        log.warn("远程调用 xf-oms 失败: fee-item.save");
        return Result.error("服务降级，费用项保存失败");
    }

    @Override
    public Result disable(Long id) {
        log.warn("远程调用 xf-oms 失败: fee-item.disable(id={})", id);
        return Result.error("服务降级，费用项停用失败");
    }

    @Override
    public Result page(Integer page, Integer pageSize, String feeCode, Integer status) {
        log.warn("远程调用 xf-oms 失败: fee-item.page");
        return Result.error("服务降级，费用项查询失败");
    }
}
