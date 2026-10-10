package com.itheima.pinda.service.state;

import com.baomidou.mybatisplus.spring.service.IService;
import com.itheima.pinda.entity.state.StatusTransitionHistory;

/**
 * 状态流转历史服务接口
 */
public interface IStatusTransitionHistoryService extends IService<StatusTransitionHistory> {

    /**
     * 记录状态流转历史
     *
     * @param businessType 业务类型：1运输任务
     * @param businessId   业务ID
     * @param businessNo   业务编号
     * @param beforeStatus 变更前状态
     * @param afterStatus  变更后状态
     * @param operatorId   操作人ID
     * @param operatorName 操作人名称
     * @param operatorType 操作人类型：1后台 2司机 3快递员
     * @param remark       操作备注
     * @return 是否记录成功
     */
    boolean recordTransition(Integer businessType, Long businessId, String businessNo,
                             Integer beforeStatus, Integer afterStatus,
                             Long operatorId, String operatorName, Integer operatorType, String remark);
}
