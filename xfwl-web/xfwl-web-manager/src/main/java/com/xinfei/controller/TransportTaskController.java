package com.xinfei.controller;
import cn.dev33.satoken.annotation.SaCheckPermission;

import com.xinfei.DTO.OrgDTO;
import com.xinfei.DTO.TaskTransportDTO;
import com.xinfei.DTO.webManager.TaskTransportQueryDTO;
import com.xinfei.DTO.webManager.TransportBoardDTO;
import com.xinfei.common.utils.PageResponse;
import com.xinfei.feign.AreaFeign;
import com.xinfei.feign.DriverJobFeign;
import com.xinfei.feign.OrderFeign;
import com.xinfei.feign.OrgFeign;
import com.xinfei.feign.TransportOrderFeign;
import com.xinfei.feign.TransportTaskFeign;
import com.xinfei.feign.UserFeign;
import com.xinfei.feign.transportline.TransportTripsFeign;
import com.xinfei.feign.truck.TruckFeign;
import com.xinfei.feign.webManager.WebManagerFeign;
import com.xinfei.util.BeanUtil;
import com.xinfei.util.Rx;
import com.xinfei.vo.work.PointDTO;
import com.xinfei.vo.work.TaskTransportVo;
import com.xinfei.vo.base.userCenter.SysUserVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Objects;


/**
 * <p>
 * 运输任务表 前端控制器
 * </p>
 *
 * @author jpf
 * @since 2019-12-29
 */
@Slf4j
@RestController
@Tag(name = "运输任务API")
@RequestMapping("transport-task-manager")
public class TransportTaskController {
    @Autowired
    private TransportTaskFeign transportTaskFeign;
    @Autowired
    private TransportTripsFeign transportTripsFeign;
    @Autowired
    private OrgFeign orgFeign;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private DriverJobFeign driverJobFeign;
    @Autowired
    private TruckFeign truckFeign;
    @Autowired
    private TransportOrderFeign transportOrderFeign;
    @Autowired
    private OrderFeign orderFeign;
    @Autowired
    private AreaFeign areaFeign;
    @Autowired
    private WebManagerFeign webManagerFeign;

    @Operation(summary = "获取运输任务分页数据")
    @PostMapping("/page")
    @SaCheckPermission("task:list")
    public PageResponse<TaskTransportVo> findByPage(@RequestBody TaskTransportVo vo) {
        TaskTransportQueryDTO dto = new TaskTransportQueryDTO();
        if (vo != null) {
            dto.setPage(vo.getPage());
            dto.setPageSize(vo.getPageSize());
            dto.setStatus(vo.getStatus());
            dto.setId(vo.getId());
            dto.setDriverName(vo.getDriverName());
        }
        // 远程调用返回 PageResponse 可能为 null，统一通过 Rx 安全取值，避免 NPE
        PageResponse<TaskTransportDTO> dtoPageResponse = webManagerFeign.findTaskTransportByPage(dto);
        List<TaskTransportDTO> dtoList = Rx.items(dtoPageResponse);
        // 司机信息批量取：一页任务一次 driver-job 查询 + 一次用户查询，
        // 避免逐行组装时产生 N+1 远程调用（详情/更新等单条路径仍走单任务查询）
        Map<Long, List<SysUserVo>> driversByTaskId = BeanUtil.parseTaskDriversBatch(
                dtoList.stream().map(TaskTransportDTO::getId).filter(Objects::nonNull).collect(Collectors.toList()),
                driverJobFeign, userFeign);
        List<TaskTransportVo> voList = dtoList.stream().map(taskTransportDTO -> BeanUtil.parseTaskTransportDTO2Vo(taskTransportDTO, transportTripsFeign, orgFeign, userFeign, truckFeign, transportOrderFeign, orderFeign, areaFeign, driverJobFeign, driversByTaskId)).collect(Collectors.toList());
        return PageResponse.<TaskTransportVo>builder().items(voList).pagesize(vo.getPageSize()).page(vo.getPage())
                .counts(dtoPageResponse != null ? dtoPageResponse.getCounts() : 0L)
                .pages(dtoPageResponse != null ? dtoPageResponse.getPages() : 0L).build();
    }

    @Operation(summary = "获取运输任务详情")
    @GetMapping("/{id}")
    @SaCheckPermission("task:detail")
    public TaskTransportVo findById(@PathVariable(name = "id") String id) {
        TaskTransportDTO dto = transportTaskFeign.findById(Long.valueOf(id));
        TaskTransportVo vo;
        // 说明：任务实时轨迹已由 GPS 模块提供（xfwl-netty /trace/replay），此处返回任务基础信息
        if (dto != null) {
            vo = BeanUtil.parseTaskTransportDTO2Vo(dto, transportTripsFeign, orgFeign, userFeign, truckFeign, transportOrderFeign, orderFeign, areaFeign, driverJobFeign);
        } else {
            vo = new TaskTransportVo();
            vo.setId(id);
        }
        return vo;
    }

    @Operation(summary = "获取运输任务坐标")
    @GetMapping("point/{id}")
    @SaCheckPermission("task:detail")
    public LinkedHashSet<PointDTO> findPointById(@PathVariable(name = "id") String id) {
        LinkedHashSet<PointDTO> pointDTOS = new LinkedHashSet<>();
        TaskTransportDTO dto = transportTaskFeign.findById(Long.valueOf(id));
        if (dto == null) {
            return pointDTOS;
        }
        // 远程调用直接返回 OrgDTO，可能为 null，判空避免 NPE
        OrgDTO startOrg = orgFeign.get(dto.getStartOrgId());
        OrgDTO endOrg = orgFeign.get(dto.getEndOrgId());
        if (startOrg == null || endOrg == null) {
            return pointDTOS;
        }
        PointDTO pointDTO1 = new PointDTO();
        pointDTO1.setName(startOrg.getName());
        pointDTO1.setMarkerPoints(startOrg.getLongitude(), startOrg.getLatitude());
        pointDTOS.add(pointDTO1);
        PointDTO pointDTO2 = new PointDTO();
        pointDTO2.setName(endOrg.getName());
        pointDTO2.setMarkerPoints(endOrg.getLongitude(), endOrg.getLatitude());
        pointDTOS.add(pointDTO2);
        return pointDTOS;
    }

    @Operation(summary = "运输闭环看板聚合数据（DSP-01：六阶段计数+任务/运单状态分布）")
    @GetMapping("/board")
    @SaCheckPermission("task:list")
    public TransportBoardDTO board() {
        // 远程调用可能返回 null，统一空对象防御，前端按空数据处理
        TransportBoardDTO board = webManagerFeign.findTransportBoard();
        return board != null ? board : new TransportBoardDTO();
    }

    @Operation(summary = "更新运输任务")
    @PutMapping("/{id}")
    @SaCheckPermission("task:update")
    public TaskTransportVo update(@PathVariable(name = "id") String id, @RequestBody TaskTransportVo vo) {
        TaskTransportDTO dto = transportTaskFeign.updateById(Long.valueOf(id), BeanUtil.parseTaskTransportVo2DTO(vo));
        return BeanUtil.parseTaskTransportDTO2Vo(dto, transportTripsFeign, orgFeign, userFeign, truckFeign, transportOrderFeign, orderFeign, areaFeign, driverJobFeign);
    }
}
