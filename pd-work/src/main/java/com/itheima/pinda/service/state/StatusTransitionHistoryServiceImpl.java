package com.itheima.pinda.service.state;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.entity.state.StatusTransitionHistory;
import com.itheima.pinda.mapper.state.StatusTransitionHistoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 状态流转历史服务实现类
 */
@Slf4j
@Service
public class StatusTransitionHistoryServiceImpl
        extends ServiceImpl<StatusTransitionHistoryMapper, StatusTransitionHistory>
        implements IStatusTransitionHistoryService {

    @Override
    public boolean recordTransition(Integer businessType, Long businessId, String businessNo,
                                    Integer beforeStatus, Integer afterStatus,
                                    Long operatorId, String operatorName, Integer operatorType,
                                    String remark) {
        try {
            StatusTransitionHistory history = new StatusTransitionHistory();
            // Long 雪花主键由 @TableId(ASSIGN_ID) 自动生成，全局唯一且趋势递增
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
