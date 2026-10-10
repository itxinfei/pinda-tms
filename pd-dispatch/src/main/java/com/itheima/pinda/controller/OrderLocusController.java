package com.itheima.pinda.controller;

import com.itheima.pinda.DTO.OrderPointDTO;
import com.itheima.pinda.DTO.OrgDTO;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.entity.CacheLineDetailEntity;
import com.itheima.pinda.entity.CacheLineEntity;
import com.itheima.pinda.entity.CacheLineUseEntity;
import com.itheima.pinda.future.PdCompletableFuture;
import com.itheima.pinda.service.ICacheLineDetailService;
import com.itheima.pinda.service.ICacheLineService;
import com.itheima.pinda.service.ICacheLineUseService;
import com.itheima.pinda.service.IOrderClassifyOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 定时任务
 *
 * @author
 */
@RestController
@RequestMapping("/orderLocus")
@Tag(name = "订单轨迹")
@Slf4j
public class OrderLocusController {
    @Autowired
    private OrgFeign orgFeign;
    @Autowired
    private IOrderClassifyOrderService orderClassifyOrderService;
    @Autowired
    private ICacheLineUseService cacheLineUseService;
    @Autowired
    private ICacheLineService cacheLineService;
    @Autowired
    private ICacheLineDetailService cacheLineDetailService;

    @SneakyThrows
    @GetMapping("point/{id}")
    @Operation(summary = "查询订单轨迹经点坐标")
    public LinkedHashSet<OrderPointDTO> findPointByOrderId(@PathVariable("id") String id) {

        LinkedHashSet<OrderPointDTO> OrderPointDTOs = new LinkedHashSet<>();

        String orderClassifyId = orderClassifyOrderService.getClassifyId(id);

        CacheLineUseEntity cacheLineUseEntity = cacheLineUseService.getByOrderClassifyId(orderClassifyId);
        if (cacheLineUseEntity != null) {
            CacheLineEntity cacheLineEntity = cacheLineService.getById(cacheLineUseEntity.getCacheLineId());
            List<CacheLineDetailEntity> cacheLineDetailEntities = cacheLineDetailService.findByCacheLineId(cacheLineUseEntity.getCacheLineId());

            Set<String> agencySet = new HashSet<>();
            agencySet.add(cacheLineEntity.getStartAgencyId());
            agencySet.add(cacheLineEntity.getEndAgencyId());
            agencySet.addAll(cacheLineDetailEntities.stream().map(cacheLineDetailEntity -> cacheLineDetailEntity.getStartAgencyId()).collect(Collectors.toSet()));
            agencySet.addAll(cacheLineDetailEntities.stream().map(cacheLineDetailEntity -> cacheLineDetailEntity.getEndAgencyId()).collect(Collectors.toSet()));

            CompletableFuture<Map<Long, OrgDTO>> agnecyMapFu = PdCompletableFuture.agencyMapFuture(orgFeign, null, agencySet, null);
            Map<Long, OrgDTO> agency = agnecyMapFu.get();

            cacheLineDetailEntities.forEach(cacheLineDetailEntity -> {

                OrgDTO startAgency = agency.get(Long.parseLong(cacheLineDetailEntity.getStartAgencyId()));
                if (startAgency == null) {
                    log.warn("未找到起始机构: {}", cacheLineDetailEntity.getStartAgencyId());
                    return;
                }

                OrgDTO endAgency = agency.get(Long.parseLong(cacheLineDetailEntity.getEndAgencyId()));
                if (endAgency == null) {
                    log.warn("未找到结束机构: {}", cacheLineDetailEntity.getEndAgencyId());
                    return;
                }

                OrderPointDTO orderPoint1 = new OrderPointDTO();
                orderPoint1.setName(startAgency.getName());
                orderPoint1.setMarkerPoints(startAgency.getLongitude(), startAgency.getLatitude());
                OrderPointDTOs.add(orderPoint1);

                OrderPointDTO orderPoint2 = new OrderPointDTO();
                orderPoint2.setName(endAgency.getName());
                orderPoint2.setMarkerPoints(endAgency.getLongitude(), endAgency.getLatitude());
                OrderPointDTOs.add(orderPoint2);
            });
        }

        log.info("订单轨迹-经点坐标：{}", OrderPointDTOs);
        return OrderPointDTOs;
    }
}