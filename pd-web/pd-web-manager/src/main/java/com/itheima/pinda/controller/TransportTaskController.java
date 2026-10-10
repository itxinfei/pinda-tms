package com.itheima.pinda.controller;

import com.itheima.pinda.DTO.OrgDTO;
import com.itheima.pinda.DTO.TaskTransportDTO;
import com.itheima.pinda.DTO.webManager.TaskTransportQueryDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.feign.AreaFeign;
import com.itheima.pinda.feign.OrderFeign;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.feign.TransportOrderFeign;
import com.itheima.pinda.feign.TransportTaskFeign;
import com.itheima.pinda.feign.UserFeign;
import com.itheima.pinda.feign.transportline.TransportTripsFeign;
import com.itheima.pinda.feign.truck.TruckFeign;
import com.itheima.pinda.feign.webManager.WebManagerFeign;
import com.itheima.pinda.util.BeanUtil;
import com.itheima.pinda.util.Rx;
import com.itheima.pinda.vo.work.PointDTO;
import com.itheima.pinda.vo.work.TaskTransportVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;


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
        List<TaskTransportVo> voList = dtoList.stream().map(taskTransportDTO -> BeanUtil.parseTaskTransportDTO2Vo(taskTransportDTO, transportTripsFeign, orgFeign, userFeign, truckFeign, transportOrderFeign, orderFeign, areaFeign)).collect(Collectors.toList());
        return PageResponse.<TaskTransportVo>builder().items(voList).pagesize(vo.getPageSize()).page(vo.getPage())
                .counts(dtoPageResponse != null ? dtoPageResponse.getCounts() : 0L)
                .pages(dtoPageResponse != null ? dtoPageResponse.getPages() : 0L).build();
    }

    @Operation(summary = "获取运输任务详情")
    @GetMapping("/{id}")
    public TaskTransportVo findById(@PathVariable(name = "id") String id) {
        TaskTransportDTO dto = transportTaskFeign.findById(Long.valueOf(id));
        TaskTransportVo vo;
        // 说明：任务实时轨迹已由 GPS 模块提供（pd-netty /trace/replay），此处返回任务基础信息
        if (dto != null) {
            vo = BeanUtil.parseTaskTransportDTO2Vo(dto, transportTripsFeign, orgFeign, userFeign, truckFeign, transportOrderFeign, orderFeign, areaFeign);
        } else {
            vo = new TaskTransportVo();
            vo.setId(id);
        }
        return vo;
    }

    @Operation(summary = "获取运输任务坐标")
    @GetMapping("point/{id}")
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

    @Operation(summary = "更新运输任务")
    @PutMapping("/{id}")
    public TaskTransportVo update(@PathVariable(name = "id") String id, @RequestBody TaskTransportVo vo) {
        TaskTransportDTO dto = transportTaskFeign.updateById(Long.valueOf(id), BeanUtil.parseTaskTransportVo2DTO(vo));
        return BeanUtil.parseTaskTransportDTO2Vo(dto, transportTripsFeign, orgFeign, userFeign, truckFeign, transportOrderFeign, orderFeign, areaFeign);
    }
}
