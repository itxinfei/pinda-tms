package com.xinfei.util;

import com.xinfei.DTO.DriverJobDTO;
import com.xinfei.DTO.OrderDTO;
import com.xinfei.DTO.OrgDTO;
import com.xinfei.DTO.TaskTransportDTO;
import com.xinfei.DTO.TransportOrderDTO;
import com.xinfei.DTO.UserDTO;
import com.xinfei.DTO.transportline.TransportTripsDto;
import com.xinfei.DTO.truck.TruckDto;
import com.xinfei.feign.AreaFeign;
import com.xinfei.feign.DriverJobFeign;
import com.xinfei.feign.OrderFeign;
import com.xinfei.feign.OrgFeign;
import com.xinfei.feign.TransportOrderFeign;
import com.xinfei.feign.transportline.TransportTripsFeign;
import com.xinfei.feign.UserFeign;
import com.xinfei.feign.truck.TruckFeign;
import com.xinfei.vo.base.userCenter.SysUserVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * EnrichContext 批量富化单测（锁定 DriverJobController 三重嵌套 N+1 修复 · 巡检报告 P0-1）
 *
 * <p>核心回归点：调用方一次性批量预取后，parse* 方法只查 ctx 里的 Map，
 * 不得发起任何逐行远程调用。本测试用「全部预置 + Feign 全 mock」验证：
 * 富化结果正确，且 8 个 Feign 客户端零交互。</p>
 */
public class BeanUtilEnrichContextTest {

    private TransportTripsFeign transportTripsFeign;
    private OrgFeign orgFeign;
    private UserFeign userFeign;
    private TruckFeign truckFeign;
    private TransportOrderFeign transportOrderFeign;
    private OrderFeign orderFeign;
    private AreaFeign areaFeign;
    private DriverJobFeign driverJobFeign;

    @BeforeEach
    public void setUp() {
        transportTripsFeign = mock(TransportTripsFeign.class);
        orgFeign = mock(OrgFeign.class);
        userFeign = mock(UserFeign.class);
        truckFeign = mock(TruckFeign.class);
        transportOrderFeign = mock(TransportOrderFeign.class);
        orderFeign = mock(OrderFeign.class);
        areaFeign = mock(AreaFeign.class);
        driverJobFeign = mock(DriverJobFeign.class);
    }

    private TaskTransportDTO taskTransport(Long id, Long tripsId, Long startOrg, Long endOrg, Long truckId, List<Long> toIds) {
        TaskTransportDTO dto = new TaskTransportDTO();
        dto.setId(id);
        dto.setTripsId(tripsId);
        dto.setStartOrgId(startOrg);
        dto.setEndOrgId(endOrg);
        dto.setTruckId(truckId);
        dto.setTransportOrderIds(toIds);
        return dto;
    }

    private EnrichContext fullContext() {
        Map<String, TransportTripsDto> tripsMap = new LinkedHashMap<>();
        tripsMap.put("1", trips("1"));
        Map<Long, OrgDTO> orgMap = new LinkedHashMap<>();
        orgMap.put(2L, org(2L, "起点机构"));
        orgMap.put(3L, org(3L, "终点机构"));
        Map<String, TruckDto> truckMap = new LinkedHashMap<>();
        truckMap.put("4", truck("4"));
        Map<Long, TransportOrderDTO> toMap = new LinkedHashMap<>();
        toMap.put(10L, transportOrder(10L, 20L));
        Map<String, OrderDTO> orderMap = new LinkedHashMap<>();
        orderMap.put("20", order("20"));
        Map<Long, UserDTO> userMap = new LinkedHashMap<>();
        userMap.put(5L, user(5L, "司机甲"));
        Map<Long, List<SysUserVo>> driversMap = new LinkedHashMap<>();
        return EnrichContext.builder(transportTripsFeign, orgFeign, userFeign, truckFeign,
                        transportOrderFeign, orderFeign, areaFeign, driverJobFeign)
                .orgMap(orgMap).userMap(userMap).tripsMap(tripsMap).truckMap(truckMap)
                .transportOrderMap(toMap).orderMap(orderMap).driversByTaskId(driversMap).build();
    }

    private TransportTripsDto trips(String id) {
        TransportTripsDto d = new TransportTripsDto();
        d.setId(id);
        return d;
    }

    private OrgDTO org(Long id, String name) {
        OrgDTO d = new OrgDTO();
        d.setId(id);
        d.setName(name);
        return d;
    }

    private TruckDto truck(String id) {
        TruckDto d = new TruckDto();
        d.setId(id);
        return d;
    }

    private TransportOrderDTO transportOrder(Long id, Long orderId) {
        TransportOrderDTO d = new TransportOrderDTO();
        d.setId(id);
        d.setOrderId(orderId);
        return d;
    }

    private OrderDTO order(String id) {
        OrderDTO d = new OrderDTO();
        d.setId(id);
        return d;
    }

    private UserDTO user(Long id, String name) {
        UserDTO d = new UserDTO();
        d.setId(id);
        d.setName(name);
        d.setAccount("acc" + id);
        return d;
    }

    @Test
    @DisplayName("运输任务富化：预置 Map 命中即零远程调用（防 N+1 回归）")
    public void testTaskTransportEnrichZeroRemoteCall() {
        TaskTransportDTO tt = taskTransport(100L, 1L, 2L, 3L, 4L, List.of(10L));
        EnrichContext ctx = fullContext();

        com.xinfei.vo.work.TaskTransportVo vo = BeanUtil.parseTaskTransportDTO2Vo(tt, ctx);

        // 数据来自预置 Map
        assertNotNull(vo.getTransportTrips(), "车次应来自 tripsMap");
        assertNotNull(vo.getStartAgency(), "起点机构应来自 orgMap");
        assertEquals("起点机构", vo.getStartAgency().getName());
        assertNotNull(vo.getEndAgency(), "终点机构应来自 orgMap");
        assertEquals("终点机构", vo.getEndAgency().getName());
        assertNotNull(vo.getTruck(), "车辆应来自 truckMap");
        assertEquals(1, vo.getTransportOrders().size(), "运单应来自 transportOrderMap");
        assertEquals(1, vo.getTransportOrderCount());
        assertTrue(vo.getDrivers().isEmpty(), "无司机预置时返回空列表而非 null");

        // 关键回归点：整段富化没有任何逐行远程调用
        verifyNoInteractions(transportTripsFeign, orgFeign, userFeign, truckFeign,
                transportOrderFeign, orderFeign, areaFeign, driverJobFeign);
    }

    @Test
    @DisplayName("司机作业单富化：机构/司机/嵌套运输任务全部来自预置 Map，零远程调用")
    public void testDriverJobEnrichZeroRemoteCall() {
        DriverJobDTO dj = new DriverJobDTO();
        dj.setId(999L);
        dj.setStartOrgId(2L);
        dj.setEndOrgId(3L);
        dj.setDriverId(5L);
        dj.setTaskTransportId(100L);
        TaskTransportDTO tt = taskTransport(100L, 1L, 2L, 3L, 4L, List.of(10L));
        EnrichContext ctx = fullContext();

        com.xinfei.vo.work.DriverJobVo vo = BeanUtil.parseDriverJobDTO2Vo(dj, tt, ctx);

        assertNotNull(vo.getStartAgency());
        assertEquals("起点机构", vo.getStartAgency().getName());
        assertNotNull(vo.getEndAgency());
        assertNotNull(vo.getDriver(), "司机应来自 userMap");
        assertEquals("司机甲", vo.getDriver().getName());
        assertNotNull(vo.getTaskTransport(), "嵌套运输任务应来自调用方预取的 ttMap");
        assertEquals(1, vo.getTaskTransport().getTransportOrders().size());

        verifyNoInteractions(transportTripsFeign, orgFeign, userFeign, truckFeign,
                transportOrderFeign, orderFeign, areaFeign, driverJobFeign);
    }
}
