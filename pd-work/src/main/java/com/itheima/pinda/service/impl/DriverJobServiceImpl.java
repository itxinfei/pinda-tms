package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.entity.DriverJob;
import com.itheima.pinda.enums.driverjob.DriverJobStatus;
import com.itheima.pinda.mapper.DriverJobMapper;
import com.itheima.pinda.service.IDriverJobService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DriverJobServiceImpl extends
        ServiceImpl<DriverJobMapper, DriverJob> implements IDriverJobService {

    @Override
    public DriverJob saveDriverJob(DriverJob driverJob) {
        // Long 雪花主键由 @TableId(ASSIGN_ID) 自动生成，无需手工 setId
        driverJob.setCreateTime(LocalDateTime.now());
        driverJob.setStatus(DriverJobStatus.PENDING.getCode());
        save(driverJob);
        return driverJob;
    }

    @Override
    public IPage<DriverJob> findByPage(Integer page, Integer pageSize, Long id, Long driverId,
                                       Integer status, Long taskTransportId) {
        Page<DriverJob> iPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<DriverJob> wrapper = new LambdaQueryWrapper<>();
        if (id != null) {
            wrapper.eq(DriverJob::getId, id);
        }
        if (driverId != null) {
            wrapper.eq(DriverJob::getDriverId, driverId);
        }
        if (status != null) {
            wrapper.eq(DriverJob::getStatus, status);
        }
        if (taskTransportId != null) {
            wrapper.eq(DriverJob::getTaskTransportId, taskTransportId);
        }
        wrapper.orderByAsc(DriverJob::getCreateTime);
        return page(iPage, wrapper);
    }

    @Override
    public List<DriverJob> findAll(List<Long> ids, Long id, Long driverId, Integer status,
                                   Long taskTransportId) {
        LambdaQueryWrapper<DriverJob> wrapper = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(DriverJob::getId, ids);
        }
        if (id != null) {
            wrapper.eq(DriverJob::getId, id);
        }
        if (driverId != null) {
            wrapper.eq(DriverJob::getDriverId, driverId);
        }
        if (status != null) {
            wrapper.eq(DriverJob::getStatus, status);
        }
        if (taskTransportId != null) {
            wrapper.eq(DriverJob::getTaskTransportId, taskTransportId);
        }
        wrapper.orderByDesc(DriverJob::getCreateTime);
        return list(wrapper);
    }
}
