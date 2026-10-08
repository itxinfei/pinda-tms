package com.itheima.pinda.feign;

import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.DTO.ExceptionReportFeignDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 异常上报 Feign 客户端（指向 pd-netty）
 *
 * <p>快递员端 D-41 异常上报：异常记录落在 pd-netty 的 pd_alarm_record，
 * 快递员端<b>不能直连内部服务</b>，须经此 Feign 走内部调用。</p>
 *
 * <p>与 {@code com.itheima.pinda.feign.courier.AppCourierFeign} 同为指向 pd-netty 的客户端，
 * 之所以各自模块内定义而不放公共 api 模块，与现有拆分方式保持一致。</p>
 */
@FeignClient(name = "pd-netty")
public interface ExceptionReportFeign {

    /**
     * 上报配送环节异常（破损/拒收/地址错误）
     *
     * @param dto 异常上报入参（三要素：类型+照片+备注）
     * @return pd-netty 返回结果，data 为落库后的告警记录
     */
    @PostMapping("/alarm/report")
    Result report(@RequestBody ExceptionReportFeignDTO dto);
}