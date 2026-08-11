package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.entity.LocationRecord;
import com.itheima.pinda.mapper.LocationRecordMapper;
import com.itheima.pinda.service.ILocationRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * GPS轨迹明细 Service 实现
 */
@Slf4j
@Service
public class LocationRecordServiceImpl extends ServiceImpl<LocationRecordMapper, LocationRecord> implements ILocationRecordService {
}
