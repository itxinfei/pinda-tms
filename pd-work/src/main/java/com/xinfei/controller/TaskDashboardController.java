package com.xinfei.controller;

import com.xinfei.DTO.dashboard.TaskDashboardSummaryDTO;
import com.xinfei.DTO.dashboard.TripsVolumeDTO;
import com.xinfei.service.ITaskDashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 运输任务大屏/仪表盘统计
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("transport-task/dashboard")
public class TaskDashboardController {

    @Autowired
    private ITaskDashboardService taskDashboardService;

    /**
     * 运输域汇总：今日运单、今日任务、在途/待执行车辆、任务五态计数
     *
     * @return 汇总统计
     */
    @GetMapping("/summary")
    public TaskDashboardSummaryDTO summary() {
        return taskDashboardService.summary();
    }

    /**
     * 近 N 日各车次运输任务量 Top10
     *
     * @param days 统计天数（1~30，默认 7）
     * @return 车次任务量列表
     */
    @GetMapping("/trips-volume")
    public List<TripsVolumeDTO> tripsVolume(@RequestParam(name = "days", defaultValue = "7") Integer days) {
        return taskDashboardService.tripsVolume(days);
    }
}
