package com.itheima.pinda.DTO;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 在途异常上报入参（司机端 · D-57）
 *
 * <p>本类仅为 Feign 传输载体，与 pd-netty 侧的
 * {@code com.itheima.pinda.DTO.ExceptionReportDTO} 字段一一对应。</p>
 *
 * <p>司机在运输途中上报车辆故障/货物损失/延误，落库复用
 * {@code pd_alarm_record}，与 GPS 自动告警同表共存。</p>
 */
@Data
public class DriverExceptionReportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 异常类型：VEHICLE_BREAKDOWN 车辆故障 / CARGO_LOSS 货物损失 / DELAY 延误 / OTHER 其他 */
    private String exceptionType;

    /** 上报人（司机）ID，由服务端从 token 取，不可由前端传他人 ID */
    private String reporterId;

    /** 关联运输任务ID（可选，在途上报建议传） */
    private String transportTaskId;

    /** 照片凭证ID列表，**必填不可为空**（D-46 三要素之一） */
    private List<String> attachmentIds;

    /** 异常备注说明，**必填不可为空**（D-46 三要素之一） */
    private String remark;

    /** 上报经度（BD09，可选） */
    private Double longitude;

    /** 上报纬度（BD09，可选） */
    private Double latitude;

    /** 司机侧上报固定为 false（服务端按 false 放行在途类类型） */
    private boolean courier;
}