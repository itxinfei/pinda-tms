package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.entity.LocationRecord;
import com.itheima.pinda.entity.LocationRecordArchive;
import com.itheima.pinda.mapper.LocationRecordMapper;
import com.itheima.pinda.service.ILocationRecordArchiveService;
import com.itheima.pinda.service.ILocationRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GPS轨迹明细 Service 实现
 */
@Slf4j
@Service
public class LocationRecordServiceImpl extends ServiceImpl<LocationRecordMapper, LocationRecord> implements ILocationRecordService {

    @Autowired
    private ILocationRecordArchiveService locationRecordArchiveService;

    /**
     * 清理过期轨迹数据（按 createTime 早于保留天数，先归档再删除）
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
        // 分批"查询→归档→删除"，避免一次性锁大量数据（每批 5000 条）
        int total = 0;
        int batchSize = 5000;
        while (true) {
            wrapper.last("limit " + batchSize);
            List<LocationRecord> batch = list(wrapper);
            if (CollectionUtils.isEmpty(batch)) {
                break;
            }
            // 1. 归档到历史表
            archiveBatch(batch);
            // 2. 删除主表数据
            removeByIds(batch.stream().map(LocationRecord::getId).collect(Collectors.toList()));
            total += batch.size();
            if (batch.size() < batchSize) {
                break;
            }
        }
        if (total > 0) {
            log.info("[GPS清理] 清理过期轨迹数据完成: 保留天数={}, 归档并删除{}条", retentionDays, total);
        } else {
            log.info("[GPS清理] 无过期轨迹数据需要清理: 保留天数={}", retentionDays);
        }
        return total;
    }

    /**
     * 批量归档轨迹记录到历史表
     *
     * @param batch 待归档的记录
     */
    private void archiveBatch(List<LocationRecord> batch) {
        LocalDateTime archiveTime = LocalDateTime.now();
        List<LocationRecordArchive> archiveList = new ArrayList<>(batch.size());
        for (LocationRecord record : batch) {
            LocationRecordArchive archive = new LocationRecordArchive();
            archive.setId(record.getId());
            archive.setBusinessId(record.getBusinessId());
            archive.setName(record.getName());
            archive.setPhone(record.getPhone());
            archive.setLicensePlate(record.getLicensePlate());
            archive.setType(record.getType());
            archive.setLng(record.getLng());
            archive.setLat(record.getLat());
            archive.setCurrentTime(record.getCurrentTime());
            archive.setTeam(record.getTeam());
            archive.setTransportTaskId(record.getTransportTaskId());
            archive.setCreateTime(record.getCreateTime());
            archive.setArchiveTime(archiveTime);
            archiveList.add(archive);
        }
        try {
            locationRecordArchiveService.saveBatch(archiveList);
            log.info("[GPS归档] 归档 {} 条历史轨迹", archiveList.size());
        } catch (Exception e) {
            // 归档失败不阻塞主流程，仅告警（后续清理轮次会再次尝试）
            log.error("[GPS归档] 批量归档失败，{} 条轨迹暂缓归档", archiveList.size(), e);
            throw e;
        }
    }
}
