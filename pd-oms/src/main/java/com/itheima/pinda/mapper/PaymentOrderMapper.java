package com.itheima.pinda.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.pinda.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 订单支付单 Mapper
 */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    /**
     * 按支付流水号跨租户反查支付单所属租户。
     *
     * <p>外部支付渠道（微信/支付宝）服务器回调不携带登录态，网关注入不了 tenantId 头，
     * 回调线程需要先靠本方法拿到真实租户、临时建立系统态租户上下文，后续查询/更新才能命中数据。
     * pay_no 全局唯一（uk_pay_no）且回调已经过渠道验签，故显式忽略租户隔离。</p>
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT tenant_id FROM pd_payment_order WHERE pay_no = #{payNo} LIMIT 1")
    Long selectTenantIdByPayNo(@Param("payNo") String payNo);
}
