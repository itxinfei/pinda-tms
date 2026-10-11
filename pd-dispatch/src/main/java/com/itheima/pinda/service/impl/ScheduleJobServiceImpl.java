/**
 * Copyright (c) 2019 联智合创 All rights reserved.
 * <p>
 * http://www.witlinked.com
 * <p>
 * 版权所有，侵权必究！
 */

package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.DTO.OrgJobTreeDTO;
import com.itheima.pinda.DTO.OrgTreeDTO;
import com.itheima.pinda.DTO.ScheduleJobDTO;
import com.itheima.pinda.common.context.TenantContextHolder;
import com.itheima.pinda.config.ScheduleRetryProperties;
import com.itheima.pinda.entity.ScheduleJobEntity;
import com.itheima.pinda.entity.ScheduleJobLogEntity;
import com.itheima.pinda.enums.ScheduleStatus;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.mapper.ScheduleJobMapper;
import com.itheima.pinda.service.IScheduleJobLogService;
import com.itheima.pinda.service.IScheduleJobService;
import com.itheima.pinda.task.DispatchTask;
import com.itheima.pinda.tenant.OrgTenantResolver;
import com.itheima.pinda.utils.ConvertUtils;
import com.itheima.pinda.utils.ExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 调度任务服务（2026-10-10 调度改造）：
 * 定时触发已统一迁至 XXL-Job（xxl-job-admin 控制台管理 Cron/启停，见《56-中间件信息总表》），
 * 本服务只负责调度任务配置的 CRUD 与「立即执行」（run 直调 DispatchTask，保留
 * 系统态租户绑定、业务级失败重试与执行日志写入，行为与原 Quartz 链路等价）。
 */
@Slf4j
@Service
public class ScheduleJobServiceImpl extends ServiceImpl<ScheduleJobMapper, ScheduleJobEntity> implements IScheduleJobService {
    @Autowired
    private OrgFeign orgFeign;
    @Autowired
    private DispatchTask dispatchTask;
    @Autowired
    private OrgTenantResolver orgTenantResolver;
    @Autowired
    private IScheduleJobLogService scheduleJobLogService;
    @Autowired
    private ScheduleRetryProperties retryProps;

    @Override
    public List<OrgJobTreeDTO> page(Map<String, Object> params) {
        List<OrgTreeDTO> orgTreeDTOS = orgFeign.tree("", true);
        if (orgTreeDTOS == null) {
            orgTreeDTOS = new ArrayList<>();
        }
        List<ScheduleJobEntity> schedulerEntity = baseMapper.selectList(null);
        Map<Long, ScheduleJobEntity> schedulerMap = schedulerEntity.stream().collect(Collectors.toMap(ScheduleJobEntity::getBusinessId, item -> item));

        List<OrgJobTreeDTO> orgJobTreeDTOS = new ArrayList<>();
        mergeOrgJob(orgJobTreeDTOS, orgTreeDTOS, schedulerMap);

        return orgJobTreeDTOS;
    }

    private void mergeOrgJob(List<OrgJobTreeDTO> orgJobTreeDTOS, List<OrgTreeDTO> orgTreeDTOS, Map<Long, ScheduleJobEntity> schedulerMap) {
        orgTreeDTOS.forEach(item -> {
            OrgJobTreeDTO orgJobTreeDTO = new OrgJobTreeDTO();
            BeanUtils.copyProperties(item, orgJobTreeDTO);
            Long orgId = orgJobTreeDTO.getId();
            ScheduleJobEntity schedulerDto = schedulerMap.get(orgId);
            if (schedulerDto != null) {
                orgJobTreeDTO.setCronExpression(schedulerDto.getCronExpression());
                orgJobTreeDTO.setJobId(schedulerDto.getId());
                orgJobTreeDTO.setParams(schedulerDto.getParams());
                orgJobTreeDTO.setRemark(schedulerDto.getRemark());
                orgJobTreeDTO.setJobStatus(schedulerDto.getStatus());
            }
            orgJobTreeDTOS.add(orgJobTreeDTO);
        });
    }


    @Override
    public ScheduleJobDTO get(Long id) {
        ScheduleJobEntity entity = baseMapper.selectById(id);
        return ConvertUtils.sourceToTarget(entity, ScheduleJobDTO.class);
    }

    @Override
    public ScheduleJobDTO getByOrgId(Long orgId) {
        LambdaQueryWrapper<ScheduleJobEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleJobEntity::getBusinessId, orgId);
        ScheduleJobEntity entity = baseMapper.selectOne(wrapper);
        return ConvertUtils.sourceToTarget(entity, ScheduleJobDTO.class);
    }


    private QueryWrapper<ScheduleJobEntity> getWrapper(Map<String, Object> params) {
        String beanName = (String) params.get("beanName");

        QueryWrapper<ScheduleJobEntity> wrapper = new QueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(beanName), "bean_name", beanName);

        return wrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(ScheduleJobDTO dto) {
        ScheduleJobEntity entity = ConvertUtils.sourceToTarget(dto, ScheduleJobEntity.class);

        entity.setStatus(ScheduleStatus.NORMAL.getValue());
        baseMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ScheduleJobDTO dto) {
        ScheduleJobEntity entity = this.getById(dto.getId());

        BeanUtils.copyProperties(dto, entity, ConvertUtils.getNullPropertyNames(dto));

        this.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(Long[] ids) {
        //删除数据（原 Quartz 触发器清理已随调度面下线移除）
        baseMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public int updateBatch(Long[] ids, int status) {
        ScheduleJobEntity scheduleJobEntity = new ScheduleJobEntity();
        scheduleJobEntity.setStatus(status);
        LambdaQueryWrapper<ScheduleJobEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ScheduleJobEntity::getId, Arrays.asList(ids));
        return baseMapper.update(scheduleJobEntity, wrapper);
    }

    @Override
    // 显式不加 @Transactional：循环内每个机构各自执行 + 业务级失败重试 + 独立写执行日志，
    // 且 dispatchTask.run 内层（如 TaskOrderClassifyServiceImpl#execute）自带 @Transactional。
    // 若此处包外层事务，内层失败会把共享事务标记为 rollback-only，被 catch 重试后最终回写日志
    // 反抛 UnexpectedRollbackException，导致失败日志落不了库。与 XXL 入口 runByOrgId() 保持同一无外层事务语义。
    public void run(Long[] ids) {
        for (Long id : ids) {
            ScheduleJobEntity scheduleJob = baseMapper.selectById(id);
            if (scheduleJob == null) {
                log.warn("定时任务不存在: " + id);
                continue;
            }
            executeWithRetry(scheduleJob);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pause(Long[] ids) {
        // 定时触发已由 XXL-Job 管理，pause/resume 仅保留配置状态语义
        updateBatch(ids, ScheduleStatus.PAUSE.getValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resume(Long[] ids) {
        updateBatch(ids, ScheduleStatus.NORMAL.getValue());
    }

    @Override
    public void runByOrgId(Long orgId) {
        LambdaQueryWrapper<ScheduleJobEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleJobEntity::getBusinessId, orgId);
        ScheduleJobEntity scheduleJob = baseMapper.selectOne(wrapper);
        if (scheduleJob == null) {
            log.warn("机构[{}]未配置调度任务，跳过执行", orgId);
            return;
        }
        executeWithRetry(scheduleJob);
    }

    /**
     * 立即执行单条调度任务（承接原 Quartz 链路：租户绑定 → 业务重试 → 执行日志）。
     *
     * <p>XXL-Job 驱动的「调度异常单重试」与手动 run 共用本方法，
     * 保证系统态租户绑定（Quartz/XXL 线程无登录态）与日志口径一致。</p>
     */
    private void executeWithRetry(ScheduleJobEntity scheduleJob) {
        // 系统态租户绑定：调度线程无登录态与请求头，按机构ID反查租户并写入当前线程上下文，
        // 供出站 Feign 拦截器读取透传给 pd-auth；执行结束在 finally 中清理，避免线程复用串租户。
        Long tenantId = 0L;
        Long businessId = scheduleJob.getBusinessId();
        if (businessId != null) {
            try {
                tenantId = orgTenantResolver.resolve(businessId);
            } catch (Exception e) {
                log.warn("调度任务解析租户失败，回落租户0，任务id：{}", scheduleJob.getId(), e);
            }
        }
        TenantContextHolder.setTenantId(tenantId);

        try {
            // 先落库执行日志，由 MyBatis-Plus 回填雪花ID，
            // dispatchTask.run 需把 logId 写入订单分类记录（disp_order_classify.job_log_id），故必须先保存再执行
            ScheduleJobLogEntity logEntity = new ScheduleJobLogEntity();
            logEntity.setJobId(scheduleJob.getId());
            logEntity.setBeanName(scheduleJob.getBeanName());
            logEntity.setParams(scheduleJob.getParams());
            logEntity.setCreateTime(new Date());
            logEntity.setStatus(0);//执行中先置失败态，成功后更新
            scheduleJobLogService.save(logEntity);

            long startTime = System.currentTimeMillis();
            int maxAttempts = Math.max(retryProps.getMaxAttempts(), 1);
            long retryIntervalMs = Math.max(retryProps.getIntervalMs(), 0);

            Exception lastException = null;
            boolean success = false;
            int attempt = 0;
            while (attempt < maxAttempts) {
                try {
                    log.info("调度任务开始执行（第{}次尝试，共{}次），任务id：{}", attempt + 1, maxAttempts, scheduleJob.getId());
                    dispatchTask.run(scheduleJob.getBusinessId(), scheduleJob.getParams(), scheduleJob.getId(), logEntity.getId());
                    success = true;
                    break;
                } catch (Exception ex) {
                    lastException = ex;
                    log.error("调度任务执行失败（第{}次尝试），任务id：{}，{}", attempt + 1, scheduleJob.getId(),
                        attempt + 1 < maxAttempts ? "将进行重试" : "不再重试");
                    attempt++;
                    if (attempt < maxAttempts) {
                        try {
                            Thread.sleep(retryIntervalMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }

            // 回写执行结果（状态/耗时/错误信息）
            ScheduleJobLogEntity result = new ScheduleJobLogEntity();
            result.setId(logEntity.getId());
            result.setStatus(success ? 1 : 0);//成功/失败
            if (!success) {
                result.setError("重试" + (maxAttempts - 1) + "次后仍失败。最后一次异常：" + ExceptionUtils.getErrorStackTrace(lastException));
            }
            result.setTimes((int) (System.currentTimeMillis() - startTime));
            scheduleJobLogService.updateById(result);
        } finally {
            TenantContextHolder.clear();
        }
    }

}
