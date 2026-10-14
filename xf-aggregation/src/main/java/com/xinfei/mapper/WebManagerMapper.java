package com.xinfei.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xinfei.DTO.*;
import com.xinfei.DTO.webManager.DriverJobQueryDTO;
import com.xinfei.DTO.webManager.TaskPickupDispatchQueryDTO;
import com.xinfei.DTO.webManager.TaskTransportQueryDTO;
import com.xinfei.DTO.webManager.TransportOrderQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

@Component
@Mapper
public interface WebManagerMapper extends BaseMapper {
    IPage<DriverJobDTO> findDriverJobByPage(IPage<DriverJobDTO> iPage, @Param("params") DriverJobQueryDTO dto);

    IPage<TaskPickupDispatchDTO> findTaskPickupDispatchJobByPage(IPage<TaskPickupDispatchDTO> iPage, @Param("params") TaskPickupDispatchQueryDTO dto);

    IPage<TransportOrderDTO> findTransportOrderByPage(IPage<TransportOrderDTO> iPage, @Param("params") TransportOrderQueryDTO dto);

    IPage<TaskTransportDTO> findTaskTransportByPage(IPage<TaskTransportDTO> iPage, @Param("params") TaskTransportQueryDTO dto);
}
