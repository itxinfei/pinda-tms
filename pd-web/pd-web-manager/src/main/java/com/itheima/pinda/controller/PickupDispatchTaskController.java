package com.itheima.pinda.controller;

import com.itheima.pinda.DTO.TaskPickupDispatchDTO;
import com.itheima.pinda.DTO.webManager.TaskPickupDispatchQueryDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.feign.AreaFeign;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.feign.OrderFeign;
import com.itheima.pinda.feign.PickupDispatchTaskFeign;
import com.itheima.pinda.feign.UserFeign;
import com.itheima.pinda.feign.webManager.WebManagerFeign;
import com.itheima.pinda.util.BeanUtil;
import com.itheima.pinda.util.Rx;
import com.itheima.pinda.vo.work.TaskPickupDispatchVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 取件、派件任务信息表 前端控制器
 * </p>
 *
 * @author jpf
 * @since 2019-12-29
 */
@Tag(name = "派件、取件任务相关API")
@Slf4j
@RestController
@RequestMapping("pickup-dispatch-task-manager")
public class PickupDispatchTaskController {
    @Autowired
    private OrderFeign orderFeign;
    @Autowired
    private AreaFeign areaFeign;
    @Autowired
    private OrgFeign orgFeign;
    @Autowired
    private PickupDispatchTaskFeign pickupDispatchTaskFeign;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private WebManagerFeign webManagerFeign;

    @Operation(summary = "获取取派件分页数据")
    @PostMapping("/page")
    public PageResponse<TaskPickupDispatchVo> findByPage(@RequestBody TaskPickupDispatchVo vo) {
        TaskPickupDispatchQueryDTO dto = new TaskPickupDispatchQueryDTO();
        if (vo != null) {
            dto.setPage(vo.getPage());
            dto.setPageSize(vo.getPageSize());
            if (vo.getTransportOrder() != null) {
                dto.setTransportOrderId(vo.getTransportOrder().getId());
            }
            if (vo.getCourier() != null) {
                dto.setCourierName(vo.getCourier().getName());
            }
            dto.setTaskType(vo.getTaskType());
            dto.setStatus(vo.getStatus());
            if (vo.getOrder() != null) {
                dto.setSenderName(vo.getOrder().getSenderName());
                if (vo.getOrder().getSenderProvince() != null) {
                    dto.setSenderProvinceId(vo.getOrder().getSenderProvince().getId());
                }
                if (vo.getOrder().getSenderCity() != null) {
                    dto.setSenderCityId(vo.getOrder().getSenderCity().getId());
                }
                dto.setReceiverName(vo.getOrder().getReceiverName());
                if (vo.getOrder().getReceiverProvince() != null) {
                    dto.setReceiverProvinceId(vo.getOrder().getReceiverProvince().getId());
                }
                if (vo.getOrder().getReceiverCity() != null) {
                    dto.setReceiverCityId(vo.getOrder().getReceiverCity().getId());
                }
            }
        }
        // 远程调用返回 PageResponse 可能为 null，统一通过 Rx 安全取值，避免 NPE
        PageResponse<TaskPickupDispatchDTO> dtoPageResponse = webManagerFeign.findTaskPickupDispatchJobByPage(dto);
        List<TaskPickupDispatchDTO> dtoList = Rx.items(dtoPageResponse);
        List<TaskPickupDispatchVo> voList = dtoList.stream().map(taskPickupDispatchDTO -> BeanUtil.parseTaskPickupDispatchDTO2Vo(taskPickupDispatchDTO, orderFeign, areaFeign, orgFeign, userFeign)).collect(Collectors.toList());
        return PageResponse.<TaskPickupDispatchVo>builder().items(voList).pagesize(vo.getPageSize()).page(vo.getPage())
                .counts(dtoPageResponse != null ? dtoPageResponse.getCounts() : 0L)
                .pages(dtoPageResponse != null ? dtoPageResponse.getPages() : 0L).build();
    }

    @Operation(summary = "更新取派件任务")
    @PutMapping("/{id}")
    public TaskPickupDispatchVo update(@PathVariable(name = "id") String id, @RequestBody TaskPickupDispatchVo vo) {
        TaskPickupDispatchDTO dto = pickupDispatchTaskFeign.updateById(Long.valueOf(id), BeanUtil.parseTaskPickupDispatchVo2DTO(vo));
        return BeanUtil.parseTaskPickupDispatchDTO2Vo(dto, orderFeign, areaFeign, orgFeign, userFeign);
    }
}
