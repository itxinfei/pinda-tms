package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.DTO.AppCourierQueryDTO;
import com.itheima.pinda.DTO.TaskPickupDispatchDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.mapper.CourierMapper;
import com.itheima.pinda.service.CourierService;
import com.itheima.pinda.support.PageResponses;
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
