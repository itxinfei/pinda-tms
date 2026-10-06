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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WebManagerServiceImpl implements WebManagerService {

    @Autowired
    private WebManagerMapper webManagerMapper;

    @Override
    public PageResponse<DriverJobDTO> findDriverJobByPage(DriverJobQueryDTO dto) {
        int page = (dto.getPage() == null || dto.getPage() < 1) ? 1 : dto.getPage();
        int pageSize = (dto.getPageSize() == null || dto.getPageSize() < 1) ? 10 : dto.getPageSize();
        IPage<DriverJobDTO> iPage = new Page<>(page, pageSize);
        webManagerMapper.findDriverJobByPage(iPage, dto);
        return PageResponse.<DriverJobDTO>builder()
                .counts(iPage.getTotal())
                .pages(iPage.getPages())
                .pagesize(pageSize)
                .page(page)
                .items(iPage.getRecords())
                .build();
    }

    @Override
    public PageResponse<TaskPickupDispatchDTO> findTaskPickupDispatchJobByPage(TaskPickupDispatchQueryDTO dto) {
        int page = (dto.getPage() == null || dto.getPage() < 1) ? 1 : dto.getPage();
        int pageSize = (dto.getPageSize() == null || dto.getPageSize() < 1) ? 10 : dto.getPageSize();
        IPage<TaskPickupDispatchDTO> iPage = new Page<>(page, pageSize);
        webManagerMapper.findTaskPickupDispatchJobByPage(iPage, dto);
        return PageResponse.<TaskPickupDispatchDTO>builder()
                .counts(iPage.getTotal())
                .pages(iPage.getPages())
                .pagesize(pageSize)
                .page(page)
                .items(iPage.getRecords())
                .build();
    }

    @Override
    public PageResponse<TransportOrderDTO> findTransportOrderByPage(TransportOrderQueryDTO dto) {
        int page = (dto.getPage() == null || dto.getPage() < 1) ? 1 : dto.getPage();
        int pageSize = (dto.getPageSize() == null || dto.getPageSize() < 1) ? 10 : dto.getPageSize();
        IPage<TransportOrderDTO> iPage = new Page<>(page, pageSize);
        webManagerMapper.findTransportOrderByPage(iPage, dto);
        return PageResponse.<TransportOrderDTO>builder()
                .counts(iPage.getTotal())
                .pages(iPage.getPages())
                .pagesize(pageSize)
                .page(page)
                .items(iPage.getRecords())
                .build();
    }

    @Override
    public PageResponse<TaskTransportDTO> findTaskTransportByPage(TaskTransportQueryDTO dto) {
        int page = (dto.getPage() == null || dto.getPage() < 1) ? 1 : dto.getPage();
        int pageSize = (dto.getPageSize() == null || dto.getPageSize() < 1) ? 10 : dto.getPageSize();
        IPage<TaskTransportDTO> iPage = new Page<>(page, pageSize);
        webManagerMapper.findTaskTransportByPage(iPage, dto);
        return PageResponse.<TaskTransportDTO>builder()
                .counts(iPage.getTotal())
                .pages(iPage.getPages())
                .pagesize(pageSize)
                .page(page)
                .items(iPage.getRecords())
                .build();
    }
}
