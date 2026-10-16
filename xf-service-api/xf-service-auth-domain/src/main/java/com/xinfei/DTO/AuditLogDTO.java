package com.xinfei.DTO;

import lombok.Data;

import java.io.Serializable;

/**
 * 审计日志上报 DTO（SYS-06 跨服务埋点）：各服务写请求经 Feign 上报 xf-auth 落 sys_operation_log。
 * tenant_id 由 xf-auth 租户插件按上报请求头填充，不在此显式传递。
 */
@Data
public class AuditLogDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日志类型：OPER / LOGIN */
    private String logType;

    /** 模块标识（如 dispatch / manager） */
    private String module;

    /** 动作类型（新增/修改/删除/导出） */
    private String actionType;

    /** 日志标题（module·action） */
    private String title;

    private String requestUri;

    private String requestMethod;

    private String ip;

    /** 操作人ID（网关 userid 头） */
    private Long operatorId;

    /** 操作人账号（可空） */
    private String operatorName;

    /** 1 成功 / 0 失败 */
    private Integer status;

    private Integer executionTime;

    private String errorMsg;
}
