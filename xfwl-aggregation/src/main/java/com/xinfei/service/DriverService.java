package com.xinfei.service;

import com.xinfei.DTO.AppDriverQueryDTO;
import com.xinfei.DTO.DriverJobDTO;
import com.xinfei.common.utils.PageResponse;

public interface DriverService {
    PageResponse<DriverJobDTO> findByPage(AppDriverQueryDTO dto);
}
