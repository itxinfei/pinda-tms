package com.itheima.pinda.controller;

import com.itheima.pinda.DTO.TaskPickupDispatchDTO;
import com.itheima.pinda.DTO.TaskTransportDTO;
import com.itheima.pinda.DTO.TransportOrderDTO;
import com.itheima.pinda.DTO.webManager.TransportOrderQueryDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.enums.pickuptask.PickupDispatchTaskType;
import com.itheima.pinda.feign.AreaFeign;
import com.itheima.pinda.feign.OrderFeign;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.feign.PickupDispatchTaskFeign;
import com.itheima.pinda.feign.TransportOrderFeign;
import com.itheima.pinda.feign.TransportTaskFeign;
import com.itheima.pinda.feign.UserFeign;
import com.itheima.pinda.feign.transportline.TransportTripsFeign;
import com.itheima.pinda.feign.truck.TruckFeign;
import com.itheima.pinda.feign.webManager.WebManagerFeign;
import com.itheima.pinda.util.BeanUtil;
import com.itheima.pinda.util.Rx;
import com.itheima.pinda.vo.work.TaskTransportVo;
import com.itheima.pinda.vo.work.TransportOrderVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 运单表 前端控制器
 * </p>
 *
 * @author jpf
 * @since 2020-01-06
 */
@Slf4j
@Tag(name = "运单相关Api")
@RestController
@RequestMapping("transport-order-manager")
public class TransportOrderController {
    @Autowired
    private TransportOrderFeign transportOrderFeign;
    @Autowired
    private OrderFeign orderFeign;
    @Autowired
    private AreaFeign areaFeign;
    @Autowired
    private PickupDispatchTaskFeign pickupDispatchTaskFeign;
    @Autowired
    private OrgFeign orgFeign;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private TransportTaskFeign transportTaskFeign;
    @Autowired
    private TransportTripsFeign transportTripsFeign;
    @Autowired
    private TruckFeign truckFeign;
    @Autowired
    private WebManagerFeign webManagerFeign;

    @Operation(summary = "获取运单分页数据")
    @PostMapping("/page")
    public PageResponse<TransportOrderVo> findByPage(@RequestBody TransportOrderVo vo) {
        TransportOrderQueryDTO dto = new TransportOrderQueryDTO();
        if (vo != null) {
            dto.setPage(vo.getPage());
            dto.setPageSize(vo.getPageSize());
            dto.setStatus(vo.getStatus());
            dto.setId(vo.getId());
            if (vo.getOrder() != null) {
                dto.setSenderName(vo.getOrder().getSenderName());
                dto.setSenderPhone(vo.getOrder().getSenderPhone());
                if (vo.getOrder().getSenderProvince() != null) {
                    dto.setSenderProvinceId(vo.getOrder().getSenderProvince().getId());
                }
                if (vo.getOrder().getSenderCity() != null) {
                    dto.setSenderCityId(vo.getOrder().getSenderCity().getId());
                }
                if (vo.getOrder().getSenderCounty() != null) {
                    dto.setSenderCountyId(vo.getOrder().getSenderCounty().getId());
                }
                dto.setReceiverName(vo.getOrder().getReceiverName());
                dto.setReceiverPhone(vo.getOrder().getReceiverPhone());
                if (vo.getOrder().getReceiverProvince() != null) {
                    dto.setReceiverProvinceId(vo.getOrder().getReceiverProvince().getId());
                }
                if (vo.getOrder().getReceiverCity() != null) {
                    dto.setReceiverCityId(vo.getOrder().getReceiverCity().getId());
                }
                if (vo.getOrder().getReceiverCounty() != null) {
                    dto.setReceiverCountyId(vo.getOrder().getReceiverCounty().getId());
                }
            }
        }
        // 远程调用返回 PageResponse 可能为 null，统一通过 Rx 安全取值，避免 NPE
        PageResponse<TransportOrderDTO> dtoPageResponse = webManagerFeign.findTransportOrderByPage(dto);
        List<TransportOrderDTO> dtoList = Rx.items(dtoPageResponse);
        List<TransportOrderVo> voList = dtoList.stream().map(transportOrderDTO -> BeanUtil.parseTransportOrderDTO2Vo(transportOrderDTO, orderFeign, areaFeign)).collect(Collectors.toList());
        return PageResponse.<TransportOrderVo>builder().items(voList).pagesize(vo.getPageSize()).page(vo.getPage())
                .counts(dtoPageResponse != null ? dtoPageResponse.getCounts() : 0L)
                .pages(dtoPageResponse != null ? dtoPageResponse.getPages() : 0L).build();
    }

    @Operation(summary = "获取运单详情")
    @GetMapping("/{id}")
    public TransportOrderVo findById(@PathVariable(name = "id") String id) {
        // 远程查询可能返回 null，先判空避免后续转换 NPE
        TransportOrderDTO dto = transportOrderFeign.findById(Long.valueOf(id));
        if (dto == null) {
            TransportOrderVo empty = new TransportOrderVo();
            empty.setId(id);
            return empty;
        }
        TransportOrderVo vo = BeanUtil.parseTransportOrderDTO2Vo(dto, orderFeign, areaFeign);
        if (vo.getOrder() != null && StringUtils.isNotEmpty(vo.getOrder().getId())) {
            //查询取派件任务信息
            TaskPickupDispatchDTO taskPickupDispatchQueryDTO = new TaskPickupDispatchDTO();
            taskPickupDispatchQueryDTO.setOrderId(Long.valueOf(vo.getOrder().getId()));
            List<TaskPickupDispatchDTO> taskPickupDispatchDTOList = pickupDispatchTaskFeign.findAll(taskPickupDispatchQueryDTO);
            if (taskPickupDispatchDTOList != null && taskPickupDispatchDTOList.size() > 0) {
                taskPickupDispatchDTOList.forEach(taskPickupDispatchDTO -> {
                    if (String.valueOf(taskPickupDispatchDTO.getOrderId()).equals(vo.getOrder().getId()) && taskPickupDispatchDTO.getTaskType() == PickupDispatchTaskType.PICKUP.getCode()) {
                        //取件信息
                        vo.setTaskPickup(BeanUtil.parseTaskPickupDispatchDTO2Vo(taskPickupDispatchDTO, orderFeign, areaFeign, orgFeign, userFeign));
                    }
                    if (String.valueOf(taskPickupDispatchDTO.getOrderId()).equals(vo.getOrder().getId()) && taskPickupDispatchDTO.getTaskType() == PickupDispatchTaskType.DISPATCH.getCode()) {
                        //派件信息
                        vo.setTaskDispatch(BeanUtil.parseTaskPickupDispatchDTO2Vo(taskPickupDispatchDTO, orderFeign, areaFeign, orgFeign, userFeign));
                    }
                });
            }
        }
        //获取运输信息
        List<TaskTransportVo> taskTransportVoList = new ArrayList<>();
        List<TaskTransportDTO> taskTransportDTOList = transportTaskFeign.findAllByOrderIdOrTaskId(Long.valueOf(vo.getId()), null);
        if (taskTransportDTOList != null) {
            taskTransportDTOList.forEach(taskTransportDTO -> taskTransportVoList.add(BeanUtil.parseTaskTransportDTO2Vo(taskTransportDTO, transportTripsFeign, orgFeign, userFeign, truckFeign, transportOrderFeign, orderFeign, areaFeign)));
        }
        vo.setTaskTransports(taskTransportVoList);
        return vo;
    }
}
