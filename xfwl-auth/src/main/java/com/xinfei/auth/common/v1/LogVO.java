package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 操作日志视图（对齐前端 LogItem）。
 */
@Data
public class LogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String module;
    private String actionType;
    private String title;
    private String content;
    private Long operatorId;
    private String operatorName;
    private String requestUri;
    private String requestMethod;
    private String ip;
    private String region;
    private String device;
    private String browser;
    private String os;
    private Integer status;
    private Integer executionTime;
    private String errorMsg;
    private String createTime;
}
