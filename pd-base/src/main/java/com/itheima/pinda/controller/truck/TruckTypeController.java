package com.itheima.pinda.controller.truck;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.pinda.common.utils.Constant;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.entity.truck.PdTruck;
import com.itheima.pinda.entity.truck.PdTruckType;
import com.itheima.pinda.entity.truck.PdTruckTypeGoodsType;
import com.itheima.pinda.service.truck.IPdTruckService;
import com.itheima.pinda.service.truck.IPdTruckTypeGoodsTypeService;
import com.itheima.pinda.service.truck.IPdTruckTypeService;
import com.itheima.pinda.DTO.truck.TruckTypeDto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.beans.BeanUtils;

/**
 * TruckTypeController
 */
@Slf4j
@RestController
@RequestMapping("base/truck/type")
public class TruckTypeController {
    @Autowired
    private IPdTruckTypeService truckTypeService;
    @Autowired
    private IPdTruckTypeGoodsTypeService truckTypeGoodsTypeService;
    @Autowired
    private IPdTruckService truckService;

    /**
     * 添加车辆类型
     *
     * @param dto 车辆类型信息
     * @return 车辆类型信息
     */
    @PostMapping("")
    public TruckTypeDto saveTruckType(@RequestBody TruckTypeDto dto) {
        PdTruckType pdTruckType = new PdTruckType();
        BeanUtils.copyProperties(dto, pdTruckType);
        pdTruckType = truckTypeService.saveTruckTypeWithGoodsTypes(pdTruckType, dto.getGoodsTypeIds());
        BeanUtils.copyProperties(pdTruckType, dto);
        return dto;
    }

    /**
     * 根据id获取车辆类型详情
     *
     * @param id 车辆类型id
     * @return 车辆类型信息
     */
    @GetMapping("/{id}")
    public TruckTypeDto fineById(@PathVariable(name = "id") String id) {
        PdTruckType pdTruckType = truckTypeService.getById(id);
        TruckTypeDto dto = new TruckTypeDto();
        BeanUtils.copyProperties(pdTruckType, dto);
        dto.setGoodsTypeIds(truckTypeGoodsTypeService.findAll(dto.getId(), null).stream().map(pdTruckTypeGoodsType -> pdTruckTypeGoodsType.getGoodsTypeId()).collect(Collectors.toList()));
        return dto;
    }

    /**
     * 获取车辆类型分页数据
     *
     * @param page            页码
     * @param pageSize        页尺寸
     * @param name            车辆类型名称
     * @param allowableLoad   车辆载重
     * @param allowableVolume 车辆体积
     * @return 车辆类型分页数据
     */
    @GetMapping("/page")
    public PageResponse<TruckTypeDto> findByPage(@RequestParam(name = "page") Integer page,
                                                 @RequestParam(name = "pageSize") Integer pageSize,
                                                 @RequestParam(name = "name", required = false) String name,
                                                 @RequestParam(name = "allowableLoad", required = false) BigDecimal allowableLoad,
                                                 @RequestParam(name = "allowableVolume", required = false) BigDecimal allowableVolume) {
        IPage<PdTruckType> pdTruckTypePage = truckTypeService.findByPage(page, pageSize, name, allowableLoad,
                allowableVolume);
        List<TruckTypeDto> dtoList = new ArrayList<>();
        pdTruckTypePage.getRecords().forEach(pdTruckType -> {
            TruckTypeDto dto = new TruckTypeDto();
            BeanUtils.copyProperties(pdTruckType, dto);
            dto.setGoodsTypeIds(truckTypeGoodsTypeService.findAll(dto.getId(), null).stream().map(pdTruckTypeGoodsType -> pdTruckTypeGoodsType.getGoodsTypeId()).collect(Collectors.toList()));
            dtoList.add(dto);
        });
        return PageResponse.<TruckTypeDto>builder().items(dtoList).pagesize(pageSize).page(page)
                .counts(pdTruckTypePage.getTotal()).pages(pdTruckTypePage.getPages()).build();
    }

    /**
     * 获取车辆类型列表
     *
     * @param ids 车辆类型id
     * @return 车辆类型列表
     */
    @GetMapping("")
    public List<TruckTypeDto> findAll(@RequestParam(name = "ids",required = false) List<String> ids) {
        return truckTypeService.findAll(ids).stream().map(truckType -> {
            TruckTypeDto dto = new TruckTypeDto();
            BeanUtils.copyProperties(truckType, dto);
            dto.setGoodsTypeIds(truckTypeGoodsTypeService.findAll(dto.getId(), null).stream().map(pdTruckTypeGoodsType -> pdTruckTypeGoodsType.getGoodsTypeId()).collect(Collectors.toList()));
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 更新车辆类型信息
     *
     * @param id  车辆类型id
     * @param dto 车辆类型信息
     * @return 车辆类型信息
     */
    @PutMapping("/{id}")
    public TruckTypeDto update(@PathVariable(name = "id") String id, @RequestBody TruckTypeDto dto) {
        dto.setId(id);
        PdTruckType truckType = new PdTruckType();
        BeanUtils.copyProperties(dto, truckType);
        truckTypeService.updateTruckTypeWithGoodsTypes(truckType, dto.getGoodsTypeIds());
        return dto;
    }

    /**
     * 删除车辆类型
     *
     * @param id 车辆类型Id
     * @return 返回信息
     */
    @PutMapping("/{id}/disable")
    public Result disable(@PathVariable(name = "id") String id) {
        // 关联校验：存在引用该类型的车辆时禁止删除
        IPage<PdTruck> truckPage = truckService.findByPage(1, 1, id, null, null);
        if (truckPage != null && truckPage.getTotal() > 0) {
            log.warn("[车辆类型] 存在 {} 辆关联车辆，禁止删除: typeId={}", truckPage.getTotal(), id);
            return Result.error(400, "该车辆类型下存在关联车辆，无法删除");
        }
        // 关联校验：存在关联的货物类型时禁止删除
        List<PdTruckTypeGoodsType> goodsTypeRefs = truckTypeGoodsTypeService.findAll(id, null);
        if (goodsTypeRefs != null && !goodsTypeRefs.isEmpty()) {
            log.warn("[车辆类型] 存在关联货物类型，禁止删除: typeId={}", id);
            return Result.error(400, "该车辆类型已关联货物类型，无法删除");
        }
        PdTruckType truckType = new PdTruckType();
        truckType.setId(id);
        truckType.setStatus(Constant.DATA_DISABLE_STATUS);
        truckTypeService.updateById(truckType);
        return Result.ok();
    }

}