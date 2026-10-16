package com.xinfei.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.DTO.OrderDTO;
import com.xinfei.DTO.OrderSearchDTO;
import com.xinfei.DTO.OrgDTO;
import com.xinfei.DTO.TaskTransportDTO;
import com.xinfei.DTO.TransportOrderDTO;
import com.xinfei.DTO.transportline.TransportLineDto;
import com.xinfei.common.utils.Result;
import com.xinfei.enums.OrderStatus;
import com.xinfei.feign.OrderFeign;
import com.xinfei.feign.OrgFeign;
import com.xinfei.feign.TransportOrderFeign;
import com.xinfei.feign.TransportTaskFeign;
import com.xinfei.feign.transportline.TransportLineFeign;
import com.xinfei.feign.truck.TruckFeign;
import com.xinfei.util.Rx;
import com.xinfei.vo.dispatch.DispatchAssignForm;
import com.xinfei.vo.dispatch.DispatchBoardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 调度一张图聚合 API（FR-04 / DSP-03）
 *
 * <p>《15》§三 后端聚合契约（D-06 拍板）：xf-dispatch 的调度分类与 DFS 路径规划是内部 Feign、
 * 网关无路由，管理端必须经 {@code xf-web-manager} 聚合暴露，故本控制器是调度一张图的唯一入口。</p>
 *
 * <p>三个端点：</p>
 * <ol>
 *   <li>{@code POST /dispatch/board/page} 待调度订单 + 可用线路聚合（地图静态要素）</li>
 *   <li>{@code GET  /dispatch/line/{fromId}/{toId}} 线路方案（派单前预览路径）</li>
 *   <li>{@code POST /dispatch/assign} 一键派单（建运输单 → 建运输任务）</li>
 * </ol>
 *
 * <p>车辆实时位置不在本聚合内：前端直连 {@code GET /api/netty-service/trace/latestAll}（xf-netty 公开端点），
 * 保持定位职责单一，也避免本控制器转发高频轮询。</p>
 */
@Slf4j
@RestController
@Tag(name = "调度一张图API")
@RequestMapping("dispatch")
@RequiredArgsConstructor
public class DispatchBoardController {

    private final OrderFeign orderFeign;
    private final OrgFeign orgFeign;
    private final TransportLineFeign transportLineFeign;
    private final TransportOrderFeign transportOrderFeign;
    private final TransportTaskFeign transportTaskFeign;
    private final TruckFeign truckFeign;

    /** 线路一次性拉取上限：TransportLineFeign 无起止机构过滤参数，内存过滤前先限流防超大结果集 */
    private static final int LINE_QUERY_LIMIT = 2000;

    @Operation(summary = "调度一张图聚合数据（待调度订单 + 可用线路）")
    @PostMapping("/board/page")
    @SaCheckPermission("dispatch:board:list")
    public DispatchBoardVO board(@RequestParam(name = "currentAgencyId", required = false) String currentAgencyId) {
        DispatchBoardVO vo = new DispatchBoardVO();
        vo.setOrders(loadPendingOrders(currentAgencyId));
        vo.setLines(loadLines());
        DispatchBoardVO.Summary summary = new DispatchBoardVO.Summary();
        summary.setPendingOrderCount(vo.getOrders().size());
        summary.setLineCount(vo.getLines().size());
        summary.setOrgCount((int) vo.getLines().stream()
                .flatMap(line -> Stream.of(line.getStartOrgId(), line.getEndOrgId()))
                .filter(StringUtils::isNotBlank).distinct().count());
        vo.setSummary(summary);
        return vo;
    }

    @Operation(summary = "线路方案（起点机构 → 目的机构的可用线路）")
    @GetMapping("/line/{fromId}/{toId}")
    @SaCheckPermission("dispatch:board:line")
    public List<DispatchBoardVO.Line> line(@PathVariable("fromId") String fromId,
                                           @PathVariable("toId") String toId) {
        if (StringUtils.isBlank(fromId) || StringUtils.isBlank(toId)) {
            return Collections.emptyList();
        }
        return loadLines().stream()
                .filter(line -> Objects.equals(line.getStartOrgId(), fromId)
                        && Objects.equals(line.getEndOrgId(), toId))
                .collect(Collectors.toList());
    }

    @Operation(summary = "一键派单（建运输单 + 建运输任务）")
    @PostMapping("/assign")
    @SaCheckPermission("dispatch:board:assign")
    public Object assign(@RequestBody DispatchAssignForm form) {
        if (form == null || form.getOrderIds() == null || form.getOrderIds().isEmpty()) {
            return Result.error(400, "请先选择待调度订单");
        }
        if (StringUtils.isBlank(form.getTruckId())) {
            return Result.error(400, "请选择承运车辆");
        }
        // 车辆存在性校验：避免前端传入已删除车辆导致建出无主运输任务
        if (truckFeign.fineById(form.getTruckId()) == null) {
            return Result.error(400, "承运车辆不存在或已停用");
        }

        List<OrderDTO> orders = Rx.list(orderFeign.findByIds(form.getOrderIds()));
        if (orders.isEmpty()) {
            return Result.error(404, "订单不存在或已变更状态");
        }

        // 起始机构 = 订单当前入库网点；目的机构取该网点第一条可用线路的终点
        String startOrgId = orders.get(0).getCurrentAgencyId();
        List<DispatchBoardVO.Line> candidates = loadLines().stream()
                .filter(line -> Objects.equals(line.getStartOrgId(), startOrgId))
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            return Result.error(400, "起始网点没有可用线路，请先在「运输线路」维护");
        }
        String endOrgId = candidates.get(0).getEndOrgId();

        // 1) 建运输单（work_transport_order 是运单与运输任务的关联载体，每单一张）
        List<Long> transportOrderIds = new ArrayList<>();
        for (OrderDTO order : orders) {
            Long orderId = toLong(order.getId());
            if (orderId == null) {
                continue;
            }
            TransportOrderDTO dto = new TransportOrderDTO();
            dto.setOrderId(orderId);
            TransportOrderDTO created = transportOrderFeign.save(dto);
            if (created != null && created.getId() != null) {
                transportOrderIds.add(created.getId());
            }
        }
        if (transportOrderIds.isEmpty()) {
            return Result.error(500, "运输单创建失败");
        }

        // 2) 建运输任务并关联运单（xf-work saveWithRelations 会置待执行/待分配状态）
        TaskTransportDTO taskDTO = new TaskTransportDTO();
        taskDTO.setStartOrgId(toLong(startOrgId));
        taskDTO.setEndOrgId(toLong(endOrgId));
        taskDTO.setTruckId(toLong(form.getTruckId()));
        Long tripsId = toLong(form.getTripsId());
        if (tripsId != null) {
            taskDTO.setTripsId(tripsId);
        }
        taskDTO.setTransportOrderIds(transportOrderIds);
        TaskTransportDTO task = transportTaskFeign.save(taskDTO);
        // fallback 熔断时 save 返回 null：此时已建的运单成孤儿，必须抛异常让事务回滚，
        // 不能返回成功让前端误以为派单完成
        if (task == null || task.getId() == null) {
            throw new IllegalStateException("运输任务创建失败（xf-work 不可用），派单已回滚");
        }

        // 返回「不含 code 的裸 Map」由 GlobalResponseAdvice 包成 {code,msg,data}：
        // Result 继承 HashMap 且 ok(map) 是扁平 putAll，自带 code 会被 Advice 原样放行，
        // 前端 request.ts 取 body.data 得到 undefined（曾导致提示「生成 0 张运输单」）
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transportOrderCount", transportOrderIds.size());
        data.put("taskId", task.getId());
        data.put("startOrgId", startOrgId);
        data.put("endOrgId", endOrgId);
        return data;
    }

    /** 待调度订单（网点入库状态）：经所属网点坐标上地图 */
    private List<DispatchBoardVO.OrderPoint> loadPendingOrders(String currentAgencyId) {
        OrderSearchDTO query = new OrderSearchDTO();
        query.setStatus(OrderStatus.OUTLETS_WAREHOUSE.getCode());
        if (StringUtils.isNotBlank(currentAgencyId)) {
            query.setCurrentAgencyId(currentAgencyId);
        }
        List<OrderDTO> orders = Rx.list(orderFeign.list(query));
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }
        // 机构坐标一次性取，避免每单一次 Feign
        Map<String, OrgDTO> orgMap = orgsById(orders.stream()
                .map(OrderDTO::getCurrentAgencyId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList()));

        return orders.stream().map(order -> {
            DispatchBoardVO.OrderPoint point = new DispatchBoardVO.OrderPoint();
            point.setId(order.getId());
            point.setOrderNo(order.getId());
            point.setSenderName(order.getSenderName());
            point.setReceiverName(order.getReceiverName());
            point.setCurrentOrgId(order.getCurrentAgencyId());
            OrgDTO org = orgMap.get(order.getCurrentAgencyId());
            if (org != null) {
                point.setCurrentOrgName(org.getName());
                point.setLongitude(org.getLongitude());
                point.setLatitude(org.getLatitude());
            }
            point.setCreateTime(order.getCreateTime() == null ? null : order.getCreateTime().toString());
            return point;
        }).collect(Collectors.toList());
    }

    /** 可用线路（启用状态），附带起止机构名称 */
    private List<DispatchBoardVO.Line> loadLines() {
        List<TransportLineDto> lineDtos = Rx.list(transportLineFeign.findAll(null, null, null));
        if (lineDtos == null || lineDtos.isEmpty()) {
            return Collections.emptyList();
        }
        List<TransportLineDto> limited = lineDtos.size() > LINE_QUERY_LIMIT
                ? lineDtos.subList(0, LINE_QUERY_LIMIT) : lineDtos;
        List<TransportLineDto> enabled = limited.stream()
                .filter(line -> line.getStatus() == null || line.getStatus() == 1)
                .collect(Collectors.toList());

        Map<String, OrgDTO> orgMap = orgsById(enabled.stream()
                .flatMap(line -> Stream.of(line.getStartAgencyId(), line.getEndAgencyId()))
                .filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList()));

        return enabled.stream().map(line -> {
            DispatchBoardVO.Line vo = new DispatchBoardVO.Line();
            vo.setId(line.getId());
            vo.setName(line.getName());
            vo.setStartOrgId(line.getStartAgencyId());
            vo.setEndOrgId(line.getEndAgencyId());
            OrgDTO start = orgMap.get(line.getStartAgencyId());
            OrgDTO end = orgMap.get(line.getEndAgencyId());
            vo.setStartOrgName(start == null ? null : start.getName());
            vo.setEndOrgName(end == null ? null : end.getName());
            if (start != null) {
                vo.setStartOrgLng(start.getLongitude());
                vo.setStartOrgLat(start.getLatitude());
            }
            if (end != null) {
                vo.setEndOrgLng(end.getLongitude());
                vo.setEndOrgLat(end.getLatitude());
            }
            vo.setDistance(line.getDistance());
            vo.setCost(line.getCost());
            vo.setEstimatedTime(line.getEstimatedTime());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 批量取机构并按字符串 id 建索引（前端以字符串传递 id，避免雪花精度问题）。
     *
     * <p>走 {@code OrgFeign.list(ids=...)} 一次远程查完，避免逐个机构 get 的 N+1
     * （线路多时可达数十次调用）；id 为空的项直接跳过，不参与查询。</p>
     */
    private Map<String, OrgDTO> orgsById(List<String> orgIds) {
        Map<String, OrgDTO> map = new LinkedHashMap<>();
        if (orgIds == null || orgIds.isEmpty()) {
            return map;
        }
        List<Long> ids = orgIds.stream().map(this::toLong).filter(Objects::nonNull).distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return map;
        }
        for (OrgDTO org : Rx.list(orgFeign.list(null, ids, null, null, null))) {
            if (org != null && org.getId() != null) {
                map.put(String.valueOf(org.getId()), org);
            }
        }
        return map;
    }

    private Long toLong(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}