package com.xinfei.controller;

import com.xinfei.DTO.OrderDTO;
import com.xinfei.DTO.OrderSearchDTO;
import com.xinfei.DTO.OrgDTO;
import com.xinfei.DTO.TaskTransportDTO;
import com.xinfei.DTO.TransportOrderDTO;
import com.xinfei.DTO.transportline.TransportLineDto;
import com.xinfei.feign.OrderFeign;
import com.xinfei.feign.OrgFeign;
import com.xinfei.feign.TransportOrderFeign;
import com.xinfei.feign.TransportTaskFeign;
import com.xinfei.feign.transportline.TransportLineFeign;
import com.xinfei.feign.truck.TruckFeign;
import com.xinfei.vo.dispatch.DispatchAssignForm;
import com.xinfei.vo.dispatch.DispatchBoardVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FR-04 调度一张图聚合控制器单测（DSP-03）
 *
 * <p>纯 Mockito 覆盖三条聚合链路的业务规则，不依赖 Spring 容器与真实中间件：
 * 待调度订单按网点坐标落点、线路过滤与统计、一键派单的校验与建单建任务顺序。</p>
 */
public class DispatchBoardControllerTest {

    private OrderFeign orderFeign;
    private OrgFeign orgFeign;
    private TransportLineFeign lineFeign;
    private TransportOrderFeign transportOrderFeign;
    private TransportTaskFeign transportTaskFeign;
    private TruckFeign truckFeign;

    private DispatchBoardController controller;

    @BeforeEach
    public void setUp() {
        orderFeign = mock(OrderFeign.class);
        orgFeign = mock(OrgFeign.class);
        lineFeign = mock(TransportLineFeign.class);
        transportOrderFeign = mock(TransportOrderFeign.class);
        transportTaskFeign = mock(TransportTaskFeign.class);
        truckFeign = mock(TruckFeign.class);
        controller = new DispatchBoardController(orderFeign, orgFeign, lineFeign,
                transportOrderFeign, transportTaskFeign, truckFeign);
    }

    private OrgDTO org(long id, String name, String lng, String lat) {
        OrgDTO dto = new OrgDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setLongitude(lng);
        dto.setLatitude(lat);
        return dto;
    }

    private TransportLineDto line(String id, String name, String start, String end) {
        TransportLineDto dto = new TransportLineDto();
        dto.setId(id);
        dto.setName(name);
        dto.setStartAgencyId(start);
        dto.setEndAgencyId(end);
        dto.setStatus(1);
        return dto;
    }

    private OrderDTO order(String id, String agencyId) {
        OrderDTO dto = new OrderDTO();
        dto.setId(id);
        dto.setSenderName("发件人" + id);
        dto.setReceiverName("收件人" + id);
        dto.setCurrentAgencyId(agencyId);
        return dto;
    }

    @Test
    @DisplayName("board聚合：待调度订单落网点坐标、线路过滤启用、统计正确")
    public void testBoardAggregate() {
        when(orderFeign.list(any(OrderSearchDTO.class)))
                .thenReturn(List.of(order("9001", "1001"), order("9002", "1001")));
        when(lineFeign.findAll(isNull(), isNull(), isNull())).thenReturn(Arrays.asList(
                line("301", "武汉江岸-武昌干线", "1001", "1002"),
                line("302", "停用线路", "1001", "1003")));
        // 第二条线路为停用状态，应被过滤
        TransportLineDto disabled = line("302", "停用线路", "1001", "1003");
        disabled.setStatus(0);
        when(lineFeign.findAll(isNull(), isNull(), isNull())).thenReturn(Arrays.asList(
                line("301", "武汉江岸-武昌干线", "1001", "1002"), disabled));
        when(orgFeign.list(any(), anyList(), any(), any(), any())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(1);
            return ids.stream().map(id -> {
                if (id == 1001L) return org(1001L, "武汉江岸网点", "114.316", "30.595");
                if (id == 1002L) return org(1002L, "武汉武昌网点", "114.328", "30.555");
                return null;
            }).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toList());
        });

        DispatchBoardVO vo = controller.board(null);

        assertEquals(2, vo.getOrders().size());
        // 订单坐标来自所属网点（WGS84），不是订单自身字段
        assertEquals("114.316", vo.getOrders().get(0).getLongitude());
        assertEquals("30.595", vo.getOrders().get(0).getLatitude());
        assertEquals("武汉江岸网点", vo.getOrders().get(0).getCurrentOrgName());
        // 停用线路被过滤，线路名补齐机构名称
        assertEquals(1, vo.getLines().size());
        assertEquals("武汉江岸网点", vo.getLines().get(0).getStartOrgName());
        assertEquals("武汉武昌网点", vo.getLines().get(0).getEndOrgName());
        // 统计：2 单 / 1 条启用线路 / 2 个覆盖网点
        assertEquals(2, vo.getSummary().getPendingOrderCount());
        assertEquals(1, vo.getSummary().getLineCount());
        assertEquals(2, vo.getSummary().getOrgCount());
    }

    @Test
    @DisplayName("board 机构维度去重：同机构多单只查一次机构（避免 N+1）")
    public void testBoardOrgLookupDedup() {
        when(orderFeign.list(any(OrderSearchDTO.class)))
                .thenReturn(List.of(order("9001", "1001"), order("9002", "1001"), order("9003", "1001")));
        when(lineFeign.findAll(isNull(), isNull(), isNull())).thenReturn(List.of());
        when(orgFeign.list(any(), anyList(), any(), any(), any()))
                .thenReturn(List.of(org(1001L, "武汉江岸网点", "114.316", "30.595")));

        DispatchBoardVO vo = controller.board(null);

        assertEquals(3, vo.getOrders().size());
        // 防 N+1 回归：3 个订单同机构，机构只能批量查一次
        verify(orgFeign, times(1)).list(any(), anyList(), any(), any(), any());
    }

    @Test
    @DisplayName("线路方案：只返回起点→终点匹配的线路，无匹配返回空")
    public void testLineFilter() {
        when(lineFeign.findAll(isNull(), isNull(), isNull())).thenReturn(Arrays.asList(
                line("301", "武汉江岸-武昌干线", "1001", "1002"),
                line("302", "京沪干线", "201", "202")));
        when(orgFeign.list(any(), anyList(), any(), any(), any())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(1);
            return ids.stream().map(id -> org(id, "机构", "1", "1")).collect(java.util.stream.Collectors.toList());
        });

        List<DispatchBoardVO.Line> hit = controller.line("1001", "1002");
        assertEquals(1, hit.size());
        assertEquals("武汉江岸-武昌干线", hit.get(0).getName());

        assertTrue(controller.line("1001", "9999").isEmpty());
    }

    @Test
    @DisplayName("一键派单：建运输单 → 建运输任务，且任务带上运单关联与车辆")
    public void testAssignHappyPath() {
        when(orderFeign.findByIds(anyList())).thenReturn(List.of(order("9001", "1001")));
        when(lineFeign.findAll(isNull(), isNull(), isNull()))
                .thenReturn(List.of(line("301", "武汉江岸-武昌干线", "1001", "1002")));
        when(orgFeign.list(any(), anyList(), any(), any(), any()))
                .thenReturn(List.of(org(1001L, "武汉江岸网点", "114.316", "30.595")));
        when(truckFeign.fineById("501")).thenReturn(new com.xinfei.DTO.truck.TruckDto());

        TransportOrderDTO created = new TransportOrderDTO();
        created.setId(7001L);
        when(transportOrderFeign.save(any(TransportOrderDTO.class))).thenReturn(created);

        TaskTransportDTO task = new TaskTransportDTO();
        task.setId(8888L);
        when(transportTaskFeign.save(any(TaskTransportDTO.class))).thenReturn(task);

        DispatchAssignForm form = new DispatchAssignForm();
        form.setOrderIds(List.of("9001"));
        form.setTruckId("501");

        Map<String, Object> result = controller.assign(form);

        assertEquals(0, result.get("code"));
        assertEquals(1, result.get("transportOrderCount"));
        assertEquals(8888L, result.get("taskId"));
        assertEquals("1001", result.get("startOrgId"));
        assertEquals("1002", result.get("endOrgId"));

        // 运输单的 orderId 必须是 Long（DTO 口径），不是字符串
        ArgumentCaptor<TransportOrderDTO> orderCaptor = ArgumentCaptor.forClass(TransportOrderDTO.class);
        verify(transportOrderFeign).save(orderCaptor.capture());
        assertEquals(9001L, orderCaptor.getValue().getOrderId());

        // 运输任务必须关联刚建的运单
        ArgumentCaptor<TaskTransportDTO> taskCaptor = ArgumentCaptor.forClass(TaskTransportDTO.class);
        verify(transportTaskFeign).save(taskCaptor.capture());
        TaskTransportDTO saved = taskCaptor.getValue();
        assertEquals(1001L, saved.getStartOrgId());
        assertEquals(1002L, saved.getEndOrgId());
        assertEquals(501L, saved.getTruckId());
        assertEquals(List.of(7001L), saved.getTransportOrderIds());
    }

    @Test
    @DisplayName("一键派单前置校验：空订单 / 无车辆 / 车辆不存在 / 订单查不到 / 无可用线路")
    public void testAssignGuards() {
        // 1) 空订单
        DispatchAssignForm empty = new DispatchAssignForm();
        empty.setOrderIds(List.of());
        empty.setTruckId("501");
        assertEquals(400, controller.assign(empty).get("code"));

        // 2) 未选车辆
        DispatchAssignForm noTruck = new DispatchAssignForm();
        noTruck.setOrderIds(List.of("9001"));
        assertEquals(400, controller.assign(noTruck).get("code"));

        // 3) 车辆不存在
        DispatchAssignForm badTruck = new DispatchAssignForm();
        badTruck.setOrderIds(List.of("9001"));
        badTruck.setTruckId("999");
        when(truckFeign.fineById("999")).thenReturn(null);
        assertEquals(400, controller.assign(badTruck).get("code"));

        // 4) 订单不存在
        when(truckFeign.fineById("501")).thenReturn(new com.xinfei.DTO.truck.TruckDto());
        when(orderFeign.findByIds(anyList())).thenReturn(List.of());
        DispatchAssignForm noOrder = new DispatchAssignForm();
        noOrder.setOrderIds(List.of("9001"));
        noOrder.setTruckId("501");
        assertEquals(404, controller.assign(noOrder).get("code"));

        // 5) 起始网点无可用线路：不建单不建任务
        when(orderFeign.findByIds(anyList())).thenReturn(List.of(order("9001", "1001")));
        when(lineFeign.findAll(isNull(), isNull(), isNull())).thenReturn(List.of());
        DispatchAssignForm noLine = new DispatchAssignForm();
        noLine.setOrderIds(List.of("9001"));
        noLine.setTruckId("501");
        assertEquals(400, controller.assign(noLine).get("code"));
        verify(transportOrderFeign, times(0)).save(any());
        verify(transportTaskFeign, times(0)).save(any());
    }

    @Test
    @DisplayName("派单带车次：tripsId 透传到运输任务")
    public void testAssignWithTrips() {
        when(orderFeign.findByIds(anyList())).thenReturn(List.of(order("9001", "1001")));
        when(lineFeign.findAll(isNull(), isNull(), isNull()))
                .thenReturn(List.of(line("301", "武汉江岸-武昌干线", "1001", "1002")));
        when(orgFeign.list(any(), anyList(), any(), any(), any()))
                .thenReturn(List.of(org(1001L, "武汉江岸网点", "114.316", "30.595")));
        when(truckFeign.fineById("501")).thenReturn(new com.xinfei.DTO.truck.TruckDto());
        TransportOrderDTO created = new TransportOrderDTO();
        created.setId(7001L);
        when(transportOrderFeign.save(any())).thenReturn(created);
        when(transportTaskFeign.save(any())).thenReturn(new TaskTransportDTO());

        DispatchAssignForm form = new DispatchAssignForm();
        form.setOrderIds(List.of("9001"));
        form.setTruckId("501");
        form.setTripsId("407");
        controller.assign(form);

        ArgumentCaptor<TaskTransportDTO> captor = ArgumentCaptor.forClass(TaskTransportDTO.class);
        verify(transportTaskFeign).save(captor.capture());
        assertEquals(407L, captor.getValue().getTripsId());
        assertNotNull(captor.getValue().getTransportOrderIds());
    }
}