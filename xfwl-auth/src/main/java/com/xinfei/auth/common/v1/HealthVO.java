package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 服务健康视图（服务健康页）。
 */
@Data
public class HealthVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Nacos 服务名（如 xfwl-auth） */
    private String serviceName;

    /** 健康 UP / DOWN（探测超时或异常视为 DOWN） */
    private String status;

    /** 实例数 */
    private Integer instances;

    /** 实例地址列表 ip:port */
    private String instanceList;

    /** 探测耗时 ms */
    private Integer latency;
}
