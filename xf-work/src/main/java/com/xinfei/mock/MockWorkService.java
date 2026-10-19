package com.xinfei.mock;

import com.xinfei.common.context.TenantContextHolder;
import com.xinfei.entity.DriverJob;
import com.xinfei.entity.TaskPickupDispatch;
import com.xinfei.entity.TaskTransport;
import com.xinfei.entity.TransportOrder;
import com.xinfei.entity.TransportOrderTask;
import com.xinfei.enums.driverjob.DriverJobStatus;
import com.xinfei.enums.pickuptask.PickupDispatchTaskAssignedStatus;
import com.xinfei.enums.pickuptask.PickupDispatchTaskStatus;
import com.xinfei.enums.pickuptask.PickupDispatchTaskType;
import com.xinfei.enums.transportorder.TransportOrderSchedulingStatus;
import com.xinfei.enums.transportorder.TransportOrderStatus;
import com.xinfei.enums.transporttask.TransportTaskAssignedStatus;
import com.xinfei.enums.transporttask.TransportTaskLoadingStatus;
import com.xinfei.enums.transporttask.TransportTaskStatus;
import com.xinfei.service.IDriverJobService;
import com.xinfei.service.ITaskPickupDispatchService;
import com.xinfei.service.ITaskTransportService;
import com.xinfei.service.ITransportOrderService;
import com.xinfei.service.ITransportOrderTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * mock 作业数据生成（xf-work 域：运单/干线任务/司机作业单/取派件任务）。
 *
 * <p>数据来源＝xf-oms 已生成的 AUTO-MOCK 订单（跨域只读不写，不违反模块边界），
 * 演示账号绑定口径（2026-10-19 实测定稿）：</p>
 * <ul>
 *   <li>司机作业单 driverId = 9111/9112（demo01/02，base_truck_driver.id 与 sys_user.id
 *       种子刻意同值——BFF 按 sys_user.id 查、调度按 base_truck_driver.id 写，必须保持该对齐）；</li>
 *   <li>取派件任务 courierId = 9211/9212（demo08/09，BFF 直查 sys_user.id）。</li>
 * </ul>
 * <p>任务含"待执行/进行中"两档，移动两端登录即有可操作待办。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Conditional(MockGuardCondition.class)
public class MockWorkService {

    /** 演示司机（demo01/demo02，id==user_id 对齐种子） */
    private static final long[] DRIVER_IDS = {9111L, 9112L};
    /** 演示快递员（demo08/demo09） */
    private static final long[] COURIER_IDS = {9211L, 9212L};
    /** 干线车辆（车队 610 种子车 501~505） */
    private static final long[] TRUCK_IDS = {501L, 502L, 503L, 504L, 505L};
    /** 起点武汉江岸网点 / 干线终点转运中心（北京/成都/西安） */
    private static final long START_ORG = 1001L;
    private static final long[] END_ORGS = {201L, 204L, 206L};

    private static final String MOCK_MARK = "AUTO-MOCK";

    /**
     * 城际订单类型码——口径＝oms 域 OrderType.OUTCITY(2)。xf-work 不依赖
     * xf-service-oms-domain，为 mock 判断引入跨模块依赖不值当，以常量+出处注释锚定。
     */
    private static final int CITY_ORDER_TYPE = 2;

    private final ITransportOrderService transportOrderService;
    private final ITaskTransportService taskTransportService;
    private final IDriverJobService driverJobService;
    private final ITaskPickupDispatchService taskPickupDispatchService;
    private final ITransportOrderTaskService transportOrderTaskService;
    private final DataSource dataSource;

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 为最近一批 AUTO-MOCK 订单补齐作业链数据
     *
     * @param orderLimit 取最近 N 条 mock 订单（默认 30）
     * @param taskLimit  最多造 N 个干线任务（含作业单）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generate(int orderLimit, int taskLimit) {
        // 系统态租户上下文：docker exec 直调无租户头，多租户插件会回落 tenant_id=0
        // 导致登录租户（1）查不到 mock 数据——必须显式置 1（惯例见 RabbitGpsTraceConsumer）
        TenantContextHolder.setTenantId(1L);
        try {
            return doGenerate(orderLimit, taskLimit);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private Map<String, Object> doGenerate(int orderLimit, int taskLimit) {
        JdbcTemplate jdbc = jdbc();
        // 跨域只读：最近 mock 订单（城际单优先给干线任务）
        List<Map<String, Object>> orders = jdbc.queryForList(
                "SELECT id, order_type FROM oms_order WHERE remark LIKE '" + MOCK_MARK + "%' AND deleted = 0 "
                        + "ORDER BY create_time DESC LIMIT " + Math.max(1, orderLimit));
        if (orders.isEmpty()) {
            throw new IllegalStateException("无 AUTO-MOCK 订单——先调 xf-oms POST /mock/generate");
        }
        // 已占用订单（取派件一单一任务，避免重复造）
        Set<Long> occupied = new HashSet<>(jdbc.queryForList(
                "SELECT order_id FROM work_pickup_dispatch_task WHERE order_id IS NOT NULL", Long.class));
        // 已造过运单的订单同理跳过
        Set<Long> waybillOrderIds = new HashSet<>(jdbc.queryForList(
                "SELECT order_id FROM work_transport_order WHERE deleted = 0", Long.class));
        List<Long> tripIds = jdbc.queryForList(
                "SELECT id FROM base_transport_trips WHERE deleted = 0 LIMIT 5", Long.class);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int waybills = 0, tasks = 0, jobs = 0, pickups = 0;

        for (Map<String, Object> order : orders) {
            Long orderId = ((Number) order.get("id")).longValue();
            int orderType = ((Number) order.get("order_type")).intValue();
            if (waybillOrderIds.contains(orderId)) {
                continue;
            }

            // 1) 运单（全部 mock 订单各一条）
            TransportOrder waybill = new TransportOrder();
            waybill.setOrderId(orderId);
            waybill.setStatus(rnd.nextBoolean()
                    ? TransportOrderStatus.CREATED.getCode()
                    : TransportOrderStatus.LOADED.getCode());
            waybill.setSchedulingStatus(TransportOrderSchedulingStatus.SCHEDULED.getCode());
            waybill.setCreateTime(LocalDateTime.now());
            transportOrderService.save(waybill);
            waybills++;

            // 2) 干线任务 + 司机作业单（城际单、限 taskLimit 个）
            if (orderType == CITY_ORDER_TYPE && tasks < taskLimit && !tripIds.isEmpty()) {
                TaskTransport task = buildTask(rnd, tripIds);
                taskTransportService.save(task);
                tasks++;

                TransportOrderTask rel = new TransportOrderTask();
                rel.setTransportOrderId(waybill.getId());
                rel.setTaskId(task.getId());
                transportOrderTaskService.save(rel);

                driverJobService.save(buildJob(rnd, task.getId()));
                jobs++;
            }

            // 3) 取派件任务（未占用的订单，取/派件各半，待执行为主）
            if (!occupied.contains(orderId)) {
                taskPickupDispatchService.save(buildPickup(rnd, orderId));
                pickups++;
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("waybills", waybills);
        stats.put("transportTasks", tasks);
        stats.put("driverJobs", jobs);
        stats.put("pickupDispatchTasks", pickups);
        log.info("[mock] work 生成完成: {}", stats);
        return stats;
    }

    private TaskTransport buildTask(ThreadLocalRandom rnd, List<Long> tripIds) {
        TaskTransport task = new TaskTransport();
        task.setTripsId(tripIds.get(rnd.nextInt(tripIds.size())));
        task.setStartOrgId(START_ORG);
        task.setEndOrgId(END_ORGS[rnd.nextInt(END_ORGS.length)]);
        // 待执行/进行中各半——司机端既有可接的活也有进行中的活
        task.setStatus(rnd.nextBoolean()
                ? TransportTaskStatus.PENDING.getCode()
                : TransportTaskStatus.PROCESSING.getCode());
        task.setAssignedStatus(TransportTaskAssignedStatus.DISTRIBUTED.getCode());
        task.setLoadingStatus(rnd.nextBoolean()
                ? TransportTaskLoadingStatus.HALF.getCode()
                : TransportTaskLoadingStatus.FULL.getCode());
        task.setTruckId(TRUCK_IDS[rnd.nextInt(TRUCK_IDS.length)]);
        // 提货坐标=武汉，交付坐标=目的地（成都/北京/西安附近）
        task.setPickupLongitude(BigDecimal.valueOf(114.30 + rnd.nextDouble() * 0.05));
        task.setPickupLatitude(BigDecimal.valueOf(30.59 + rnd.nextDouble() * 0.05));
        task.setDeliverLongitude(BigDecimal.valueOf(104.06 + rnd.nextDouble() * 0.1));
        task.setDeliverLatitude(BigDecimal.valueOf(30.57 + rnd.nextDouble() * 0.1));
        LocalDateTime base = LocalDateTime.now().withMinute(0);
        task.setPlanDepartureTime(base.plusHours(2 + rnd.nextInt(6)));
        task.setPlanArrivalTime(base.plusDays(1));
        task.setPlanPickUpTime(base.plusHours(1 + rnd.nextInt(3)));
        task.setPlanDeliveryTime(base.plusDays(1).plusHours(rnd.nextInt(8)));
        task.setCreateTime(LocalDateTime.now());
        return task;
    }

    private DriverJob buildJob(ThreadLocalRandom rnd, Long taskTransportId) {
        DriverJob job = new DriverJob();
        job.setDriverId(DRIVER_IDS[rnd.nextInt(DRIVER_IDS.length)]);
        job.setTaskTransportId(taskTransportId);
        job.setStartOrgId(START_ORG);
        job.setEndOrgId(END_ORGS[rnd.nextInt(END_ORGS.length)]);
        job.setStatus(rnd.nextBoolean()
                ? DriverJobStatus.PENDING.getCode()
                : DriverJobStatus.PROCESSING.getCode());
        job.setStartHandover("网点值班长");
        job.setFinishHandover("目的网点仓管");
        LocalDateTime base = LocalDateTime.now().withMinute(0);
        job.setPlanDepartureTime(base.plusHours(2 + rnd.nextInt(6)));
        job.setPlanArrivalTime(base.plusDays(1));
        job.setCreateTime(LocalDateTime.now());
        return job;
    }

    private TaskPickupDispatch buildPickup(ThreadLocalRandom rnd, Long orderId) {
        TaskPickupDispatch pickup = new TaskPickupDispatch();
        pickup.setOrderId(orderId);
        pickup.setTaskType(rnd.nextBoolean()
                ? PickupDispatchTaskType.PICKUP.getCode()
                : PickupDispatchTaskType.DISPATCH.getCode());
        // 待执行为主（60%）——快递员端可"开始任务→扫码→完成"全流程演练
        pickup.setStatus(rnd.nextInt(5) < 3
                ? PickupDispatchTaskStatus.PENDING.getCode()
                : PickupDispatchTaskStatus.PROCESSING.getCode());
        pickup.setCourierId(COURIER_IDS[rnd.nextInt(COURIER_IDS.length)]);
        pickup.setOrgId(START_ORG);
        pickup.setAssignedStatus(PickupDispatchTaskAssignedStatus.DISTRIBUTED.getCode());
        LocalDateTime base = LocalDateTime.now().withMinute(0);
        pickup.setEstimatedStartTime(base.plusHours(1 + rnd.nextInt(4)));
        pickup.setEstimatedEndTime(base.plusHours(5 + rnd.nextInt(4)));
        pickup.setMark(MOCK_MARK);
        pickup.setCreateTime(LocalDateTime.now());
        return pickup;
    }

    /**
     * 清理 AUTO-MOCK 订单关联的全部作业链数据（物理删，逆依赖序：作业单→任务→关联→取派件→运单）。
     * 必须先清 xf-netty（轨迹/告警引用任务 id），再清本域，最后清 xf-oms（oms 侧有顺序守护）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cleanup() {
        JdbcTemplate jdbc = jdbc();
        Map<String, Object> stats = new LinkedHashMap<>();
        List<Long> orderIds = jdbc.queryForList(
                "SELECT id FROM oms_order WHERE remark LIKE '" + MOCK_MARK + "%'", Long.class);
        if (orderIds.isEmpty()) {
            stats.put("total", 0);
            return stats;
        }
        String inOrders = joinIds(orderIds);
        List<Long> waybillIds = jdbc.queryForList(
                "SELECT id FROM work_transport_order WHERE order_id IN (" + inOrders + ")", Long.class);

        int total = 0;
        if (!waybillIds.isEmpty()) {
            String inWaybills = joinIds(waybillIds);
            List<Long> taskIds = jdbc.queryForList(
                    "SELECT task_id FROM work_transport_order_task WHERE transport_order_id IN (" + inWaybills + ")", Long.class);
            if (!taskIds.isEmpty()) {
                String inTasks = joinIds(taskIds);
                total += jdbc.update("DELETE FROM work_driver_job WHERE task_transport_id IN (" + inTasks + ")");
                total += jdbc.update("DELETE FROM work_task_transport WHERE id IN (" + inTasks + ")");
            }
            total += jdbc.update("DELETE FROM work_transport_order_task WHERE transport_order_id IN (" + inWaybills + ")");
        }
        total += jdbc.update("DELETE FROM work_pickup_dispatch_task WHERE order_id IN (" + inOrders + ")");
        total += jdbc.update("DELETE FROM work_transport_order WHERE order_id IN (" + inOrders + ")");
        stats.put("total", total);
        log.info("[mock] work 清理完成: {}", stats);
        return stats;
    }

    /** 拼接 IN 子句 id 串（元素类型必须保持 Long——放行字符串将打开 SQL 注入面） */
    private String joinIds(List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id);
        }
        return sb.toString();
    }
}
