package com.itheima.pinda.service.state;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.common.CustomIdGenerator;
import com.itheima.pinda.entity.state.StatusTransitionHistory;
import com.itheima.pinda.mapper.state.StatusTransitionHistoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 状态流转历史服务实现类
 *
 * @author Claude Code
 * @since 2026-07-01
 */
@Slf4j
@Service
public class StatusTransitionHistoryServiceImpl extends ServiceImpl<StatusTransitionHistoryMapper, StatusTransitionHistory> implements IStatusTransitionHistoryService {

    @Autowired
    private CustomIdGenerator idGenerator;

    @Override
    public boolean recordTransition(Integer businessType, String businessId, String businessNo,
                                    Integer beforeStatus, Integer afterStatus,
                                    String operatorId, String operatorName, Integer operatorType, String remark) {
        try {
            StatusTransitionHistory history = new StatusTransitionHistory();
            // 修改点：原使用 System.nanoTime() 作主键，高并发下极易重复导致唯一键冲突、审计记录丢失；
            // 改用项目统一的雪花算法 CustomIdGenerator 生成全局唯一且趋势递增的 ID。
            history.setId(String.valueOf(idGenerator.nextId(history)));
            history.setBusinessType(businessType);
            history.setBusinessId(businessId);
            history.setBusinessNo(businessNo);
            history.setOperationType(1); // 1-状态变更
            history.setBeforeStatus(beforeStatus);
            history.setAfterStatus(afterStatus);
            history.setOperatorId(operatorId);
            history.setOperatorName(operatorName);
            history.setOperatorType(operatorType);
            history.setRemark(remark);
            history.setOperateTime(LocalDateTime.now());
            history.setCreateTime(LocalDateTime.now());

            save(history);
            log.info("记录状态流转历史成功：businessType={}, businessId={}, beforeStatus={}, afterStatus={}",
                businessType, businessId, beforeStatus, afterStatus);
            return true;
        } catch (Exception e) {
            log.error("记录状态流转历史失败：businessType=" + businessType + ", businessId=" + businessId, e);
            return false;
        }
    }
}
