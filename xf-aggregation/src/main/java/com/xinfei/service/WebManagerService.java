package com.xinfei.service;

import com.xinfei.DTO.DriverJobDTO;
import com.xinfei.DTO.TaskPickupDispatchDTO;
import com.xinfei.DTO.TaskTransportDTO;
import com.xinfei.DTO.TransportOrderDTO;
import com.xinfei.DTO.webManager.DriverJobQueryDTO;
import com.xinfei.DTO.webManager.TaskPickupDispatchQueryDTO;
import com.xinfei.DTO.webManager.TaskTransportQueryDTO;
import com.xinfei.DTO.webManager.TransportOrderQueryDTO;
import com.xinfei.common.utils.PageResponse;

public interface WebManagerService {
    /**
     * 获取司机作业单分页数据
     *
     * @param dto 查询参数
     * @return 司机作业单分页数据
     */
    PageResponse<DriverJobDTO> findDriverJobByPage(DriverJobQueryDTO dto);

    /**
     * 获取取派件任务分页数据
     *
     * @param dto 查询参数
     * @return 取派件分页数据
     */
    PageResponse<TaskPickupDispatchDTO> findTaskPickupDispatchJobByPage(TaskPickupDispatchQueryDTO dto);

    /**
     * 获取运单分页数据
     *
     * @param dto 查询参数
     * @return 运单分页数据
     */
    PageResponse<TransportOrderDTO> findTransportOrderByPage(TransportOrderQueryDTO dto);

    /**
     * 获取运输任务分页数据
     *
     * @param dto 查询参数
     * @return 运输任务分页数据
     */
    PageResponse<TaskTransportDTO> findTaskTransportByPage(TaskTransportQueryDTO dto);
}
