package com.xinfei.service;

import com.xinfei.DTO.OrderClassifyGroupDTO;
import com.xinfei.DTO.OrderLineSimpleDTO;

import java.util.List;

/**
 * 路线规划
 */
public interface ITaskRoutePlanningService {
    /**
     * 执行
     * @return
     */
    List<OrderLineSimpleDTO> execute(List<OrderClassifyGroupDTO> orderClassifyGroupDTOS, Long orgId, Long jobId, Long logId, String params);
}
