package com.xinfei.service;

import com.xinfei.DTO.AppCourierQueryDTO;
import com.xinfei.DTO.TaskPickupDispatchDTO;
import com.xinfei.common.utils.PageResponse;

public interface CourierService {
    PageResponse<TaskPickupDispatchDTO> findByPage(AppCourierQueryDTO dto);
}
