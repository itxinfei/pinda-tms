package com.xinfei.task;

import com.xinfei.DTO.ScheduleJobDTO;
import com.xinfei.entity.ScheduleExceptionOrder;
import com.xinfei.service.IScheduleExceptionOrderService;
import com.xinfei.service.IScheduleJobService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 异常调度订单定时重试任务（2026-10-10 调度改造：由自研 @Scheduled 改为 XXL-Job 驱动）。
 *
 * <p>调度触发迁至 xxl-job-admin 控制台「心飞调度中心」分组的
 * 「调度异常单重试」任务（Cron 每 10 分钟，与原默认值一致）；
 * 本类只保留业务逻辑，执行开关/频率改由控制台管理（启停任务即开关）。</p>
 */
@Slf4j
@Component
public class ScheduleExceptionRetryTask {

    @Autowired
    private IScheduleExceptionOrderService scheduleExceptionOrderService;

    @Autowired
    private IScheduleJobService scheduleJobService;

    /**
     * 定时重试：Cron 由 xxl-job-admin 控制台任务配置驱动（默认每 10 分钟）
     *
     * <p>只对存在待处理异常订单的机构触发重新调度；机构未配置调度任务时跳过。
     * 调度成功后订单不再进入 ERROR 分组，异常登记由下次登记逻辑自然收敛。</p>
     */
    @XxlJob("scheduleExceptionRetryTask")
    public void retryPendingExceptions() {
        List<ScheduleExceptionOrder> pending = scheduleExceptionOrderService.listPending();
        if (pending.isEmpty()) {
            XxlJobHelper.handleSuccess("无待处理异常订单");
            return;
        }
        // 按机构去重，避免同一机构重复触发
        List<Long> agencyIds = pending.stream()
            .map(ScheduleExceptionOrder::getOrgId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        log.info("[异常调度重试] 待处理异常订单 {} 条，涉及机构 {} 个，开始自动重试", pending.size(), agencyIds.size());

        int ok = 0, skip = 0, fail = 0;
        for (Long agencyId : agencyIds) {
            try {
                ScheduleJobDTO scheduleJob = scheduleJobService.getByOrgId(agencyId);
                if (scheduleJob == null || scheduleJob.getId() == null) {
                    log.warn("[异常调度重试] 机构[{}]未配置调度任务，跳过重试", agencyId);
                    skip++;
                    continue;
                }
                scheduleJobService.run(new Long[]{scheduleJob.getId()});
                log.info("[异常调度重试] 机构[{}]重新调度已触发", agencyId);
                ok++;
            } catch (Exception e) {
                log.error("[异常调度重试] 机构[{}]重试触发失败", agencyId, e);
                fail++;
            }
        }
        XxlJobHelper.handleSuccess("异常订单 " + pending.size() + " 条，机构 " + agencyIds.size()
            + " 个：触发成功 " + ok + "，跳过 " + skip + "，失败 " + fail);
    }
}
