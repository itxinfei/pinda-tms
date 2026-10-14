package com.xinfei.controller;
import cn.dev33.satoken.annotation.SaCheckPermission;

import com.xinfei.DTO.DriverJobDTO;
import com.xinfei.DTO.webManager.DriverJobQueryDTO;
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
import com.xinfei.vo.work.DriverJobVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 司机作业单相关API
 *
 * @author itcast
 */
@RestController
@Slf4j
@Tag(name = "司机作业单相关API")
@RequestMapping("driver-job-manager")
public class DriverJobController {
    @Autowired
    private WebManagerFeign webManagerFeign;
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

    @Operation(summary = "获取司机作业单分页数据")
    @PostMapping("/page")
    @SaCheckPermission("driverJob:list")
    public PageResponse<DriverJobVo> findByPage(@RequestBody DriverJobVo vo) {
        DriverJobQueryDTO dto = new DriverJobQueryDTO();
        if (vo != null) {
            dto.setPage(vo.getPage());
            dto.setPageSize(vo.getPageSize());
            if (vo.getDriver() != null) {
                dto.setDriverName(vo.getDriver().getName());
            }
            if (vo.getTaskTransport() != null) {
                dto.setTaskTransportId(vo.getTaskTransport().getId());
            }
            dto.setStatus(vo.getStatus());
            dto.setId(vo.getId());
        }
        PageResponse<DriverJobDTO> dtoPageResponse = webManagerFeign.findDriverJobByPage(dto);
        // 远程调用返回 PageResponse 可能为 null，先判空避免 NPE
        if (dtoPageResponse == null) {
            return PageResponse.<DriverJobVo>builder().items(new ArrayList<>()).pagesize(vo.getPageSize()).page(vo.getPage()).counts(0L).pages(0L).build();
        }
        List<DriverJobDTO> dtoList = dtoPageResponse.getItems();
        if (CollectionUtils.isEmpty(dtoList)) {
            return PageResponse.<DriverJobVo>builder().items(new ArrayList<>()).pagesize(vo.getPageSize()).page(vo.getPage()).counts(0L).pages(0L).build();
        }
        List<DriverJobVo> voList = dtoList.stream().map(driverJobDTO -> BeanUtil.parseDriverJobDTO2Vo(driverJobDTO, transportTripsFeign, orgFeign, userFeign, truckFeign, transportOrderFeign, orderFeign, areaFeign, transportTaskFeign)).collect(Collectors.toList());
        return PageResponse.<DriverJobVo>builder().items(voList).pagesize(vo.getPageSize()).page(vo.getPage()).counts(dtoPageResponse.getCounts()).pages(dtoPageResponse.getPages()).build();
    }
}
