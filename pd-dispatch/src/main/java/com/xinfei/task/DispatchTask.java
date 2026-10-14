package com.xinfei.task;
import com.xinfei.DTO.OrderClassifyGroupDTO;
import com.xinfei.DTO.OrderLineSimpleDTO;
import com.xinfei.DTO.OrderLineTripsTruckDriverDTO;
import com.xinfei.DTO.TaskTransportDTO;
import com.xinfei.service.IBusinessOperationService;
import com.xinfei.service.IScheduleJobService;
import com.xinfei.service.ITaskOrderClassifyService;
import com.xinfei.service.ITaskRoutePlanningService;
import com.xinfei.service.ITaskTripsSchedulingService;
import com.xinfei.utils.IdConvert;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
/**
 * 调度任务，每个机构都有一个任务
 */
@Slf4j
@Component("dispatchTask")
public class DispatchTask {
    @Autowired
    private ITaskOrderClassifyService orderClassifyService;
    @Autowired
    private ITaskTripsSchedulingService truckSchedulingService;
    @Autowired
    private ITaskRoutePlanningService routePlanningService;
    @Autowired
    private IBusinessOperationService businessOperationService;
    /**
     * ScheduleJobServiceImpl 已注入本类，此处反向注入用 @Lazy 打破循环依赖
     */
    @Autowired
    @Lazy
    private IScheduleJobService scheduleJobService;

    /**
     * 执行单个机构的智能调度全链路（订单分类 → 路线规划 → 运输任务 → 车次车辆司机）
     */
    public void run(Long businessId, String params, Long jobId, Long logId) {
        String LOGID = businessId + "" + System.currentTimeMillis();
        log.info("[{}]TestTask定时任务正在执行，参数为：{},{},{},{}", LOGID, businessId, params, jobId, logId);

        // 订单分类
        List<OrderClassifyGroupDTO> orderClassifys = orderClassifyService.execute(businessId, jobId, logId);
        log.info("[{}] 订单分类完成:{}", LOGID, orderClassifys);

        // 路线规划
        List<OrderLineSimpleDTO> orderLineSimpleDTOS = routePlanningService.execute(orderClassifys, businessId, jobId, logId, params);
        log.info("[{}] 路线规划完成:{}", LOGID, orderLineSimpleDTOS);

        //  创建运输任务； 关联运单
        Map<String, TaskTransportDTO> transportTaskMap = businessOperationService.createTransportOrderTask(orderLineSimpleDTOS);
        log.info("[{}] 运输任务创建完成:{}", LOGID, transportTaskMap);

        // 车次 车辆 司机
        List<OrderLineTripsTruckDriverDTO> orderLineTripsTruckDriverDTOS = truckSchedulingService.execute(orderLineSimpleDTOS, businessId, jobId, logId, businessId);
        log.info("[{}] 规划车次车辆司机:{}", LOGID, orderLineTripsTruckDriverDTOS);

        //  完善运输任务信息； 创建司机作业任务；
        businessOperationService.updateTransportTask(orderLineTripsTruckDriverDTOS, transportTaskMap);
        log.info("[{}] 安排车次车辆司机,当前机构调度结束！！！！！", LOGID);
    }

    /**
     * XXL-Job 调度入口：xxl-job-admin 控制台按机构配置任务，任务参数为机构ID。
     * 租户绑定/失败重试/执行日志统一在 ScheduleJobServiceImpl#executeWithRetry 中处理。
     */
    @XxlJob("dispatchTask")
    public void runJob() {
        String param = XxlJobHelper.getJobParam();
        Long orgId = IdConvert.toLong(param);
        if (orgId == null) {
            log.error("[XXL-Job] dispatchTask 参数非法（需要机构ID），param={}", param);
            XxlJobHelper.handleFail("机构ID参数非法: " + param);
            return;
        }
        scheduleJobService.runByOrgId(orgId);
    }
}
