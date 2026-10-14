package com.xinfei.DTO;

import com.xinfei.DTO.OrgTreeDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrgJobTreeDTO extends OrgTreeDTO {

    /**
     * 定时任务ID（disp_schedule_job.id）
     */
    private Long jobId;
    /**
     * 参数
     */
    private String params;
    /**
     * cron表达式
     */
    private String cronExpression;
    /**
     * 任务状态  0：暂停  1：正常
     */
    private Integer jobStatus;
    /**
     * 备注
     */
    private String remark;
}
