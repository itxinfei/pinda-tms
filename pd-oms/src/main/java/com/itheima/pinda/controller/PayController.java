package com.itheima.pinda.controller;

import com.alibaba.fastjson2.JSON;
import com.itheima.pinda.common.context.RequestContext;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.entity.PaymentOrder;
import com.itheima.pinda.service.IPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一支付接口
 *
 * <p>对外暴露支付创建、回调、查询、退款能力，
 * 支持微信/支付宝/模拟渠道（由 pay.channel 配置决定）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/pay")
@Tag(name = "统一支付")
public class PayController {

    @Autowired
    private IPayService payService;

    /**
     * 创建支付
     *
     * @param orderId 订单ID
     * @return 支付单（含 prepayParams 供前端拉起支付）
     */
    @Operation(summary = "创建支付")
    @PostMapping("/create/{orderId}")
    public Result create(@PathVariable(name = "orderId") String orderId) {
        // 身份校验（deny-by-default）：网关应透传 userid，直连端口拒绝
        if (StringUtils.isBlank(RequestContext.getUserId())) {
            return Result.error(401, "未登录或身份信息缺失");
        }
        PaymentOrder paymentOrder = payService.createPayment(orderId);
        if (paymentOrder == null) {
            return Result.error(400, "创建支付失败（订单不存在或已支付）");
        }
        return Result.ok().put("data", paymentOrder);
    }

    /**
     * 支付回调（微信/支付宝/模拟渠道统一回调入口）
     *
     * <p>微信 APIv3 把签名放在 HTTP 头、报文体需按原文验签，所以这里保留 rawBody 与原样
     * 头信息传给渠道；否则渠道侧拿不到签名，只能退化成"字段非空即放行"。</p>
     *
     * @param channel 渠道编码: wechat/alipay/mock
     * @return 处理结果
     */
    @Operation(summary = "支付回调")
    @PostMapping("/callback/{channel}")
    public Result callback(@PathVariable(name = "channel") String channel,
                           @RequestBody(required = false) String rawBody,
                           HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        // 支付宝异步通知是 x-www-form-urlencoded，键值在 request 参数里而不是请求体
        request.getParameterMap().forEach((k, v) ->
                params.put(k, v == null || v.length == 0 ? null : v[0]));
        if (StringUtils.isNotBlank(rawBody)) {
            try {
                JSON.parseObject(rawBody).forEach((k, v) -> params.put(k, v == null ? null : String.valueOf(v)));
            } catch (Exception e) {
                log.debug("[支付回调] 报文不是 JSON（可能是表单回调），已按 request 参数处理: channel={}", channel);
            }
        }
        // 只有微信渠道注入 APIv3 签名头：支付宝的验签要按参数原样拼接，多塞键会把它的验签带坏
        if (PaymentOrder.CHANNEL_WECHAT.equals(channel)) {
            params.put("wechatpay_signature", request.getHeader("Wechatpay-Signature"));
            params.put("wechatpay_timestamp", request.getHeader("Wechatpay-Timestamp"));
            params.put("wechatpay_nonce", request.getHeader("Wechatpay-Nonce"));
            params.put("wechatpay_serial", request.getHeader("Wechatpay-Serial"));
            params.put("rawBody", rawBody);
        }
        boolean success = payService.handleCallback(channel, params);
        return success ? Result.ok() : Result.error(500, "支付回调处理失败");
    }

    /**
     * 查询支付状态
     *
     * @param orderId 订单ID
     * @return 支付单
     */
    @Operation(summary = "查询支付状态")
    @GetMapping("/query/{orderId}")
    public Result query(@PathVariable(name = "orderId") String orderId) {
        // 身份校验（deny-by-default）
        if (StringUtils.isBlank(RequestContext.getUserId())) {
            return Result.error(401, "未登录或身份信息缺失");
        }
        PaymentOrder paymentOrder = payService.queryPayment(orderId);
        if (paymentOrder == null) {
            return Result.error(404, "未查询到支付单");
        }
        return Result.ok().put("data", paymentOrder);
    }

    /**
     * 退款（管理端操作，需网关角色鉴权：此处仅做身份校验兜底）
     *
     * @param orderId 订单ID
     * @return 处理结果
     */
    @Operation(summary = "退款")
    @PostMapping("/refund/{orderId}")
    public Result refund(@PathVariable(name = "orderId") String orderId) {
        // 身份校验（deny-by-default）；管理端角色校验由网关/管理端前端控制
        if (StringUtils.isBlank(RequestContext.getUserId())) {
            return Result.error(401, "未登录或身份信息缺失");
        }
        boolean success = payService.refund(orderId);
        return success ? Result.ok() : Result.error(500, "退款失败");
    }
}
