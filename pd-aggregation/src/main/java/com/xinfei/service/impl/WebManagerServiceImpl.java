package com.xinfei.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.DTO.DriverJobDTO;
import com.xinfei.DTO.TaskPickupDispatchDTO;
import com.xinfei.DTO.TaskTransportDTO;
import com.xinfei.DTO.TransportOrderDTO;
import com.xinfei.DTO.webManager.DriverJobQueryDTO;
import com.xinfei.DTO.webManager.TaskPickupDispatchQueryDTO;
import com.xinfei.DTO.webManager.TaskTransportQueryDTO;
import com.xinfei.DTO.webManager.TransportOrderQueryDTO;
import com.xinfei.common.utils.PageResponse;
import com.xinfei.mapper.WebManagerMapper;
import com.xinfei.service.WebManagerService;
import com.xinfei.support.PageResponses;
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
