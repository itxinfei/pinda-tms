package com.itheima.pinda.controller;

import com.itheima.pinda.DTO.AreaDTO;
import com.itheima.pinda.DTO.UserDTO;
import com.itheima.pinda.DTO.angency.AgencyScopeDto;
import com.itheima.pinda.DTO.base.GoodsTypeDto;
import com.itheima.pinda.DTO.truck.TruckTypeDto;
import com.itheima.pinda.DTO.user.CourierScopeDto;
import com.itheima.pinda.common.utils.EntCoordSyncJob;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.constant.StaticStation;
import com.itheima.pinda.feign.AreaFeign;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.feign.UserFeign;
import com.itheima.pinda.feign.agency.AgencyScopeFeign;
import com.itheima.pinda.feign.common.GoodsTypeFeign;
import com.itheima.pinda.feign.truck.TruckTypeFeign;
import com.itheima.pinda.feign.user.CourierScopeFeign;
import com.itheima.pinda.future.PdCompletableFuture;
import com.itheima.pinda.util.BeanUtil;
import com.itheima.pinda.util.Rx;
import com.itheima.pinda.vo.base.AreaSimpleVo;
import com.itheima.pinda.vo.base.angency.AgencyScopeVo;
import com.itheima.pinda.vo.base.businessHall.CourierScopeVo;
import com.itheima.pinda.vo.base.businessHall.GoodsTypeVo;
import com.itheima.pinda.vo.base.transforCenter.business.TruckTypeVo;
import com.itheima.pinda.vo.base.userCenter.SysUserVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 网点管理
 */
@RestController
@RequestMapping("business-hall")
@Tag(name = "网点管理")
@Slf4j
public class BusinessHallController {
    @Autowired
    private GoodsTypeFeign goodsTypeFeign;
    @Autowired
    private TruckTypeFeign truckTypeFeign;
    @Autowired
    private OrgFeign orgFeign;
    @Autowired
    private CourierScopeFeign courierScopeFeign;
    @Autowired
    private AreaFeign areaFeign;
    @Autowired
    private AgencyScopeFeign agencyScopeFeign;
    @Autowired
    private UserFeign userFeign;

    @Operation(summary = "添加货物类型")
    @PostMapping("/goodsType")
    public GoodsTypeVo saveGoodsType(@RequestBody GoodsTypeVo vo) {
        GoodsTypeDto dto = new GoodsTypeDto();
        BeanUtils.copyProperties(vo, dto);
        //处理车辆类型关联数据
        if (vo.getTruckTypes() != null) {
            dto.setTruckTypeIds(vo.getTruckTypes().stream().map(truckTypeVo -> truckTypeVo.getId()).collect(Collectors.toList()));
        }
        GoodsTypeDto resultDto = goodsTypeFeign.saveGoodsType(dto);
        BeanUtils.copyProperties(resultDto, vo);
        return vo;
    }

    @Operation(summary = "更新货物类型")
    @PutMapping("/goodsType/{id}")
    public GoodsTypeVo updateGoodsType(@PathVariable(name = "id") String id, @RequestBody GoodsTypeVo vo) {
        vo.setId(id);
        GoodsTypeDto dto = new GoodsTypeDto();
        BeanUtils.copyProperties(vo, dto);
        //处理车辆类型关联数据
        if (vo.getTruckTypes() != null) {
            dto.setTruckTypeIds(vo.getTruckTypes().stream().map(truckTypeVo -> truckTypeVo.getId()).collect(Collectors.toList()));
        }
        GoodsTypeDto resultDto = goodsTypeFeign.update(id, dto);
        BeanUtils.copyProperties(resultDto, vo);
        return vo;
    }

    @Operation(summary = "获取货物类型分页数据")
    @GetMapping("/goodsType/page")
    public PageResponse<GoodsTypeVo> findGoodsTypeByPage(@RequestParam(name = "page") Integer page,
                                                         @RequestParam(name = "pageSize") Integer pageSize,
                                                         @RequestParam(name = "name", required = false) String name,
                                                         @RequestParam(name = "truckTypeId", required = false) String truckTypeId,
                                                         @RequestParam(name = "truckTypeName", required = false) String truckTypeName) {
        // 远程调用返回 PageResponse 可能为 null，统一通过 Rx 安全取值，避免 NPE
        PageResponse<GoodsTypeDto> goodsTypePage = goodsTypeFeign.findByPage(page, pageSize, name, truckTypeId, truckTypeName);
        //加工数据
        List<GoodsTypeDto> goodsTypeDtoList = Rx.items(goodsTypePage);
        Set<String> truckTypeSet = new HashSet<>();
        goodsTypeDtoList.forEach(goodsTypeDto -> {
            if (goodsTypeDto.getTruckTypeIds() != null) {
                truckTypeSet.addAll(goodsTypeDto.getTruckTypeIds());
            }
        });
        CompletableFuture<Map> truckTypeFuture = PdCompletableFuture.truckTypeMapFuture(truckTypeFeign, truckTypeSet);
        CompletableFuture.allOf(truckTypeFuture).join();
        List<GoodsTypeVo> goodsTypeVoList = goodsTypeDtoList.stream().map(goodsTypeDto -> {
            GoodsTypeVo vo = new GoodsTypeVo();
            BeanUtils.copyProperties(goodsTypeDto, vo);
            try {
                if (goodsTypeDto.getTruckTypeIds() != null) {
                    List<TruckTypeVo> truckTypeVoList = new ArrayList<>();
                    for (String typeId : goodsTypeDto.getTruckTypeIds()) {
                        truckTypeVoList.add((TruckTypeVo) truckTypeFuture.get().get(typeId));
                    }
                    vo.setTruckTypes(truckTypeVoList);
                }
            } catch (Exception e) {
                // 说明：当前为弱关系处理（记录错误并继续），不影响主流程；如改为强关系可在 catch 后返回错误
                log.error("操作异常", e);
            }
            return vo;
        }).collect(Collectors.toList());
        return PageResponse.<GoodsTypeVo>builder().items(goodsTypeVoList).pagesize(pageSize).page(page)
                .counts(goodsTypePage != null ? goodsTypePage.getCounts() : 0L)
                .pages(goodsTypePage != null ? goodsTypePage.getPages() : 0L).build();
    }

    @Operation(summary = "获取货物类型详情")
    @GetMapping("/goodsType/{id}")
    public GoodsTypeVo findGoodsTypeById(@PathVariable(name = "id") String id) {
        GoodsTypeDto dto = goodsTypeFeign.fineById(id);
        GoodsTypeVo vo = new GoodsTypeVo();
        BeanUtils.copyProperties(dto, vo);
        if (dto.getTruckTypeIds() != null && dto.getTruckTypeIds().size() > 0) {
            CompletableFuture<List<TruckTypeDto>> truckTypeFuture = PdCompletableFuture.truckTypeListFuture(truckTypeFeign, dto.getTruckTypeIds());
            CompletableFuture.allOf(truckTypeFuture);
            try {
                vo.setTruckTypes(truckTypeFuture.get().stream().map(truckTypeDto -> {
                    TruckTypeVo truckTypeVo = new TruckTypeVo();
                    BeanUtils.copyProperties(truckTypeDto, truckTypeVo);
                    return truckTypeVo;
                }).collect(Collectors.toList()));
            } catch (Exception e) {
                // 说明：当前为弱关系处理（记录错误并继续），不影响主流程；如改为强关系可在 catch 后返回错误
                log.error("操作异常", e);
            }
        }
        return vo;
    }

    @Operation(summary = "删除货物类型")
    @DeleteMapping("/goodsType/{id}")
    public Result deleteGoodsType(@PathVariable(name = "id") String id) {
        // 说明：货物类型关联校验（已被车辆类型关联时禁止删除）已在 pd-base disable 侧实现
        goodsTypeFeign.disable(id);
        return Result.ok();
    }

    @Operation(summary = "获取快递员分页数据")
    @GetMapping("/courier/page")
    public PageResponse<SysUserVo> findCourierByPage(@RequestParam(name = "page") Integer page,
                                                     @RequestParam(name = "pageSize") Integer pageSize,
                                                     @RequestParam(name = "name", required = false) String name,
                                                     @RequestParam(name = "mobile", required = false) String mobile) {
        // 远程调用返回 PageResponse 可能为 null，统一通过 Rx 安全取值，避免 NPE
        PageResponse<UserDTO> userPage = userFeign.page(page.longValue(), pageSize.longValue(), null, StaticStation.COURIER_ID, name, null, mobile);
        if (userPage != null) {
            //处理对象转换
            List<SysUserVo> voList = Rx.items(userPage).stream().map(user -> BeanUtil.parseUser2Vo(user, null, orgFeign)).collect(Collectors.toList());
            return PageResponse.<SysUserVo>builder().items(voList).page(page).pagesize(pageSize).counts(userPage.getCounts()).pages(userPage.getPages()).build();
        }
        return PageResponse.<SysUserVo>builder().items(new ArrayList<>()).page(page).pagesize(pageSize).counts(0L).pages(0L).build();
    }

    @Operation(summary = "获取快递员详情")
    @GetMapping("/courier/{id}")
    public SysUserVo findCourierById(@PathVariable(name = "id") String id) {
        // 远程调用直接返回 UserDTO，可能为 null，判空避免 NPE
        UserDTO user = userFeign.get(Long.valueOf(id));
        SysUserVo vo = null;
        if (user != null) {
            vo = BeanUtil.parseUser2Vo(user, null, orgFeign);
        }
        return vo;
    }

    @Operation(summary = "保存快递员业务范围")
    @PostMapping("/courier/scope")
    public Result saveCourierScope(@RequestBody CourierScopeVo vo) {
        //验证和处理范围和区域信息
        Result result = validateParam(vo);
        if (!"0".equals(result.get("code").toString())) {
            return result;
        }
        //保存前先清理一遍
        CourierScopeDto deleteDto = new CourierScopeDto();
        deleteDto.setUserId(vo.getCourier().getUserId());
        courierScopeFeign.deleteCourierScope(deleteDto);
        //保存数据
        List<CourierScopeDto> saveList = vo.getAreas().stream().map(areaVo -> {
            CourierScopeDto dto = new CourierScopeDto();
            dto.setAreaId(areaVo.getId());
            dto.setUserId(vo.getCourier().getUserId());
            dto.setMutiPoints(areaVo.getMutiPoints());
            return dto;
        }).collect(Collectors.toList());
        courierScopeFeign.batchSaveCourierScope(saveList);
        return Result.ok();
    }

    /**
     * 验证范围参数设置区域id
     *
     * @param vo
     * @return
     */
    private Result validateParam(CourierScopeVo vo) {
        List<AreaSimpleVo> areas = vo.getAreas();
        if (areas == null || areas.size() == 0) {
            return Result.error(5000, "范围信息为空");
        } else {
            for (AreaSimpleVo areaSimpleVo : areas) {
                String adcodeOld = "";
                AreaDTO area = new AreaDTO();
                //一个区域的多个范围
                List<List<Map>> list = areaSimpleVo.getMutiPoints();
                if (list == null || list.size() == 0) {
                    return Result.error(5000, "范围信息为空");
                } else {
                    for (List<Map> listMap : list) {
                        for(int i=0;i<listMap.size();i++){
                            Map pointMap = listMap.get(i);
                            String point = getPoint(pointMap);
                            Map map = EntCoordSyncJob.getLocationByPosition(point);
                            String adcode = map.getOrDefault("adcode", "").toString();
                            if (StringUtils.isBlank(adcode)) {
                                return Result.error(5000, "根据地图获取区划编码为空");
                            } else {
                                if (!StringUtils.equals(adcode, adcodeOld) && i>0) {
                                    return Result.error(5000, "一个机构作业范围必须在一个区域内");
                                }
                                // 远程调用直接返回 AreaDTO，可能为 null，判空避免 NPE
                                AreaDTO areaByCode = areaFeign.getByCode(adcode + "000000");
                                if (areaByCode != null) {
                                    area = areaByCode;
                                }
                            }
                            adcodeOld = adcode;
                        }

                    }
                }
                areaSimpleVo.setId(area.getId() + "");
                areaSimpleVo.setName(area.getName());
            }

        }
        return Result.ok();
    }

    private String getPoint(Map pointMap) {
        String lng = pointMap.getOrDefault("lng","").toString();
        String lat = pointMap.getOrDefault("lat","").toString();
        return lng+","+lat;
    }

    @Operation(summary = "获取快递员业务范围")
    @GetMapping("/courier/scope/{id}")
    public CourierScopeVo findAllCourierScope(@PathVariable(name = "id") String id) {
        // Feign 直接返回 List 可能为 null，统一通过 Rx 安全取值
        List<CourierScopeDto> courierScopeDtoList = Rx.list(courierScopeFeign.findAllCourierScope(null, id));
        List<Long> areaIds = courierScopeDtoList.stream().map(dto -> Long.valueOf(dto.getAreaId())).collect(Collectors.toList());
        CourierScopeVo vo = new CourierScopeVo();
        // 远程调用直接返回 UserDTO，可能为 null，判空避免 NPE
        UserDTO user = userFeign.get(Long.valueOf(id));
        if (user != null) {
            vo.setCourier(BeanUtil.parseUser2Vo(user, null, null));
        }
        //处理已选列表
        if (areaIds != null && areaIds.size() > 0) {
            // 远程调用直接返回 List，可能为 null，统一通过 Rx 安全取值
            List<AreaDTO> areaDtoList = Rx.list(areaFeign.findAll(null, areaIds));
            List<AreaSimpleVo> areas = areaDtoList.stream().map(BeanUtil::parseArea2Vo).collect(Collectors.toList());
            vo.setAreas(addMutiPoints(areas,courierScopeDtoList));
        }
        return vo;
    }

    private List<AreaSimpleVo> addMutiPoints(List<AreaSimpleVo> areas, List<CourierScopeDto> courierScopeDtoList) {
        for (AreaSimpleVo areaSimpleVo : areas) {
            for (CourierScopeDto courierScopeDto : courierScopeDtoList) {
                if (courierScopeDto.getAreaId().equals(areaSimpleVo.getId())){
                    areaSimpleVo.setMutiPoints(courierScopeDto.getMutiPoints());
                }
            }
        }
        return areas;
    }
}
