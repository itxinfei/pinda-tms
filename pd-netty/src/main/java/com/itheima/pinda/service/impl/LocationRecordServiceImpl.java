package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.entity.LocationRecord;
import com.itheima.pinda.mapper.LocationRecordMapper;
import com.itheima.pinda.service.ILocationRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * GPS轨迹明细 Service 实现
 */
@Slf4j
@Service
public class LocationRecordServiceImpl extends ServiceImpl<LocationRecordMapper, LocationRecord> implements ILocationRecordService {

    /**
     * 清理过期轨迹数据（按 createTime 早于保留天数删除）
     *
     * @param retentionDays 保留天数（>0 才执行清理）
     * @return 删除的记录数
     */
    @Override
    public int cleanExpiredTraces(int retentionDays) {
        if (retentionDays <= 0) {
            log.info("[GPS清理] 保留天数配置非法或未开启，跳过清理: retentionDays={}", retentionDays);
            return 0;
        }
        LocalDateTime expireTime = LocalDateTime.now().minusDays(retentionDays);
        LambdaQueryWrapper<LocationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(LocationRecord::getCreateTime, expireTime);
        // 分批删除，避免一次性锁大量数据（每批 5000 条）
        int total = 0;
        int batchSize = 5000;
        while (true) {
            wrapper.last("limit " + batchSize);
            boolean removed = remove(wrapper);
            if (!removed) {
                break;
            }
            total += batchSize;
            // 检查是否还有剩余（若剩余不足 batchSize，remove 返回 false 但已删除部分）
            long remaining = count(wrapper);
            if (remaining == 0) {
                break;
            }
            if (remaining < batchSize) {
                // 最后一轮：直接删除全部剩余
                remove(wrapper);
                total += remaining;
                break;
            }
        }
        if (total > 0) {
            log.info("[GPS清理] 清理过期轨迹数据完成: 保留天数={}, 删除约{}条", retentionDays, total);
        } else {
            log.info("[GPS清理] 无过期轨迹数据需要清理: 保留天数={}", retentionDays);
        }
        return total;
    }
}
