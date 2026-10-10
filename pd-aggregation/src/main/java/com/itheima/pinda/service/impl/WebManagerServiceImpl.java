package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.DTO.DriverJobDTO;
import com.itheima.pinda.DTO.TaskPickupDispatchDTO;
import com.itheima.pinda.DTO.TaskTransportDTO;
import com.itheima.pinda.DTO.TransportOrderDTO;
import com.itheima.pinda.DTO.webManager.DriverJobQueryDTO;
import com.itheima.pinda.DTO.webManager.TaskPickupDispatchQueryDTO;
import com.itheima.pinda.DTO.webManager.TaskTransportQueryDTO;
import com.itheima.pinda.DTO.webManager.TransportOrderQueryDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.mapper.WebManagerMapper;
import com.itheima.pinda.service.WebManagerService;
import com.itheima.pinda.support.PageResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WebManagerServiceImpl implements WebManagerService {

    @Autowired
    private WebManagerMapper webManagerMapper;

    @Override
    public PageResponse<DriverJobDTO> findDriverJobByPage(DriverJobQueryDTO dto) {
        int page = PageResponses.page(dto.getPage());
        int pageSize = PageResponses.pageSize(dto.getPageSize());
        IPage<DriverJobDTO> iPage = new Page<>(page, pageSize);
        webManagerMapper.findDriverJobByPage(iPage, dto);
        return PageResponses.of(iPage, page, pageSize);
    }

    @Override
    public PageResponse<TaskPickupDispatchDTO> findTaskPickupDispatchJobByPage(TaskPickupDispatchQueryDTO dto) {
        int page = PageResponses.page(dto.getPage());
        int pageSize = PageResponses.pageSize(dto.getPageSize());
        IPage<TaskPickupDispatchDTO> iPage = new Page<>(page, pageSize);
        webManagerMapper.findTaskPickupDispatchJobByPage(iPage, dto);
        return PageResponses.of(iPage, page, pageSize);
    }

    @Override
    public PageResponse<TransportOrderDTO> findTransportOrderByPage(TransportOrderQueryDTO dto) {
        int page = PageResponses.page(dto.getPage());
        int pageSize = PageResponses.pageSize(dto.getPageSize());
        IPage<TransportOrderDTO> iPage = new Page<>(page, pageSize);
        webManagerMapper.findTransportOrderByPage(iPage, dto);
        return PageResponses.of(iPage, page, pageSize);
    }

    @Override
    public PageResponse<TaskTransportDTO> findTaskTransportByPage(TaskTransportQueryDTO dto) {
        int page = PageResponses.page(dto.getPage());
        int pageSize = PageResponses.pageSize(dto.getPageSize());
        IPage<TaskTransportDTO> iPage = new Page<>(page, pageSize);
        webManagerMapper.findTaskTransportByPage(iPage, dto);
        return PageResponses.of(iPage, page, pageSize);
    }
}
