package com.itheima.pinda.task;

import com.itheima.pinda.DTO.ScheduleJobDTO;
import com.itheima.pinda.entity.ScheduleExceptionOrder;
import com.itheima.pinda.service.IScheduleExceptionOrderService;
import com.itheima.pinda.service.IScheduleJobService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 异常调度订单定时重试任务
 *
 * <p>定期扫描待处理的异常调度订单，按机构分组触发重新调度，
 * 避免运营人员漏处理导致订单长期滞留。可通过配置控制开关。</p>
 */
@Slf4j
@Component
public class ScheduleExceptionRetryTask {

    /**
     * 重试开关（默认开启；可通过配置 schedule.retry.enabled 关闭）
     */
    @org.springframework.beans.factory.annotation.Value("${schedule.exception.retry.enabled:true}")
    private boolean retryEnabled;

    @Autowired
    private IScheduleExceptionOrderService scheduleExceptionOrderService;

    @Autowired
    private IScheduleJobService scheduleJobService;

    /**
     * 定时重试：每 10 分钟执行一次
     *
     * <p>只对存在待处理异常订单的机构触发重新调度；机构未配置调度任务时跳过。
     * 调度成功后订单不再进入 ERROR 分组，异常登记由下次登记逻辑自然收敛。</p>
     */
    @Scheduled(cron = "${schedule.exception.retry.cron:0 */10 * * * ?}")
    public void retryPendingExceptions() {
        if (!retryEnabled) {
            log.debug("[异常调度重试] 定时重试已关闭，跳过执行");
            return;
        }
        try {
            List<ScheduleExceptionOrder> pending = scheduleExceptionOrderService.listPending();
            if (pending.isEmpty()) {
                return;
            }
            // 按机构去重，避免同一机构重复触发
            List<String> agencyIds = pending.stream()
                .map(ScheduleExceptionOrder::getAgencyId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
            log.info("[异常调度重试] 待处理异常订单 {} 条，涉及机构 {} 个，开始自动重试", pending.size(), agencyIds.size());

            for (String agencyId : agencyIds) {
                try {
                    ScheduleJobDTO scheduleJob = scheduleJobService.getByOrgId(agencyId);
                    if (scheduleJob == null || StringUtils.isBlank(scheduleJob.getId())) {
                        log.warn("[异常调度重试] 机构[{}]未配置调度任务，跳过重试", agencyId);
                        continue;
                    }
                    scheduleJobService.run(new String[]{scheduleJob.getId()});
                    log.info("[异常调度重试] 机构[{}]重新调度已触发", agencyId);
                } catch (Exception e) {
                    log.error("[异常调度重试] 机构[{}]重试触发失败", agencyId, e);
                }
            }
        } catch (Exception e) {
            log.error("[异常调度重试] 定时重试执行异常", e);
        }
    }
}
