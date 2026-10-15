package com.xinfei.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志。写入由 AOP/拦截器落库（本期先建表与读接口）。
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
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
    private LocalDateTime createTime;
}
