package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务（disp_schedule_job）
 *
 * <p>主键雪花 Long；tenant_id 由 TenantLineInnerInterceptor 自动填充，实体不声明。</p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("disp_schedule_job")
public class ScheduleJobEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属组织ID（sys_org.id）
     */
    private Long businessId;

    /**
     * 执行器 Bean 名
     */
    private String beanName;

    /**
     * 任务参数
     */
    private String params;

    /**
     * cron 表达式
     */
    private String cronExpression;

    /**
     * 任务状态 0：暂停 1：正常
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新人
     */
    private Long updateBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除 0：未删 1：已删
     */
    @TableLogic
    private Integer deleted;
}
