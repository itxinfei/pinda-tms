package com.xinfei.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作/登录日志。写入由登录埋点与写请求拦截器落库（2110 迁移单表 + log_type 判别口径）。
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    /** 日志类型：OPER 操作 / LOGIN 登录（《62》§4.3 口径，2110 迁移定案） */
    public static final String TYPE_OPER = "OPER";
    public static final String TYPE_LOGIN = "LOGIN";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** OPER 操作 / LOGIN 登录（缺省 OPER 由表默认值兜底，但登录写入显式赋值） */
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
