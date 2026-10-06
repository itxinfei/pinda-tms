package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.DTO.AppCourierQueryDTO;
import com.itheima.pinda.DTO.TaskPickupDispatchDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.mapper.CourierMapper;
import com.itheima.pinda.service.CourierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CourierServiceImpl implements CourierService {

    @Autowired
    private CourierMapper courierMapper;

    @Override
    public PageResponse<TaskPickupDispatchDTO> findByPage(AppCourierQueryDTO dto) {
        // 兜底：调用方可能不传 page/pageSize（setPage(null) 会覆盖 DTO 字段默认值），
        // 直接传入 MyBatis-Plus 的 Page 会导致 LIMIT null,null 的 SQL 异常（500）。
        Integer page = dto.getPage() == null ? 1 : dto.getPage();
        Integer pageSize = dto.getPageSize() == null ? 10 : dto.getPageSize();
        IPage<TaskPickupDispatchDTO> iPage = new Page<>(page, pageSize);
        courierMapper.findByPage(iPage, dto);

        return PageResponse.<TaskPickupDispatchDTO>builder()
                .counts(iPage.getTotal())
                .pages(iPage.getPages())
                .pagesize(pageSize)
                .page(page)
                .items(iPage.getRecords())
                .build();
    }
}
