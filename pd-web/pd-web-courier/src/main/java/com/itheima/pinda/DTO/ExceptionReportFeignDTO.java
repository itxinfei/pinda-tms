package com.itheima.pinda.DTO;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 异常上报入参（快递员端 · D-41）
 *
 * <p>本类仅为 Feign 传输载体，与 pd-netty 侧的
 * {@code com.itheima.pinda.DTO.ExceptionReportDTO} 字段一一对应。
 * <b>不复用 pd-netty 的类</b>：web端不应依赖内部服务模块的实体类
 * （与现有 {@code TraceFeign} 用通用 Map 回参的做法一致）。</p>
 *
 * <p>定案 <b>D-46「类型 + 照片 + 备注」三要素</b>，三者缺一不可。</p>
 */
@Data
public class ExceptionReportFeignDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 异常类型：GOODS_DAMAGED 破损 / REJECTED 拒收 / ADDRESS_ERROR 地址错误 */
    private String exceptionType;

    /** 上报人（快递员）ID，由服务端从 token 取，不接受前端传他人 ID */
    private String reporterId;

    /** 关联运输任务ID（可选） */
    private String transportTaskId;

    /** 照片凭证ID列表，**必填不可为空**（D-46） */
    private List<String> attachmentIds;

    /** 异常备注，**必填不可为空**（D-46） */
    private String remark;

    /** 上报经度（BD09，可选） */
    private Double longitude;

    /** 上报纬度（BD09，可选） */
    private Double latitude;

    /** 快递员侧上报固定为 true，用于服务端做环节越界校验 */
    private boolean courier;
}