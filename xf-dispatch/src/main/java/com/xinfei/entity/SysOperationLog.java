package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作/登录日志（跨服务审计埋点：dispatch 写请求直写同一张表，
 * 与 xf-auth 单表口径一致——log_type 判别 OPER/LOGIN，2110 迁移）。
 * 注意：不声明 tenantId 字段，交由租户插件按请求头填充（声明后插件跳过填充的坑见《12》§2）。
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    /** OPER 操作 / LOGIN 登录 */
    public static final String TYPE_OPER = "OPER";
    public static final String TYPE_LOGIN = "LOGIN";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String logType;

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

    private LocalDateTime createTime;
}
