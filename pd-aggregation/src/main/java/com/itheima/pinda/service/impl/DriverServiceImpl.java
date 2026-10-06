package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.DTO.AppDriverQueryDTO;
import com.itheima.pinda.DTO.DriverJobDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.mapper.DriverMapper;
import com.itheima.pinda.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DriverServiceImpl implements DriverService {

    @Autowired
    private DriverMapper driverMapper;

    @Override
    public PageResponse<DriverJobDTO> findByPage(AppDriverQueryDTO dto) {
        int page = (dto.getPage() == null || dto.getPage() < 1) ? 1 : dto.getPage();
        int pageSize = (dto.getPageSize() == null || dto.getPageSize() < 1) ? 10 : dto.getPageSize();
        IPage<DriverJobDTO> iPage = new Page<>(page, pageSize);
        driverMapper.findByPage(iPage, dto);

        return PageResponse.<DriverJobDTO>builder()
                .counts(iPage.getTotal())
                .pages(iPage.getPages())
                .pagesize(pageSize)
                .page(page)
                .items(iPage.getRecords())
                .build();
    }
}
