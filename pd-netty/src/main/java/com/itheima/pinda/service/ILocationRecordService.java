package com.itheima.pinda.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.pinda.entity.LocationRecord;

/**
 * GPS轨迹明细 Service
 */
public interface ILocationRecordService extends IService<LocationRecord> {

    /**
     * 清理过期轨迹数据（按 createTime 早于保留天数删除）
     *
     * @param retentionDays 保留天数（>0 才执行清理）
     * @return 删除的记录数
     */
    int cleanExpiredTraces(int retentionDays);
}
