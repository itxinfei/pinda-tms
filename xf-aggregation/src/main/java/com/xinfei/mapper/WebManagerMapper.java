package com.xinfei.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xinfei.DTO.*;
import com.xinfei.DTO.dashboard.NameValueDTO;
import com.xinfei.DTO.webManager.DriverJobQueryDTO;
import com.xinfei.DTO.webManager.TaskPickupDispatchQueryDTO;
import com.xinfei.DTO.webManager.TaskTransportQueryDTO;
import com.xinfei.DTO.webManager.TransportOrderQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Mapper
public interface WebManagerMapper extends BaseMapper {
    IPage<DriverJobDTO> findDriverJobByPage(IPage<DriverJobDTO> iPage, @Param("params") DriverJobQueryDTO dto);

    IPage<TaskPickupDispatchDTO> findTaskPickupDispatchJobByPage(IPage<TaskPickupDispatchDTO> iPage, @Param("params") TaskPickupDispatchQueryDTO dto);

    IPage<TransportOrderDTO> findTransportOrderByPage(IPage<TransportOrderDTO> iPage, @Param("params") TransportOrderQueryDTO dto);

    IPage<TaskTransportDTO> findTaskTransportByPage(IPage<TaskTransportDTO> iPage, @Param("params") TaskTransportQueryDTO dto);

    /** 运输任务按状态分组计数（看板 DSP-01，name 返回状态码字符串） */
    List<NameValueDTO> countTaskTransportByStatus();

    /** 运单按状态分组计数（看板 DSP-01，name 返回状态码字符串） */
    List<NameValueDTO> countTransportOrderByStatus();

    /** 运单按调度状态分组计数（看板 DSP-01，name 返回调度状态码字符串） */
    List<NameValueDTO> countTransportOrderBySchedulingStatus();
}
