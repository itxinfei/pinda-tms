package com.itheima.pinda.DTO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 异常上报入参（一线人员主动上报 · D-41 快递员异常上报 / D-57 司机在途异常上报）
 *
 * <p>定案 <b>D-46「类型 + 照片 + 备注」三要素</b>：三者缺一不可，照片为合规凭证。
 * 落库目标表 {@code pd_alarm_record}，与 GPS 自动检测告警同表共存。</p>
 */
@Data
@ApiModel("异常上报入参")
public class ExceptionReportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "异常类型", required = true,
            allowableValues = "GOODS_DAMAGED,REJECTED,ADDRESS_ERROR,VEHICLE_BREAKDOWN,CARGO_LOSS,DELAY,OTHER")
    private String exceptionType;

    @ApiModelProperty(value = "上报人ID（快递员ID / 司机ID，从 token 取，不可由前端传他人ID）", required = true)
    private String reporterId;

    @ApiModelProperty(value = "关联运输任务ID（可选，在途上报建议传）")
    private String transportTaskId;

    @ApiModelProperty(value = "照片凭证ID列表，**必填不可为空**（D-46 三要素之一）", required = true)
    private List<String> attachmentIds;

    @ApiModelProperty(value = "异常备注说明，**必填不可为空**（D-46 三要素之一）", required = true)
    private String remark;

    @ApiModelProperty(value = "上报经度（BD09，可选）")
    private Double longitude;

    @ApiModelProperty(value = "上报纬度（BD09，可选）")
    private Double latitude;

    /**
     * 是否为快递员侧上报（配送环节）。
     *
     * <p>由各端Feign 调用时按端区分：快递员端传 true、司机端传 false。
     * 用于拦截"配送环节上报车辆故障"这类越界误报。</p>
     */
    private boolean courier;
}