package com.xinfei.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.TaskCost;

import java.util.List;
import java.util.Map;

/**
 * 趟次成本 Service（P0-6d/OPS-4 §6.3 最小集：录入/趟明细合计/分页）
 *
 * <p>加油油耗(base_truck_fuel)、成本看板 summary 聚合属 OPS-4 P1，不在本卡。</p>
 */
public interface ITaskCostService extends IService<TaskCost> {

    /**
     * 录入成本（校验必填与类型白名单）。
     *
     * @return 已落库的成本
     */
    TaskCost saveCost(TaskCost cost);

    /**
     * 某趟成本明细 + 合计（趟次利润的输入）。
     *
     * @return {items: List&lt;TaskCost&gt;, total: BigDecimal}
     */
    Map<String, Object> taskCostDetail(Long taskTransportId);

    /**
     * 成本分页（按任务/车辆/类型筛选）。
     */
    Page<TaskCost> page(Integer page, Integer pageSize, Long taskTransportId, Long truckId, String costType);
}
