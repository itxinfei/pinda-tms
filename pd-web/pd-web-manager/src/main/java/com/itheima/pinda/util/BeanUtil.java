package com.itheima.pinda.util;

import com.itheima.pinda.DTO.*;
import com.itheima.pinda.DTO.angency.FleetDto;
import com.itheima.pinda.DTO.transportline.TransportTripsDto;
import com.itheima.pinda.DTO.truck.TruckDto;
import com.itheima.pinda.DTO.user.TruckDriverDto;
import com.itheima.pinda.common.utils.Constant;
import com.itheima.pinda.constant.StaticStation;
import com.itheima.pinda.enums.org.OrgType;
import com.itheima.pinda.feign.AreaFeign;
import com.itheima.pinda.feign.OrderFeign;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.feign.RoleFeign;
import com.itheima.pinda.feign.TransportOrderFeign;
import com.itheima.pinda.feign.TransportTaskFeign;
import com.itheima.pinda.feign.UserFeign;
import com.itheima.pinda.feign.agency.FleetFeign;
import com.itheima.pinda.feign.transportline.TransportTripsFeign;
import com.itheima.pinda.feign.truck.TruckFeign;
import com.itheima.pinda.vo.base.AreaSimpleVo;
import com.itheima.pinda.vo.base.angency.AgencySimpleVo;
import com.itheima.pinda.vo.base.angency.AgencyVo;
import com.itheima.pinda.vo.base.angency.RoleVo;
import com.itheima.pinda.vo.base.transforCenter.business.DriverVo;
import com.itheima.pinda.vo.base.transforCenter.business.FleetVo;
import com.itheima.pinda.vo.base.transforCenter.business.TransportTripsVo;
import com.itheima.pinda.vo.base.transforCenter.business.TruckVo;
import com.itheima.pinda.vo.base.userCenter.SysUserVo;
import com.itheima.pinda.vo.oms.OrderVo;
import com.itheima.pinda.vo.work.DriverJobVo;
import com.itheima.pinda.vo.work.TaskPickupDispatchVo;
import com.itheima.pinda.vo.work.TaskTransportVo;
import com.itheima.pinda.vo.work.TransportOrderVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class BeanUtil {
    public static SysUserVo parseUser2Vo(UserDTO user, RoleFeign roleFeign, OrgFeign orgFeign) {
        SysUserVo vo = new SysUserVo();
        //填充基本信息
        vo.setUserId(String.valueOf(user.getId()));
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setMobile(user.getMobile());
        vo.setUsername(user.getAccount());
        vo.setName(user.getName());
        // 员工编号：EMP + 用户ID（左补零至至少8位，保留完整ID，唯一）
        vo.setWorkNumber(generateWorkNumber(user));
        //处理角色信息
        if (roleFeign != null) {
            List<RoleDTO> roleList = roleFeign.list(user.getId());
            List<RoleVo> roles = new ArrayList<>();
            if (roleList != null) {
                roleList.forEach(role -> roles.add(parseRole2Vo(role)));
            }
            vo.setRoles(roles);
        }
        //处理所属机构信息
        if (orgFeign != null && user.getOrgId() != null && user.getOrgId() != 0) {
            OrgDTO org = orgFeign.get(user.getOrgId());
            if (org != null) {
                vo.setAgency(parseOrg2SimpleVo(org));
            }
        }
        //处理岗位信息
        if (user.getStationId() != null && user.getStationId() != 0) {
            if (user.getStationId() == StaticStation.COURIER_ID) {
                vo.setStation(Constant.UserStation.COURIER.getStation());
                vo.setStationName(Constant.UserStation.COURIER.getName());
            } else if (user.getStationId() == StaticStation.DRIVER_ID) {
                vo.setStation(Constant.UserStation.DRIVER.getStation());
                vo.setStationName(Constant.UserStation.DRIVER.getName());
            } else {
                vo.setStation(Constant.UserStation.PERSONNEL.getStation());
                vo.setStationName(Constant.UserStation.PERSONNEL.getName());
            }
        }
        return vo;
    }

    /**
     * 角色数据模型转换
     *
     * @param role
     * @return
     */
    public static RoleVo parseRole2Vo(RoleDTO role) {
        RoleVo vo = new RoleVo();
        vo.setId(String.valueOf(role.getId()));
        vo.setName(role.getName());
        return vo;
    }


    /**
     * 机构数据模型转换
     *
     * @param org
     * @return
     */
    public static AgencyVo parseOrg2Vo(OrgDTO org, OrgFeign orgFeign, AreaFeign areaFeign) {
        AgencyVo agencyVo = new AgencyVo();
        agencyVo.setId(org.getId() + "");
        agencyVo.setName(org.getName());
        if (org.getOrgType() != null) {
            agencyVo.setAgencyType(org.getOrgType());
            agencyVo.setAgencyTypeName(OrgType.getEnumByType(org.getOrgType()).getName());
        }
        agencyVo.setAddress(org.getAddress());
        agencyVo.setLongitude(org.getLongitude());
        agencyVo.setLatitude(org.getLatitude());
        agencyVo.setContractNumber(org.getContractNumber());
        agencyVo.setStatus(Boolean.TRUE.equals(org.getStatus()) ? 0 : 1);
        // 负责人信息：由 org.manager 名称承载（如需完整用户对象可再经 userFeign 查询）
        if (org.getManager() != null) {
            SysUserVo managerVo = new SysUserVo();
            managerVo.setName(org.getManager());
            agencyVo.setManager(managerVo);
        }
        //处理父级信息
        if (org.getParentId() != null && org.getParentId() != 0 && orgFeign != null) {
            OrgDTO parent = orgFeign.get(org.getParentId());
            if (parent != null && parent.getId() != null) {
                AgencySimpleVo simpleVo = new AgencySimpleVo();
                BeanUtils.copyProperties(parent, simpleVo);
                simpleVo.setId(String.valueOf(parent.getId()));
                agencyVo.setParent(simpleVo);
            }
        }
        //处理省市区信息
        Set<Long> areaIds = new HashSet<>();
        boolean provinceOk = org.getProvinceId() != null && org.getProvinceId() != 0;
        boolean cityOk = org.getCityId() != null && org.getCityId() != 0;
        boolean countyOk = org.getCountyId() != null && org.getCountyId() != 0;
        if (provinceOk) {
            areaIds.add(org.getProvinceId());
        }
        if (cityOk) {
            areaIds.add(org.getCityId());
        }
        if (countyOk) {
            areaIds.add(org.getCountyId());
        }
        if (areaIds.size() > 0 && areaFeign != null) {
            List<AreaDTO> areas = areaFeign.findAll(null, new ArrayList<>(areaIds));
            if (areas != null) {
                Map<Long, AreaDTO> areaMap = areas.stream().collect(Collectors.toMap(AreaDTO::getId, area -> area));
                if (provinceOk) {
                    agencyVo.setProvince(parseArea2Vo(areaMap.get(org.getProvinceId())));
                }
                if (cityOk) {
                    agencyVo.setCity(parseArea2Vo(areaMap.get(org.getCityId())));
                }
                if (countyOk) {
                    agencyVo.setCounty(parseArea2Vo(areaMap.get(org.getCountyId())));
                }
            }
        }
        return agencyVo;
    }

    public static AgencySimpleVo parseOrg2SimpleVo(OrgDTO org) {
        AgencySimpleVo vo = new AgencySimpleVo();
        vo.setId(String.valueOf(org.getId()));
        vo.setName(org.getName());
        return vo;
    }

    public static AreaSimpleVo parseArea2Vo(AreaDTO area) {
        AreaSimpleVo vo = new AreaSimpleVo();
        if (area != null && area.getId() != null) {
            BeanUtils.copyProperties(area, vo);
            vo.setId(String.valueOf(area.getId()));
        }
        return vo;
    }

    public static OrderDTO parseOrderVo2DTO(OrderVo vo) {
        OrderDTO dto = new OrderDTO();
        BeanUtils.copyProperties(vo, dto);
        if (vo.getSenderProvince() != null) {
            dto.setSenderProvinceId(vo.getSenderProvince().getId());
        }
        if (vo.getSenderCity() != null) {
            dto.setSenderCityId(vo.getSenderCity().getId());
        }
        if (vo.getSenderCounty() != null) {
            dto.setSenderCountyId(vo.getSenderCounty().getId());
        }
        if (vo.getReceiverProvince() != null) {
            dto.setReceiverProvinceId(vo.getReceiverProvince().getId());
        }
        if (vo.getReceiverCity() != null) {
            dto.setReceiverCityId(vo.getReceiverCity().getId());
        }
        if (vo.getReceiverCounty() != null) {
            dto.setReceiverCountyId(vo.getReceiverCounty().getId());
        }
        return dto;
    }

    public static OrderVo parseOrderDTO2Vo(OrderDTO dto, AreaFeign areaFeign) {
        OrderVo vo = new OrderVo();
        BeanUtils.copyProperties(dto, vo);
        if (dto.getSenderProvinceId() != null && areaFeign != null) {
            AreaDTO area = areaFeign.get(Long.valueOf(dto.getSenderProvinceId()));
            if (area != null) {
                vo.setSenderProvince(parseArea2Vo(area));
            }
        }
        if (dto.getSenderCityId() != null && areaFeign != null) {
            AreaDTO area = areaFeign.get(Long.valueOf(dto.getSenderCityId()));
            if (area != null) {
                vo.setSenderCity(parseArea2Vo(area));
            }
        }
        if (dto.getSenderCountyId() != null && areaFeign != null) {
            AreaDTO area = areaFeign.get(Long.valueOf(dto.getSenderCountyId()));
            if (area != null) {
                vo.setSenderCounty(parseArea2Vo(area));
            }
        }
        if (dto.getReceiverProvinceId() != null && areaFeign != null) {
            AreaDTO area = areaFeign.get(Long.valueOf(dto.getReceiverProvinceId()));
            if (area != null) {
                vo.setReceiverProvince(parseArea2Vo(area));
            }
        }
        if (dto.getReceiverCityId() != null && areaFeign != null) {
            AreaDTO area = areaFeign.get(Long.valueOf(dto.getReceiverCityId()));
            if (area != null) {
                vo.setReceiverCity(parseArea2Vo(area));
            }
        }
        if (dto.getReceiverCountyId() != null && areaFeign != null) {
            AreaDTO area = areaFeign.get(Long.valueOf(dto.getReceiverCountyId()));
            if (area != null) {
                vo.setReceiverCounty(parseArea2Vo(area));
            }
        }
        return vo;
    }

    /**
     * VO 面向浏览器，雪花 ID 超过 JS 安全整数上限必须保持 String；
     * 以下三个方法为 work DTO(Long) 与 VO(String) 在调用边界的转换工具。
     */
    private static Long toLong(String id) {
        return StringUtils.isBlank(id) ? null : Long.parseLong(id);
    }

    private static String toStr(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    private static String toStr(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private static BigDecimal toBigDecimal(String value) {
        return StringUtils.isBlank(value) ? null : new BigDecimal(value.trim());
    }

    public static TaskPickupDispatchDTO parseTaskPickupDispatchVo2DTO(TaskPickupDispatchVo vo) {
        TaskPickupDispatchDTO dto = new TaskPickupDispatchDTO();
        BeanUtils.copyProperties(vo, dto);
        dto.setId(toLong(vo.getId()));
        if (vo.getAgency() != null) {
            dto.setOrgId(toLong(vo.getAgency().getId()));
        }
        if (vo.getCourier() != null) {
            dto.setCourierId(toLong(vo.getCourier().getUserId()));
        }
        if (vo.getOrder() != null) {
            dto.setOrderId(toLong(vo.getOrder().getId()));
            if (vo.getOrder().getSenderProvince() != null) {
                dto.setSenderProvinceId(vo.getOrder().getSenderProvince().getId());
            }
            if (vo.getOrder().getSenderCity() != null) {
                dto.setSenderCityId(vo.getOrder().getSenderCity().getId());
            }
            if (StringUtils.isNotEmpty(vo.getOrder().getSenderName())) {
                dto.setSenderName(vo.getOrder().getSenderName());
            }
            if (vo.getOrder().getReceiverProvince() != null) {
                dto.setReceiverProvinceId(vo.getOrder().getReceiverProvince().getId());
            }
            if (vo.getOrder().getReceiverCity() != null) {
                dto.setReceiverCityId(vo.getOrder().getReceiverCity().getId());
            }
            if (StringUtils.isNotEmpty(vo.getOrder().getReceiverName())) {
                dto.setReceiverName(vo.getOrder().getReceiverName());
            }
        }
        return dto;
    }

    public static TaskPickupDispatchVo parseTaskPickupDispatchDTO2Vo(TaskPickupDispatchDTO dto, OrderFeign orderFeign, AreaFeign areaFeign, OrgFeign orgFeign, UserFeign userFeign) {
        TaskPickupDispatchVo vo = new TaskPickupDispatchVo();
        BeanUtils.copyProperties(dto, vo);
        vo.setId(toStr(dto.getId()));
        if (dto.getOrderId() != null && orderFeign != null) {
            OrderDTO orderDTO = orderFeign.findById(toStr(dto.getOrderId()));
            if (orderDTO != null) {
                vo.setOrder(parseOrderDTO2Vo(orderDTO, areaFeign));
            }
        }
        if (dto.getOrgId() != null && orgFeign != null) {
            OrgDTO org = orgFeign.get(dto.getOrgId());
            if (org != null) {
                vo.setAgency(parseOrg2SimpleVo(org));
            }
        }
        if (dto.getCourierId() != null && userFeign != null) {
            UserDTO user = userFeign.get(dto.getCourierId());
            if (user != null) {
                vo.setCourier(parseUser2Vo(user, null, null));
            }
        }
        return vo;
    }

    public static TransportOrderDTO parseTransportOrderVo2DTO(TransportOrderVo vo) {
        TransportOrderDTO dto = new TransportOrderDTO();
        BeanUtils.copyProperties(vo, dto);
        dto.setId(toLong(vo.getId()));
        if (vo.getOrder() != null) {
            dto.setOrderId(toLong(vo.getOrder().getId()));
        }
        return dto;
    }

    public static TransportOrderVo parseTransportOrderDTO2Vo(TransportOrderDTO dto, OrderFeign orderFeign, AreaFeign areaFeign) {
        TransportOrderVo vo = new TransportOrderVo();
        BeanUtils.copyProperties(dto, vo);
        vo.setId(toStr(dto.getId()));
        if (dto.getOrderId() != null && orderFeign != null) {
            OrderDTO orderDTO = orderFeign.findById(toStr(dto.getOrderId()));
            if (orderDTO != null) {
                vo.setOrder(parseOrderDTO2Vo(orderDTO, areaFeign));
            }
        }
        return vo;
    }

    public static TaskTransportDTO parseTaskTransportVo2DTO(TaskTransportVo vo) {
        TaskTransportDTO dto = new TaskTransportDTO();
        BeanUtils.copyProperties(vo, dto);
        dto.setId(toLong(vo.getId()));
        if (vo.getTransportTrips() != null) {
            dto.setTripsId(toLong(vo.getTransportTrips().getId()));
        }
        if (vo.getStartAgency() != null) {
            dto.setStartOrgId(toLong(vo.getStartAgency().getId()));
        }
        if (vo.getEndAgency() != null) {
            dto.setEndOrgId(toLong(vo.getEndAgency().getId()));
        }
        if (vo.getTruck() != null) {
            dto.setTruckId(toLong(vo.getTruck().getId()));
        }
        // 旧 VO 字段名对齐 work DTO 新字段名
        dto.setPickupPicture(vo.getCargoPickUpPicture());
        dto.setCertificatePicture(vo.getTransportCertificate());
        dto.setPlanPickUpTime(vo.getPlanPickUpGoodsTime());
        dto.setActualPickUpTime(vo.getActualPickUpGoodsTime());
        dto.setPickupLatitude(toBigDecimal(vo.getDeliveryLatitude()));
        dto.setPickupLongitude(toBigDecimal(vo.getDeliveryLongitude()));
        dto.setDeliverLatitude(toBigDecimal(vo.getDeliverLatitude()));
        dto.setDeliverLongitude(toBigDecimal(vo.getDeliverLongitude()));
        if (vo.getTransportOrders() != null && !vo.getTransportOrders().isEmpty()) {
            dto.setTransportOrderIds(vo.getTransportOrders().stream()
                    .filter(transOrder -> StringUtils.isNotEmpty(transOrder.getId()))
                    .map(transOrder -> toLong(transOrder.getId()))
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public static TaskTransportVo parseTaskTransportDTO2Vo(TaskTransportDTO dto, TransportTripsFeign transportTripsFeign, OrgFeign orgFeign, UserFeign userFeign, TruckFeign truckFeign, TransportOrderFeign transportOrderFeign, OrderFeign orderFeign, AreaFeign areaFeign) {
        TaskTransportVo vo = new TaskTransportVo();
        BeanUtils.copyProperties(dto, vo);
        vo.setId(toStr(dto.getId()));
        if (dto.getTripsId() != null && transportTripsFeign != null) {
            TransportTripsDto transportTripsDto = transportTripsFeign.fineById(toStr(dto.getTripsId()));
            if (transportTripsDto != null) {
                TransportTripsVo transportTripsVo = new TransportTripsVo();
                BeanUtils.copyProperties(transportTripsDto, transportTripsVo);
                vo.setTransportTrips(transportTripsVo);
            }
        }
        if (dto.getStartOrgId() != null && orgFeign != null) {
            OrgDTO org = orgFeign.get(dto.getStartOrgId());
            if (org != null) {
                vo.setStartAgency(parseOrg2SimpleVo(org));
            }
        }
        if (dto.getEndOrgId() != null && orgFeign != null) {
            OrgDTO org = orgFeign.get(dto.getEndOrgId());
            if (org != null) {
                vo.setEndAgency(parseOrg2SimpleVo(org));
            }
        }
        if (dto.getTruckId() != null && truckFeign != null) {
            TruckDto truckDto = truckFeign.fineById(toStr(dto.getTruckId()));
            if (truckDto != null) {
                TruckVo truckVo = new TruckVo();
                BeanUtils.copyProperties(truckDto, truckVo);
                vo.setTruck(truckVo);
            }
        }
        // work DTO 新字段名回填旧 VO 字段名
        vo.setCargoPickUpPicture(dto.getPickupPicture());
        vo.setTransportCertificate(dto.getCertificatePicture());
        vo.setPlanPickUpGoodsTime(dto.getPlanPickUpTime());
        vo.setActualPickUpGoodsTime(dto.getActualPickUpTime());
        vo.setDeliveryLatitude(toStr(dto.getPickupLatitude()));
        vo.setDeliveryLongitude(toStr(dto.getPickupLongitude()));
        vo.setDeliverLatitude(toStr(dto.getDeliverLatitude()));
        vo.setDeliverLongitude(toStr(dto.getDeliverLongitude()));
        List<TransportOrderVo> transportOrderVoList = new ArrayList<>();
        if (dto.getTransportOrderIds() != null && !dto.getTransportOrderIds().isEmpty() && transportOrderFeign != null) {
            dto.getTransportOrderIds().forEach(transportOrderId -> {
                if (transportOrderId != null) {
                    TransportOrderDTO transportOrderDTO = transportOrderFeign.findById(transportOrderId);
                    if (transportOrderDTO != null) {
                        transportOrderVoList.add(parseTransportOrderDTO2Vo(transportOrderDTO, orderFeign, areaFeign));
                    }
                }
            });
        }
        vo.setTransportOrders(transportOrderVoList);
        // 说明：司机信息由 parseTruckDriverDto2Vo / parseDriverJobVo2DTO 等转换方法承载，此处不再重复组装
        return vo;
    }

    public static DriverJobDTO parseDriverJobVo2DTO(DriverJobVo vo) {
        DriverJobDTO dto = new DriverJobDTO();
        BeanUtils.copyProperties(vo, dto);
        dto.setId(toLong(vo.getId()));
        if (vo.getStartAgency() != null) {
            dto.setStartOrgId(toLong(vo.getStartAgency().getId()));
        }
        if (vo.getEndAgency() != null) {
            dto.setEndOrgId(toLong(vo.getEndAgency().getId()));
        }
        if (vo.getDriver() != null) {
            dto.setDriverId(toLong(vo.getDriver().getUserId()));
        }
        if (vo.getTaskTransport() != null) {
            dto.setTaskTransportId(toLong(vo.getTaskTransport().getId()));
        }
        return dto;
    }

    public static DriverJobVo parseDriverJobDTO2Vo(DriverJobDTO dto, TransportTripsFeign transportTripsFeign, OrgFeign orgFeign, UserFeign userFeign, TruckFeign truckFeign, TransportOrderFeign transportOrderFeign, OrderFeign orderFeign, AreaFeign areaFeign, TransportTaskFeign transportTaskFeign) {
        DriverJobVo vo = new DriverJobVo();
        BeanUtils.copyProperties(dto, vo);
        vo.setId(toStr(dto.getId()));
        if (dto.getStartOrgId() != null && orgFeign != null) {
            OrgDTO org = orgFeign.get(dto.getStartOrgId());
            if (org != null) {
                vo.setStartAgency(parseOrg2SimpleVo(org));
            }
        }
        if (dto.getEndOrgId() != null && orgFeign != null) {
            OrgDTO org = orgFeign.get(dto.getEndOrgId());
            if (org != null) {
                vo.setEndAgency(parseOrg2SimpleVo(org));
            }
        }
        if (dto.getDriverId() != null && userFeign != null) {
            UserDTO user = userFeign.get(dto.getDriverId());
            if (user != null) {
                vo.setDriver(parseUser2Vo(user, null, null));
            }
        }
        if (dto.getTaskTransportId() != null && transportTaskFeign != null) {
            TaskTransportDTO taskTransportDTO = transportTaskFeign.findById(dto.getTaskTransportId());
            if (taskTransportDTO != null) {
                vo.setTaskTransport(parseTaskTransportDTO2Vo(taskTransportDTO, transportTripsFeign, orgFeign, userFeign, truckFeign, transportOrderFeign, orderFeign, areaFeign));
            }
        }
        return vo;
    }

    public static DriverVo parseTruckDriverDto2Vo(TruckDriverDto dto, UserFeign userFeign, FleetFeign fleetFeign, OrgFeign orgFeign) {
        DriverVo vo = new DriverVo();
        if (StringUtils.isNotEmpty(dto.getUserId()) && userFeign != null) {
            UserDTO user = userFeign.get(Long.valueOf(dto.getUserId()));
            if (user != null) {
                vo.setUserId(dto.getUserId());
                BeanUtils.copyProperties(user, vo);
                if (user.getOrgId() != null) {
                    OrgDTO org = orgFeign.get(user.getOrgId());
                    if (org != null) {
                        vo.setAgency(parseOrg2SimpleVo(org));
                    }
                }
            }
        }
        if (StringUtils.isNotEmpty(dto.getFleetId()) && fleetFeign != null) {
            FleetDto fleetDto = fleetFeign.fineById(dto.getFleetId());
            FleetVo fleetVo = new FleetVo();
            BeanUtils.copyProperties(fleetDto, fleetVo);
            vo.setFleet(fleetVo);
        }
        return vo;
    }

    /**
     * 员工编号自动生成：EMP + 用户ID（左补零至至少8位，保留完整ID，保证唯一）
     *
     * @param user 用户
     * @return 员工编号
     */
    private static String generateWorkNumber(UserDTO user) {
        if (user == null || user.getId() == null) {
            return "";
        }
        // 全ID左补零到至少8位，保留完整ID，保证不同用户ID不会映射为相同员工编号
        String idStr = String.valueOf(user.getId());
        return "EMP" + String.format("%8s", idStr).replace(' ', '0');
    }
}
