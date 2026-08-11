package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.entity.LocationRecordArchive;
import com.itheima.pinda.mapper.LocationRecordArchiveMapper;
import com.itheima.pinda.service.ILocationRecordArchiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * GPS轨迹归档 Service 实现
 */
@Slf4j
@Service
public class LocationRecordArchiveServiceImpl extends ServiceImpl<LocationRecordArchiveMapper, LocationRecordArchive>
        implements ILocationRecordArchiveService {
}
