package com.xinfei.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.DTO.AppCourierQueryDTO;
import com.xinfei.DTO.TaskPickupDispatchDTO;
import com.xinfei.common.utils.PageResponse;
import com.xinfei.mapper.CourierMapper;
import com.xinfei.service.CourierService;
import com.xinfei.support.PageResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CourierServiceImpl implements CourierService {

    @Autowired
    private CourierMapper courierMapper;

    @Override
    public PageResponse<TaskPickupDispatchDTO> findByPage(AppCourierQueryDTO dto) {
        int page = PageResponses.page(dto.getPage());
        int pageSize = PageResponses.pageSize(dto.getPageSize());
        IPage<TaskPickupDispatchDTO> iPage = new Page<>(page, pageSize);
        courierMapper.findByPage(iPage, dto);
        return PageResponses.of(iPage, page, pageSize);
    }
}
