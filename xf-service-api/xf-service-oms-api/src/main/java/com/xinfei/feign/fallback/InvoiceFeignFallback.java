package com.xinfei.feign.fallback;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.InvoiceFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 电子发票（开票）Feign 熔断降级
 */
@Slf4j
@Component
public class InvoiceFeignFallback implements InvoiceFeign {

    @Override
    public Result apply(Long billId, Integer type, String taxNo, String mailAddress) {
        log.warn("远程调用 xf-oms 失败: apply(billId={})", billId);
        return Result.error("服务降级，开票申请失败");
    }

    @Override
    public Result issue(Long id) {
        log.warn("远程调用 xf-oms 失败: issue(id={})", id);
        return Result.error("服务降级，开票失败");
    }

    @Override
    public Result mail(Long id) {
        log.warn("远程调用 xf-oms 失败: mail(id={})", id);
        return Result.error("服务降级，邮寄登记失败");
    }

    @Override
    public Result voidInvoice(Long id, String reason) {
        log.warn("远程调用 xf-oms 失败: voidInvoice(id={})", id);
        return Result.error("服务降级，作废失败");
    }

    @Override
    public Result page(Integer page, Integer pageSize, String customerId, Integer status) {
        log.warn("远程调用 xf-oms 失败: page(customerId={}, status={})", customerId, status);
        return Result.error("服务降级，分页查询失败");
    }
}
