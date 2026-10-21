package com.xinfei.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.LocationRecord;

import java.util.List;

/**
 * GPS轨迹明细 Service
 */
public interface ILocationRecordService extends IService<LocationRecord> {

    /**
     * 批量查询每个业务对象的最新一条位置（供管理端在途监控）
     *
     * @param businessType 业务类型数字码（1车辆 2快递员，null 查全部）
     * @param licensePlate 车牌号模糊过滤（null 不过滤）
     * @return 每个业务对象最新一条轨迹记录
     */
    List<LocationRecord> selectLatestPerBusiness(Integer businessType, String licensePlate);

    /**
     * 清理过期轨迹数据（按 createTime 早于保留天数删除）
     *
     * @param retentionDays 保留天数（>0 才执行清理）
     * @return 删除的记录数
     */
    int cleanExpiredTraces(int retentionDays);
}
