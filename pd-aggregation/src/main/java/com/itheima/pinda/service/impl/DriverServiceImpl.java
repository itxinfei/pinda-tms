package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.DTO.AppDriverQueryDTO;
import com.itheima.pinda.DTO.DriverJobDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.mapper.DriverMapper;
import com.itheima.pinda.service.DriverService;
import com.itheima.pinda.support.PageResponses;
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
