package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务日志（disp_schedule_job_log）
 *
 * <p>主键雪花 Long；tenant_id 由 TenantLineInnerInterceptor 自动填充。</p>
 */
@Data
@TableName("disp_schedule_job_log")
public class ScheduleJobLogEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 任务ID
     */
    private Long jobId;

    /**
     * 执行器 Bean 名
     */
    private String beanName;

    /**
     * 任务参数
     */
    private String params;

    /**
     * 任务状态 0：失败 1：成功
     */
    private Integer status;

    /**
     * 失败信息
     */
    private String error;

    /**
     * 耗时(毫秒)
     */
    private Integer times;

    /**
     * 创建时间
     */
    private Date createTime;
}
