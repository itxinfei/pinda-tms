package com.itheima.pinda.future;

import com.itheima.pinda.DTO.OrgDTO;
import com.itheima.pinda.DTO.UserDTO;
import com.itheima.pinda.DTO.angency.FleetDto;
import com.itheima.pinda.DTO.base.GoodsTypeDto;
import com.itheima.pinda.DTO.transportline.TransportLineDto;
import com.itheima.pinda.DTO.transportline.TransportLineTypeDto;
import com.itheima.pinda.DTO.truck.TruckDto;
import com.itheima.pinda.DTO.truck.TruckTypeDto;
import com.itheima.pinda.common.utils.Constant;
import com.itheima.pinda.constant.StaticStation;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.feign.UserFeign;
import com.itheima.pinda.feign.agency.FleetFeign;
import com.itheima.pinda.feign.common.GoodsTypeFeign;
import com.itheima.pinda.feign.transportline.TransportLineFeign;
import com.itheima.pinda.feign.transportline.TransportLineTypeFeign;
import com.itheima.pinda.feign.truck.TruckFeign;
import com.itheima.pinda.feign.truck.TruckTypeFeign;
import com.itheima.pinda.util.BeanUtil;
import com.itheima.pinda.vo.base.angency.AgencySimpleVo;
import com.itheima.pinda.vo.base.angency.AgencyVo;
import com.itheima.pinda.vo.base.businessHall.GoodsTypeVo;
import com.itheima.pinda.vo.base.transforCenter.business.*;
import com.itheima.pinda.vo.base.userCenter.SysUserVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PdCompletableFuture {
    /**
     * 获取map类型用户数据集合
     *
     * @param userFeign 数据接口
     * @param userSet 用户id列表
     * @return 执行结果
     */
    public static final CompletableFuture<Map> userMapFuture(UserFeign userFeign, Set<String> userSet, Integer station, String name, String agencyId) {
        return CompletableFuture.supplyAsync(() -> {
            //查询创建者信息列表
            Long stationId = null;
            if (station != null && station == Constant.UserStation.COURIER.getStation()) {
                stationId = StaticStation.COURIER_ID;
            } else if (station != null && station == Constant.UserStation.DRIVER.getStation()) {
                stationId = StaticStation.DRIVER_ID;
            }
            List<Long> ids = userSet.stream().map(Long::valueOf).collect(Collectors.toList());
            List<UserDTO> userList = userFeign.list(ids, stationId, name, StringUtils.isNotEmpty(agencyId) ? Long.valueOf(agencyId) : null);
            if (userList == null) {
                userList = new ArrayList<>();
            }
            return userList.stream().map(user -> BeanUtil.parseUser2Vo(user, null, null)).collect(Collectors.toMap(SysUserVo::getUserId, vo -> vo));
        });
    }

    /**
     * 获取用户信息
     *
     * @param userFeign 数据接口
     * @param id  用户id
     * @return 执行结果
     */
    public static final CompletableFuture<SysUserVo> userFuture(UserFeign userFeign, String id) {
        return CompletableFuture.supplyAsync(() -> {
            UserDTO user = userFeign.get(Long.valueOf(id));
            if (user == null) {
                return new SysUserVo();
            }
            return BeanUtil.parseUser2Vo(user, null, null);
        });
    }

    /**
     * 获取map类型机构数据集合
     *
     * @param orgFeign  数据接口
     * @param agencySet 机构id列表
     * @return 执行结果
     */
    public static final CompletableFuture<Map> agencyMapFuture(OrgFeign orgFeign, Set<Long> agencySet) {
        return CompletableFuture.supplyAsync(() -> {
            //查询所属机构信息列表
            List<OrgDTO> orgList = orgFeign.list(null, new ArrayList<>(agencySet), null, null, null);
            Map<String, AgencyVo> voMap = new HashMap<>();
            if (orgList != null) {
                orgList.forEach(org -> {
                    AgencyVo agencyVo = new AgencyVo();
                    agencyVo.setId(org.getId() + "");
                    BeanUtils.copyProperties(org, agencyVo);
                    if (!voMap.containsKey(agencyVo.getId())) {
                        voMap.put(agencyVo.getId(), agencyVo);
                    }
                });
            }
            return voMap;
        });
    }

    /**
     * 获取map类型线路类型数据集合
     *
     * @param feign                数据接口
     * @param transportLineTypeSet 线路类型id列表
     * @return 执行结果
     */
    public static final CompletableFuture<Map> transportLineTypeMapFuture(TransportLineTypeFeign feign, Set<String> transportLineTypeSet) {
        return CompletableFuture.supplyAsync(() -> {
            List<TransportLineTypeDto> transportLineTypeDtoList = feign.findAll(new ArrayList<>(transportLineTypeSet));
            return transportLineTypeDtoList.stream().map(dto -> {
                TransportLineTypeVo vo = new TransportLineTypeVo();
                BeanUtils.copyProperties(dto, vo);
                return vo;
            }).collect(Collectors.toMap(TransportLineTypeVo::getId, vo -> vo));
        });
    }

    /**
     * 获取map类型线路数据集合
     *
     * @param feign            数据接口
     * @param transportLineSet 线路id列表
     * @return 执行结果
     */
    public static final CompletableFuture<Map> transportLineMapFuture(TransportLineFeign feign, Set<String> transportLineSet, String agencyId, List<String> agencyIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<TransportLineDto> transportLineDtoList = feign.findAll(new ArrayList<>(transportLineSet), agencyId, agencyIds);
            return transportLineDtoList.stream().map(dto -> {
                TransportLineVo vo = new TransportLineVo();
                BeanUtils.copyProperties(dto, vo);
                return vo;
            }).collect(Collectors.toMap(TransportLineVo::getId, vo -> vo));
        });
    }

    /**
     * 获取map类型车队数据集合
     *
     * @param feign    数据接口
     * @param fleetSet 车队id列表
     * @return 执行结果
     */
    public static final CompletableFuture<Map> fleetMapFuture(FleetFeign feign, Set<String> fleetSet, String agencyId) {
        return CompletableFuture.supplyAsync(() -> {
            //查询所属机构信息列表
            List<FleetDto> fleetList = feign.findAll(new ArrayList<>(fleetSet), agencyId);
            return fleetList.stream().map(fleetDto -> {
                FleetVo vo = new FleetVo();
                BeanUtils.copyProperties(fleetDto, vo);
                return vo;
            }).collect(Collectors.toMap(FleetVo::getId, vo -> vo));
        });
    }

    /**
     * 获取map类型车辆类型数据集合
     *
     * @param feign        数据接口
     * @param truckTypeSet 车辆类型id列表
     * @return 执行结果
     */
    public static final CompletableFuture<Map> truckTypeMapFuture(TruckTypeFeign feign, Set<String> truckTypeSet) {
        return CompletableFuture.supplyAsync(() -> {
            List<TruckTypeDto> truckTypeDtoList = feign.findAll(new ArrayList<>(truckTypeSet));
            return truckTypeDtoList.stream().map(truckTypeDto -> {
                TruckTypeVo vo = new TruckTypeVo();
                BeanUtils.copyProperties(truckTypeDto, vo);
                return vo;
            }).collect(Collectors.toMap(TruckTypeVo::getId, vo -> vo));
        });
    }

    /**
     * 获取map类型车辆数据集合
     *
     * @param feign    数据接口
     * @param truckSet 车辆id列表
     * @return 执行结果
     */
    public static final CompletableFuture<Map> truckMapFuture(TruckFeign feign, Set<String> truckSet, String fleetId) {
        return CompletableFuture.supplyAsync(() -> {
            List<TruckDto> truckDtoList = feign.findAll(new ArrayList<>(truckSet), fleetId);
            return truckDtoList.stream().map(dto -> {
                TruckVo vo = new TruckVo();
                BeanUtils.copyProperties(dto, vo);
                return vo;
            }).collect(Collectors.toMap(TruckVo::getId, vo -> vo));
        });
    }

    /**
     * 获取map类型货物类型数据集合
     *
     * @param feign        数据接口
     * @param goodsTypeSet 货物类型id列表
     * @return 执行结果
     */
    public static final CompletableFuture<Map> goodsTypeMapFuture(GoodsTypeFeign feign, Set<String> goodsTypeSet) {
        return CompletableFuture.supplyAsync(() -> {
            List<GoodsTypeDto> goodsTypeDtoList = feign.findAll(new ArrayList<>(goodsTypeSet));
            return goodsTypeDtoList.stream().map(goodsTypeDto -> {
                GoodsTypeVo vo = new GoodsTypeVo();
                BeanUtils.copyProperties(goodsTypeDto, vo);
                return vo;
            }).collect(Collectors.toMap(GoodsTypeVo::getId, vo -> vo));
        });
    }

    /**
     * 获取车辆类型信息
     *
     * @param feign 数据接口
     * @param id    车辆类型id
     * @return 执行结果
     */
    public static final CompletableFuture<TruckTypeVo> truckTypeFuture(TruckTypeFeign feign, String id) {
        return CompletableFuture.supplyAsync(() -> {
            //查询车辆类型信息
            TruckTypeDto dto = feign.fineById(id);
            TruckTypeVo vo = new TruckTypeVo();
            BeanUtils.copyProperties(dto, vo);
            return vo;
        });
    }

    /**
     * 获取车队信息
     *
     * @param feign 数据接口
     * @param id    车队id
     * @return 执行结果
     */
    public static final CompletableFuture<FleetVo> fleetFuture(FleetFeign feign, String id) {
        return CompletableFuture.supplyAsync(() -> {
            //查询车队信息
            FleetDto dto = feign.fineById(id);
            FleetVo vo = new FleetVo();
            BeanUtils.copyProperties(dto, vo);
            if (StringUtils.isNotEmpty(dto.getAgencyId())) {
                AgencySimpleVo agencyVo = new AgencySimpleVo();
                agencyVo.setId(dto.getAgencyId());
                vo.setAgency(agencyVo);
            }
            return vo;
        });
    }

    /**
     * 获取机构数据列表
     *
     * @param orgFeign   数据接口
     * @param agencyType 机构类型
     * @param ids        机构id列表
     * @return 执行结果
     */
    public static final CompletableFuture<List<OrgDTO>> agencyListFuture(OrgFeign orgFeign, Integer agencyType, List<Long> ids, Long countyId) {
        return CompletableFuture.supplyAsync(() -> {
            List<OrgDTO> list = orgFeign.list(agencyType, ids, countyId, null, null);
            return list != null ? list : new ArrayList<>();
        });
    }

    /**
     * 获取车辆类型列表
     *
     * @param feign 数据接口
     * @param ids   车辆类型id列表
     * @return 执行结果
     */
    public static final CompletableFuture<List<TruckTypeDto>> truckTypeListFuture(TruckTypeFeign feign, List<String> ids) {
        return CompletableFuture.supplyAsync(() -> feign.findAll(ids));
    }

    /**
     * 获取货物类型列表
     *
     * @param feign 数据接口
     * @param ids   货物类型id列表
     * @return 执行结果
     */
    public static final CompletableFuture<List<GoodsTypeDto>> goodsTypeListFuture(GoodsTypeFeign feign, List<String> ids) {
        return CompletableFuture.supplyAsync(() -> feign.findAll(ids));
    }

    /**
     * 获取机构详情
     *
     * @param orgFeign 数据接口
     * @param id  机构id
     * @return 执行结果
     */
//    public static final CompletableFuture<AgencyVo> agencyFuture(OrgFeign orgFeign, Long id) {
//        return CompletableFuture.supplyAsync(() -> {
//            UserDTO result = orgFeign.get(id);
//            AgencySimpleVo agencyVo = new AgencySimpleVo();
//            // 说明：数据处理逻辑待补全
//            return BeanUtil.parseOrg2SimpleVo(result);
//        });
//    }

    /**
     * 获取线路类型信息
     *
     * @param feign 数据接口
     * @param id    线路类型id
     * @return 执行结果
     */
    public static final CompletableFuture<TransportLineTypeVo> transportLineTypeFuture(TransportLineTypeFeign feign, String id) {
        return CompletableFuture.supplyAsync(() -> {
            TransportLineTypeDto dto = feign.fineById(id);
            TransportLineTypeVo vo = new TransportLineTypeVo();
            BeanUtils.copyProperties(dto, vo);
            return vo;
        });
    }

    /**
     * 获取线路信息
     *
     * @param feign 数据接口
     * @param id    线路id
     * @return 执行结果
     */
    public static final CompletableFuture<TransportLineVo> transportLineFuture(TransportLineFeign feign, String id) {
        return CompletableFuture.supplyAsync(() -> {
            TransportLineDto dto = feign.fineById(id);
            TransportLineVo vo = new TransportLineVo();
            BeanUtils.copyProperties(dto, vo);
            return vo;
        });
    }
}
