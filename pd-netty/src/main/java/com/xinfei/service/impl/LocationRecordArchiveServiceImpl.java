package com.xinfei.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.LocationRecordArchive;
import com.xinfei.mapper.LocationRecordArchiveMapper;
import com.xinfei.service.ILocationRecordArchiveService;
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
