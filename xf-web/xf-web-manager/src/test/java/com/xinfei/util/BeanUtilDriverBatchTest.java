package com.xinfei.util;

import com.xinfei.DTO.DriverJobDTO;
import com.xinfei.DTO.UserDTO;
import com.xinfei.feign.DriverJobFeign;
import com.xinfei.feign.UserFeign;
import com.xinfei.vo.base.userCenter.SysUserVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 运输任务司机信息批量组装单测（修复列表 N+1 的回归护栏）
 *
 * <p>分页列表原本逐行远程查（每行 1 次 driver-job + 1 次用户），改为整页一次批量查询。
 * 本测试锁定三个要点：调用次数、去重、按任务分组。</p>
 */
public class BeanUtilDriverBatchTest {

    private DriverJobFeign driverJobFeign;
    private UserFeign userFeign;

    @BeforeEach
    public void setUp() {
        driverJobFeign = mock(DriverJobFeign.class);
        userFeign = mock(UserFeign.class);
    }

    private DriverJobDTO job(Long driverId, Long taskId) {
        DriverJobDTO dto = new DriverJobDTO();
        dto.setDriverId(driverId);
        dto.setTaskTransportId(taskId);
        return dto;
    }

    private UserDTO user(Long id, String name) {
        UserDTO dto = new UserDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setAccount("acc" + id);
        return dto;
    }

    @Test
    @DisplayName("批量取司机：一页任务只发2 次远程调用（防 N+1 回归）")
    public void testBatchUsesTwoRemoteCalls() {
        when(driverJobFeign.findAllByTaskTransportIds(anyList())).thenReturn(Arrays.asList(
                job(9101L, 1L), job(9111L, 2L), job(9113L, 2L)));
        when(userFeign.list(anyList(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(user(9101L, "张三"), user(9111L, "李四"), user(9113L, "王五")));

        Map<Long, List<SysUserVo>> result = BeanUtil.parseTaskDriversBatch(
                List.of(1L, 2L, 3L), driverJobFeign, userFeign);

        assertEquals(2, result.size());
        // 关键回归点：一页 3 个任务仍然只有 1 次 driver-job + 1 次用户查询
        verify(driverJobFeign, times(1)).findAllByTaskTransportIds(anyList());
        verify(userFeign, times(1)).list(anyList(), isNull(), isNull(), isNull());
        // 无司机的任务不出现在结果里（调用方按getOrDefault 兜空列表）
        assertTrue(!result.containsKey(3L));
    }

    @Test
    @DisplayName("批量取司机：先查作业单再查用户，调用顺序固定")
    public void testBatchCallOrder() {
        when(driverJobFeign.findAllByTaskTransportIds(anyList())).thenReturn(List.of(job(9101L, 1L)));
        when(userFeign.list(anyList(), isNull(), isNull(), isNull())).thenReturn(List.of(user(9101L, "张三")));

        BeanUtil.parseTaskDriversBatch(List.of(1L), driverJobFeign, userFeign);

        InOrder order = inOrder(driverJobFeign, userFeign);
        order.verify(driverJobFeign).findAllByTaskTransportIds(anyList());
        order.verify(userFeign).list(anyList(), isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("批量取司机：同一任务多司机全部保留，重复作业单按userId 去重")
    public void testBatchMultiDriverAndDedup() {
        // 同一任务同一司机出现两条作业单（历史重复），按userId 去重
        when(driverJobFeign.findAllByTaskTransportIds(anyList())).thenReturn(Arrays.asList(
                job(9111L, 2L), job(9113L, 2L), job(9111L, 2L)));
        when(userFeign.list(anyList(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(user(9111L, "李四"), user(9113L, "王五")));

        Map<Long, List<SysUserVo>> result = BeanUtil.parseTaskDriversBatch(
                List.of(2L), driverJobFeign, userFeign);

        List<SysUserVo> drivers = result.get(2L);
        assertEquals(2, drivers.size(), "重复作业单应按userId 去重，两名司机都要保留");
        assertTrue(drivers.stream().anyMatch(item -> "李四".equals(item.getName())));
        assertTrue(drivers.stream().anyMatch(item -> "王五".equals(item.getName())));
    }

    @Test
    @DisplayName("批量取司机：空入参/空结果/无司机 id 均安全返回空且不打多余远程调用")
    public void testBatchEdgeCases() {
        // 1) 空任务集合：不调用任何 Feign
        assertTrue(BeanUtil.parseTaskDriversBatch(List.of(), driverJobFeign, userFeign).isEmpty());
        verifyNoInteractions(driverJobFeign);
        verifyNoInteractions(userFeign);

        // 2) 无司机作业单：不查用户
        when(driverJobFeign.findAllByTaskTransportIds(anyList())).thenReturn(List.of());
        assertTrue(BeanUtil.parseTaskDriversBatch(List.of(1L), driverJobFeign, userFeign).isEmpty());
        verifyNoInteractions(userFeign);

        // 3) Feign 返 null：Rx 兜底为空，不抛 NPE
        when(driverJobFeign.findAllByTaskTransportIds(anyList())).thenReturn(null);
        assertTrue(BeanUtil.parseTaskDriversBatch(List.of(1L), driverJobFeign, userFeign).isEmpty());
    }
}