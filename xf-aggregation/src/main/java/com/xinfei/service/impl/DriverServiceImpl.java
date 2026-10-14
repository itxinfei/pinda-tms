package com.xinfei.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.DTO.AppDriverQueryDTO;
import com.xinfei.DTO.DriverJobDTO;
import com.xinfei.common.utils.PageResponse;
import com.xinfei.mapper.DriverMapper;
import com.xinfei.service.DriverService;
import com.xinfei.support.PageResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DriverServiceImpl implements DriverService {

    @Autowired
    private DriverMapper driverMapper;

    @Override
    public PageResponse<DriverJobDTO> findByPage(AppDriverQueryDTO dto) {
        int page = PageResponses.page(dto.getPage());
        int pageSize = PageResponses.pageSize(dto.getPageSize());
        IPage<DriverJobDTO> iPage = new Page<>(page, pageSize);
        driverMapper.findByPage(iPage, dto);
        return PageResponses.of(iPage, page, pageSize);
    }
}
