package com.xinfei.feign.fallback;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.PriceAgreementFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 客户价格协议 Feign 熔断降级
 */
@Slf4j
@Component
public class PriceAgreementFeignFallback implements PriceAgreementFeign {

    @Override
    public Result save(Map<String, Object> body) {
        log.warn("远程调用 xf-oms 失败: price-agreement.save");
        return Result.error("服务降级，价格协议保存失败");
    }

    @Override
    public Result disable(Long id) {
        log.warn("远程调用 xf-oms 失败: price-agreement.disable(id={})", id);
        return Result.error("服务降级，价格协议停用失败");
    }

    @Override
    public Result page(Integer page, Integer pageSize, String memberId, Integer status) {
        log.warn("远程调用 xf-oms 失败: price-agreement.page");
        return Result.error("服务降级，价格协议查询失败");
    }
}
