package com.itheima.pinda.controller;

import com.itheima.pinda.DTO.UserDTO;
import com.itheima.pinda.DTO.angency.FleetDto;
import com.itheima.pinda.DTO.base.GoodsTypeDto;
import com.itheima.pinda.DTO.transportline.TransportLineTypeDto;
import com.itheima.pinda.DTO.truck.TruckDto;
import com.itheima.pinda.DTO.truck.TruckTypeDto;
import com.itheima.pinda.DTO.user.TruckDriverDto;
import com.itheima.pinda.common.utils.Constant;
import com.itheima.pinda.constant.StaticStation;
import com.itheima.pinda.feign.AreaFeign;
import com.itheima.pinda.feign.UserFeign;
import com.itheima.pinda.feign.agency.FleetFeign;
import com.itheima.pinda.feign.common.GoodsTypeFeign;
import com.itheima.pinda.feign.transportline.TransportLineTypeFeign;
import com.itheima.pinda.feign.truck.TruckFeign;
import com.itheima.pinda.feign.truck.TruckTypeFeign;
import com.itheima.pinda.feign.user.DriverFeign;
import com.itheima.pinda.util.BeanUtil;
import com.itheima.pinda.util.Rx;
import com.itheima.pinda.vo.base.AreaSimpleVo;
import com.itheima.pinda.vo.base.businessHall.GoodsTypeVo;
import com.itheima.pinda.vo.base.transforCenter.business.FleetVo;
import com.itheima.pinda.vo.base.transforCenter.business.TransportLineTypeVo;
import com.itheima.pinda.vo.base.transforCenter.business.TruckTypeVo;
import com.itheima.pinda.vo.base.transforCenter.business.TruckVo;
import com.itheima.pinda.vo.base.userCenter.SysUserVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("common")
@Tag(name = "公共信息")
@Slf4j
public class CommonController {
    @Autowired
    private AreaFeign areaFeign;
    @Autowired
    private FleetFeign fleetFeign;
    @Autowired
    private TruckTypeFeign truckTypeFeign;
    @Autowired
    private TransportLineTypeFeign transportLineTypeFeign;
    @Autowired
    private GoodsTypeFeign goodsTypeFeign;
    @Autowired
    private DriverFeign driverFeign;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private TruckFeign truckFeign;

    @Operation(summary = "获取行政区域简要信息列表")
    @GetMapping(value = "area/simple")
    public List<AreaSimpleVo> areaSimple(@RequestParam(value = "parentId") String parentId) {
        // 远程调用结果可能返回 null，统一通过 Rx 安全取值，避免 NPE
        return Rx.list(areaFeign.findAll(StringUtils.isEmpty(parentId) ? null : Long.valueOf(parentId), null))
                .stream().map(BeanUtil::parseArea2Vo).collect(Collectors.toList());
    }

    @Operation(summary = "获取负责人信息列表")
    @GetMapping(value = "user/simple")
    public List<SysUserVo> userSimple(@RequestParam(name = "station", required = false) Integer station, @RequestParam(name = "name", required = false) String name) {
        // 说明：当前为全局查询；如需按当前用户所属机构过滤，需网关在 token 校验时透传机构头(orgid)，
        // 并在 userFeign.list 中追加 orgId 条件（见 TransforCenterBusinessController 中 RequestContext 用法）
        Long stationId = null;
        if (station != null && station == Constant.UserStation.COURIER.getStation()) {
            stationId = StaticStation.COURIER_ID;
        } else if (station != null && station == Constant.UserStation.DRIVER.getStation()) {
            stationId = StaticStation.DRIVER_ID;
        }
        List<UserDTO> users = userFeign.list(null, stationId, name, null);
        List<SysUserVo> userVoList = new ArrayList<>();
        if (users != null) {
            users.forEach(user -> userVoList.add(BeanUtil.parseUser2Vo(user, null, null)));
        }
        return userVoList;
    }

    @Operation(summary = "获取车队信息列表")
    @GetMapping(value = "fleet/simple")
    public List<FleetVo> fleetSimple(@RequestParam(name = "more", required = false, defaultValue = "false") Boolean more) {
        // 说明：当前为全局查询；如需按当前用户所属机构过滤，需网关透传机构头(orgid)后追加查询条件
        // Feign 直接返回 List 可能为 null，统一通过 Rx 安全取值
        List<FleetDto> fleetDtoList = Rx.list(fleetFeign.findAll(null, null));
        return fleetDtoList.stream().map(fleetDto -> {
            FleetVo simpleVo = new FleetVo();
            BeanUtils.copyProperties(fleetDto, simpleVo);
            if (Boolean.TRUE.equals(more)) {
                List<TruckDto> truckDtoList = truckFeign.findAll(null, fleetDto.getId());
                if (truckDtoList != null && truckDtoList.size() > 0) {
                    simpleVo.setTrucks(truckDtoList.stream().map(truckDto -> {
                        TruckVo truckVo = new TruckVo();
                        BeanUtils.copyProperties(truckDto, truckVo);
                        return truckVo;
                    }).collect(Collectors.toList()));
                }
                List<TruckDriverDto> driverDtoList = driverFeign.findAllDriver(null, fleetDto.getId());
                if (driverDtoList != null && driverDtoList.size() > 0) {
                    List<Long> driverIds = driverDtoList.stream().map(driverDto -> Long.valueOf(driverDto.getUserId())).collect(Collectors.toList());
                    List<UserDTO> users = userFeign.list(driverIds, null, null, null);
                    List<SysUserVo> driverVoList = new ArrayList<>();
                    if (users != null) {
                        users.forEach(user -> driverVoList.add(BeanUtil.parseUser2Vo(user, null, null)));
                    }
                    simpleVo.setDrivers(driverVoList);
                    simpleVo.setDriverCount(driverVoList.size());
                }
            }
            return simpleVo;
        }).collect(Collectors.toList());
    }

    @Operation(summary = "获取车辆类型信息列表")
    @GetMapping(value = "truckType/simple")
    public List<TruckTypeVo> truckTypeSimple() {
        // 车辆类型为全局基础数据，无需按机构过滤；Feign 返回 List 可能为 null，统一通过 Rx 安全取值
        List<TruckTypeDto> truckTypeDtoList = Rx.list(truckTypeFeign.findAll(null));
        return truckTypeDtoList.stream().map(truckTypeDto -> {
            TruckTypeVo simpleVo = new TruckTypeVo();
            BeanUtils.copyProperties(truckTypeDto, simpleVo);
            return simpleVo;
        }).collect(Collectors.toList());
    }

    @Operation(summary = "获取线路类型信息列表")
    @GetMapping(value = "transportLineType/simple")
    public List<TransportLineTypeVo> transportLineTypeSimple() {
        // 线路类型为全局基础数据，无需按机构过滤；Feign 返回 List 可能为 null，统一通过 Rx 安全取值
        List<TransportLineTypeDto> transportLineTypeDtoList = Rx.list(transportLineTypeFeign.findAll(null));
        return transportLineTypeDtoList.stream().map(transportLineTypeDto -> {
            TransportLineTypeVo simpleVo = new TransportLineTypeVo();
            BeanUtils.copyProperties(transportLineTypeDto, simpleVo);
            return simpleVo;
        }).collect(Collectors.toList());
    }

    @Operation(summary = "获取货物类型信息列表")
    @GetMapping(value = "goodsType/simple")
    public List<GoodsTypeVo> goodsTypeSimple() {
        // Feign 直接返回 List 可能为 null，统一通过 Rx 安全取值
        List<GoodsTypeDto> goodsTypeDtoList = Rx.list(goodsTypeFeign.findAll(null));
        return goodsTypeDtoList.stream().map(goodsTypeDto -> {
            GoodsTypeVo simpleVo = new GoodsTypeVo();
            BeanUtils.copyProperties(goodsTypeDto, simpleVo);
            return simpleVo;
        }).collect(Collectors.toList());
    }
}
