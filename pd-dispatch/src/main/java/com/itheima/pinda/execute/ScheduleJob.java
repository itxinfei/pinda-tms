package com.itheima.pinda.execute;

import com.itheima.pinda.common.utils.SpringContextUtils;
import com.itheima.pinda.entity.ScheduleJobEntity;
import com.itheima.pinda.entity.ScheduleJobLogEntity;
import com.itheima.pinda.service.IScheduleJobLogService;
import com.itheima.pinda.utils.ExceptionUtils;
import com.itheima.pinda.utils.IdUtils;
import com.itheima.pinda.utils.ScheduleUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * 定时任务类，进行智能调度操作
 */
public class ScheduleJob extends QuartzJobBean {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        ScheduleJobEntity scheduleJob =
                (ScheduleJobEntity) context.getMergedJobDataMap().
                        get(ScheduleUtils.JOB_PARAM_KEY);
        logger.info("{}定时任务开始执行...,定时任务id：{}", new Date(), scheduleJob.getId());

        //记录定时任务相关的日志信息
        //封装日志对象
        ScheduleJobLogEntity logEntity = new ScheduleJobLogEntity();
        logEntity.setId(IdUtils.get());
        logEntity.setJobId(scheduleJob.getId());
        logEntity.setBeanName(scheduleJob.getBeanName());
        logEntity.setParams(scheduleJob.getParams());
        logEntity.setCreateDate(new Date());

        long startTime = System.currentTimeMillis();

        // 失败自动重试次数（总尝试次数 = 重试次数 + 1）
        final int MAX_RETRY = 2;
        // 重试间隔（毫秒）
        final long RETRY_INTERVAL_MS = 2000L;

        Exception lastException = null;
        boolean success = false;
        int attempt = 0;
        while (attempt <= MAX_RETRY) {
            try {
                //通过反射调用目标对象，在目标对象中封装智能调度核心逻辑
                logger.info("定时任务准备执行（第{}次尝试），任务id为：{}", attempt + 1, scheduleJob.getId());

                //获得目标对象
                Object target = SpringContextUtils.getBean(scheduleJob.getBeanName());
                //获得目标方法对象
                Method method = target.getClass().getDeclaredMethod("run", String.class, String.class, String.class, String.class);

                //通过反射调用目标对象的方法
                method.invoke(target, scheduleJob.getBusinessId(), scheduleJob.getParams(), scheduleJob.getId(), logEntity.getId());

                logEntity.setStatus(1);//成功
                success = true;
                break;
            } catch (Exception ex) {
                lastException = ex;
                logger.error("定时任务执行失败（第{}次尝试），任务id为：{}，将{}重试",
                    attempt + 1, scheduleJob.getId(), attempt < MAX_RETRY ? "进行" : "不再");
                attempt++;
                if (attempt <= MAX_RETRY) {
                    try {
                        Thread.sleep(RETRY_INTERVAL_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (!success) {
            logEntity.setStatus(0);//失败
            // 记录完整重试信息，便于排查
            logEntity.setError("重试" + MAX_RETRY + "次后仍失败。最后一次异常：" + ExceptionUtils.getErrorStackTrace(lastException));
            logger.error("定时任务执行失败（已重试{}次），任务id为：{}", MAX_RETRY, scheduleJob.getId());
        } else if (attempt > 0) {
            logger.info("定时任务第{}次尝试执行成功，任务id为：{}", attempt + 1, scheduleJob.getId());
        }

        // 无论成功失败，统一记录执行耗时与日志
        int times = (int) (System.currentTimeMillis() - startTime);
        logEntity.setTimes(times);//耗时

        IScheduleJobLogService scheduleJobLogService = SpringContextUtils.getBean(IScheduleJobLogService.class);
        scheduleJobLogService.save(logEntity);
    }
}
