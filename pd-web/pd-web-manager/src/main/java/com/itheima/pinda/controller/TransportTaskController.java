package com.itheima.pinda.controller;

import com.itheima.pinda.DTO.TaskTransportDTO;
import com.itheima.pinda.DTO.webManager.TaskTransportQueryDTO;
import com.itheima.pinda.authority.api.AreaApi;
import com.itheima.pinda.authority.api.OrgApi;
import com.itheima.pinda.authority.api.UserApi;
import com.itheima.pinda.authority.entity.core.Org;
import com.itheima.pinda.base.R;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.feign.OrderFeign;
import com.itheima.pinda.feign.TransportOrderFeign;
import com.itheima.pinda.feign.TransportTaskFeign;
import com.itheima.pinda.feign.transportline.TransportTripsFeign;
import com.itheima.pinda.feign.truck.TruckFeign;
import com.itheima.pinda.feign.webManager.WebManagerFeign;
import com.itheima.pinda.util.BeanUtil;
import com.itheima.pinda.util.Rx;
import com.itheima.pinda.vo.work.PointDTO;
import com.itheima.pinda.vo.work.TaskTransportVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
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
@Api(tags = "运输任务API")
@RequestMapping("transport-task-manager")
public class TransportTaskController {
    @Autowired
    private TransportTaskFeign transportTaskFeign;
    @Autowired
    private TransportTripsFeign transportTripsFeign;
    @Autowired
    private OrgApi orgApi;
    @Autowired
    private UserApi userApi;
    @Autowired
    private TruckFeign truckFeign;
    @Autowired
    private TransportOrderFeign transportOrderFeign;
    @Autowired
    private OrderFeign orderFeign;
    @Autowired
    private AreaApi areaApi;
    @Autowired
    private WebManagerFeign webManagerFeign;

    @ApiOperation(value = "获取运输任务分页数据")
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
        // 修改点：远程调用返回 PageResponse 可能为 null，统一通过 Rx 安全取值，避免 NPE
        PageResponse<TaskTransportDTO> dtoPageResponse = webManagerFeign.findTaskTransportByPage(dto);
        List<TaskTransportDTO> dtoList = Rx.items(dtoPageResponse);
        List<TaskTransportVo> voList = dtoList.stream().map(taskTransportDTO -> BeanUtil.parseTaskTransportDTO2Vo(taskTransportDTO, transportTripsFeign, orgApi, userApi, truckFeign, transportOrderFeign, orderFeign, areaApi)).collect(Collectors.toList());
        return PageResponse.<TaskTransportVo>builder().items(voList).pagesize(vo.getPageSize()).page(vo.getPage())
                .counts(dtoPageResponse != null ? dtoPageResponse.getCounts() : 0L)
                .pages(dtoPageResponse != null ? dtoPageResponse.getPages() : 0L).build();
    }

    @ApiOperation(value = "获取运输任务详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "运输任务id", required = true, example = "1", paramType = "{path}")
    })
    @GetMapping("/{id}")
    public TaskTransportVo findById(@PathVariable(name = "id") String id) {
        TaskTransportDTO dto = transportTaskFeign.findById(id);
        TaskTransportVo vo;
        // 说明：任务实时轨迹已由 GPS 模块提供（pd-netty /trace/replay），此处返回任务基础信息
        if (dto != null) {
            vo = BeanUtil.parseTaskTransportDTO2Vo(dto, transportTripsFeign, orgApi, userApi, truckFeign, transportOrderFeign, orderFeign, areaApi);
        } else {
            vo = new TaskTransportVo();
            vo.setId(id);
        }
        return vo;
    }

    @ApiOperation(value = "获取运输任务坐标")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "运输任务id", required = true, example = "1", paramType = "{path}")
    })
    @GetMapping("point/{id}")
    public LinkedHashSet<PointDTO> findPointById(@PathVariable(name = "id") String id) {
        LinkedHashSet<PointDTO> pointDTOS = new LinkedHashSet<>();
        TaskTransportDTO dto = transportTaskFeign.findById(id);
        if (dto == null) {
            return pointDTOS;
        }
        // 修改点：远程调用可能返回 null 包装，统一通过 Rx 安全取值，避免 NPE
        Org startOrg = Rx.data(orgApi.get(Long.parseLong(dto.getStartAgencyId())));
        Org endOrg = Rx.data(orgApi.get(Long.parseLong(dto.getEndAgencyId())));
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

    @ApiOperation(value = "更新运输任务")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "运输任务id", required = true, example = "1", paramType = "{path}")
    })
    @PutMapping("/{id}")
    public TaskTransportVo update(@PathVariable(name = "id") String id, @RequestBody TaskTransportVo vo) {
        TaskTransportDTO dto = transportTaskFeign.updateById(id, BeanUtil.parseTaskTransportVo2DTO(vo));
        return BeanUtil.parseTaskTransportDTO2Vo(dto, transportTripsFeign, orgApi, userApi, truckFeign, transportOrderFeign, orderFeign, areaApi);
    }
}
