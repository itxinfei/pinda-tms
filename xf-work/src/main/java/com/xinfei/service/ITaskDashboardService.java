package com.xinfei.service;

import com.xinfei.DTO.dashboard.TaskDashboardSummaryDTO;
import com.xinfei.DTO.dashboard.TripsVolumeDTO;

import java.util.List;

/**
 * 管理端大屏/仪表盘-运输域统计服务。
 *
 * <p>只读统计，不新增表、不改状态；车辆状态按 OPS-3 口径由运输任务关联推导。</p>
 */
public interface ITaskDashboardService {

    /**
     * 运输域汇总：今日运单、今日任务、在途/待执行车辆、任务五态计数。
     *
     * @return 汇总统计
     */
    TaskDashboardSummaryDTO summary();

    /**
     * 近 N 日各车次运输任务量 Top10（按任务量倒序）。
     *
     * @param days 统计天数，null 默认 7，服务端夹紧到 1~30
     * @return 车次任务量列表
     */
    List<TripsVolumeDTO> tripsVolume(Integer days);
}
