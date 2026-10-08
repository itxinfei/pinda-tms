package com.itheima.pinda.feign;

import com.itheima.pinda.DTO.DriverExceptionReportDTO;
import com.itheima.pinda.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 在途异常上报 Feign 客户端（指向 pd-netty）
 *
 * <p>司机端 D-57 在途异常上报：异常记录落在 pd-netty 的 pd_alarm_record，
 * 司机端<b>不能直连内部服务</b>，须经此 Feign 走内部调用。</p>
 */
@FeignClient(name = "pd-netty")
public interface DriverExceptionReportFeign {

    /**
     * 上报在途异常（车辆故障/货物损失/延误）
     *
     * @param dto 异常上报入参（三要素：类型+照片+备注）
     * @return pd-netty 返回结果，data 为落库后的告警记录
     */
    @PostMapping("/alarm/report")
    Result report(@RequestBody DriverExceptionReportDTO dto);
}