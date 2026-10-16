package com.xinfei.util;

import com.xinfei.DTO.*;
import com.xinfei.DTO.angency.FleetDto;
import com.xinfei.DTO.transportline.TransportTripsDto;
import com.xinfei.DTO.truck.TruckDto;
import com.xinfei.DTO.user.TruckDriverDto;
import com.xinfei.common.utils.Constant;
import com.xinfei.constant.StaticStation;
import com.xinfei.enums.org.OrgType;
import com.xinfei.feign.AreaFeign;
import com.xinfei.feign.DriverJobFeign;
import com.xinfei.feign.OrderFeign;
import com.xinfei.feign.OrgFeign;
import com.xinfei.feign.RoleFeign;
import com.xinfei.feign.TransportOrderFeign;
import com.xinfei.feign.TransportTaskFeign;
import com.xinfei.feign.UserFeign;
import com.xinfei.feign.agency.FleetFeign;
import com.xinfei.feign.transportline.TransportTripsFeign;
import com.xinfei.feign.truck.TruckFeign;
import com.xinfei.vo.base.AreaSimpleVo;
import com.xinfei.vo.base.angency.AgencySimpleVo;
import com.xinfei.vo.base.angency.AgencyVo;
import com.xinfei.vo.base.angency.RoleVo;
import com.xinfei.vo.base.transforCenter.business.DriverVo;
import com.xinfei.vo.base.transforCenter.business.FleetVo;
import com.xinfei.vo.base.transforCenter.business.TransportTripsVo;
import com.xinfei.vo.base.transforCenter.business.TruckVo;
import com.xinfei.vo.base.userCenter.SysUserVo;
import com.xinfei.vo.oms.OrderVo;
import com.xinfei.vo.work.DriverJobVo;
import com.xinfei.vo.work.TaskPickupDispatchVo;
import com.xinfei.vo.work.TaskTransportVo;
import com.xinfei.vo.work.TransportOrderVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            OrgType orgTypeEnum = OrgType.getEnumByType(org.getOrgType());
        agencyVo.setAgencyTypeName(orgTypeEnum == null ? null : orgTypeEnum.getName());
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

    /**
     * 订单 DTO → VO（区划批量富化）。
     *
     * <p>原实现逐单逐级6 次 {@code areaFeign.get}（发/收各省市区），分页 20 单就是
     * 120 次远程调用；这里改为整页一次批量查询后回填。</p>
     */
    public static OrderVo parseOrderDTO2Vo(OrderDTO dto, AreaFeign areaFeign) {
        return parseOrderDTO2Vo(dto, areaFeign, null);
    }

    /**
     * 订单 DTO → VO（区划可由调用方批量预置）。
     *
     * @param areasById 区划 id → 区划数据；为空时按单条查询（详情场景）
     */
    public static OrderVo parseOrderDTO2Vo(OrderDTO dto, AreaFeign areaFeign, Map<Long, AreaDTO> areasById) {
        OrderVo vo = new OrderVo();
        BeanUtils.copyProperties(dto, vo);
        fillOrderArea(vo, dto, areaFeign, areasById, true);
        return vo;
    }

    /** 填充发件/收件三级区划（single=true 表示单条模式，可直接远程查） */
    private static void fillOrderArea(OrderVo vo, OrderDTO dto, AreaFeign areaFeign,
                                      Map<Long, AreaDTO> areasById, boolean single) {
        vo.setSenderProvince(parseArea2Vo(lookupArea(dto.getSenderProvinceId(), areaFeign, areasById, single)));
        vo.setSenderCity(parseArea2Vo(lookupArea(dto.getSenderCityId(), areaFeign, areasById, single)));
        vo.setSenderCounty(parseArea2Vo(lookupArea(dto.getSenderCountyId(), areaFeign, areasById, single)));
        vo.setReceiverProvince(parseArea2Vo(lookupArea(dto.getReceiverProvinceId(), areaFeign, areasById, single)));
        vo.setReceiverCity(parseArea2Vo(lookupArea(dto.getReceiverCityId(), areaFeign, areasById, single)));
        vo.setReceiverCounty(parseArea2Vo(lookupArea(dto.getReceiverCountyId(), areaFeign, areasById, single)));
    }

    /** 区划查询：优先取批量预置，single 模式才远程单查 */
    private static AreaDTO lookupArea(String rawId, AreaFeign areaFeign, Map<Long, AreaDTO> areasById, boolean single) {
        if (StringUtils.isBlank(rawId)) {
            return null;
        }
        Long areaId;
        try {
            areaId = Long.valueOf(rawId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
        if (areasById != null) {
            return areasById.get(areaId);
        }
        return single && areaFeign != null ? areaFeign.get(areaId) : null;
    }

    /**
     * 批量取一页订单涉及的区划（省/市/区去重后一次查询）。
     *
     * @param orders 一页订单
     * @return 区划 id → 区划数据
     */
    public static Map<Long, AreaDTO> loadAreasBatch(List<OrderDTO> orders, AreaFeign areaFeign) {
        Map<Long, AreaDTO> result = new LinkedHashMap<>();
        if (orders == null || orders.isEmpty() || areaFeign == null) {
            return result;
        }
        List<Long> ids = orders.stream()
                .flatMap(order -> Stream.of(order.getSenderProvinceId(), order.getSenderCityId(),
                        order.getSenderCountyId(), order.getReceiverProvinceId(),
                        order.getReceiverCityId(), order.getReceiverCountyId()))
                .filter(StringUtils::isNotBlank)
                .map(id -> {
                    try {
                        return Long.valueOf(id.trim());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return result;
        }
        for (AreaDTO area : Rx.list(areaFeign.findAll(null, ids))) {
            if (area != null && area.getId() != null) {
                result.put(area.getId(), area);
            }
        }
        return result;
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
        return parseTaskTransportDTO2Vo(dto, transportTripsFeign, orgFeign, userFeign, truckFeign,
                transportOrderFeign, orderFeign, areaFeign, null);
    }

    /**
     * 运输任务 DTO → VO。
     *
     * <p>driverJobFeign 用于组装司机信息：任务与司机的权威关联是 {@code work_driver_job}
     * （一个任务可绑定多个司机，如主副驾）。历史实现未组装 drivers，导致列表「司机」列与
     * 详情「司机信息」表格恒为空；此处补齐并对 Feign 为 null 的旧调用方保持兼容。</p>
     */
    public static TaskTransportVo parseTaskTransportDTO2Vo(TaskTransportDTO dto, TransportTripsFeign transportTripsFeign, OrgFeign orgFeign, UserFeign userFeign, TruckFeign truckFeign, TransportOrderFeign transportOrderFeign, OrderFeign orderFeign, AreaFeign areaFeign, DriverJobFeign driverJobFeign) {
        return parseTaskTransportDTO2Vo(dto, transportTripsFeign, orgFeign, userFeign, truckFeign,
                transportOrderFeign, orderFeign, areaFeign, driverJobFeign, null);
    }

    /**
     * 运输任务 DTO → VO（可传入预置的司机映射，避免列表逐行远程查询）。
     *
     * @param driversByTaskId 任务 id → 司机列表的预置映射；为空时按单任务查询（详情场景）
     */
    public static TaskTransportVo parseTaskTransportDTO2Vo(TaskTransportDTO dto, TransportTripsFeign transportTripsFeign, OrgFeign orgFeign, UserFeign userFeign, TruckFeign truckFeign, TransportOrderFeign transportOrderFeign, OrderFeign orderFeign, AreaFeign areaFeign, DriverJobFeign driverJobFeign, Map<Long, List<SysUserVo>> driversByTaskId) {
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
        // 司机信息：列表场景由调用方批量预置映射（避免逐行远程查询）；详情场景单任务反查。
        // 未派司机的任务返回空列表（前端显示 "-"）
        List<SysUserVo> drivers = driversByTaskId != null
                ? driversByTaskId.getOrDefault(dto.getId(), new ArrayList<>())
                : parseTaskDrivers(dto.getId(), driverJobFeign, userFeign);
        vo.setDrivers(drivers);
        // 运单数量：聚合层 SQL 不返回该列，直接由关联运单数推导，
        // 否则详情页「运单数量」恒显-（与「司机信息恒空」同类问题）
        vo.setTransportOrderCount(dto.getTransportOrderIds() == null ? 0 : dto.getTransportOrderIds().size());
        return vo;
    }

    /**
     * 组装运输任务的司机列表（可多司机）。
     *
     * <p>链路：{@code work_driver_job.task_transport_id → driver_id → 用户详情}。
     * Feign 返回可能为 null（下游异常/无数据），统一走 {@link Rx} 兜底为空集合，
     * 避免详情页因单个关联查询失败整体报错。</p>
     *
     * @param taskId运输任务 id
     * @return 司机 VO 列表，无司机时为空列表（非 null）
     */
    /**
     * 批量取一批运输任务的司机（分页列表用）。
     *
     * <p>把「每行 1 次 driver-job 查询 + 1 次用户查询」的 N+1 压成 2 次远程调用：
     * 先按任务 id 集合一次查司机作业单，再把所有司机 id 去重后一次查用户。
     * 任一步失败/无数据都返回空 Map（前端显示 "-"），不影响列表主体渲染。</p>
     *
     * @param taskIds 一页内的运输任务 id
     * @return 任务 id → 司机列表
     */
    public static Map<Long, List<SysUserVo>> parseTaskDriversBatch(List<Long> taskIds, DriverJobFeign driverJobFeign, UserFeign userFeign) {
        Map<Long, List<SysUserVo>> result = new LinkedHashMap<>();
        if (taskIds == null || taskIds.isEmpty() || driverJobFeign == null || userFeign == null) {
            return result;
        }
        List<DriverJobDTO> jobs = Rx.list(driverJobFeign.findAllByTaskTransportIds(taskIds));
        if (jobs.isEmpty()) {
            return result;
        }
        List<Long> driverIds = jobs.stream()
                .map(DriverJobDTO::getDriverId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (driverIds.isEmpty()) {
            return result;
        }
        Map<Long, SysUserVo> userMap = Rx.list(userFeign.list(driverIds, null, null, null)).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(UserDTO::getId, user -> parseUser2Vo(user, null, null), (a, b) -> a));
        for (DriverJobDTO job : jobs) {
            SysUserVo userVo = userMap.get(job.getDriverId());
            if (userVo == null || job.getTaskTransportId() == null) {
                continue;
            }
            result.computeIfAbsent(job.getTaskTransportId(), k -> new ArrayList<>());
            // 同一任务多司机时按 userId 去重
            List<SysUserVo> drivers = result.get(job.getTaskTransportId());
            boolean duplicated = drivers.stream().anyMatch(item -> String.valueOf(item.getUserId()).equals(String.valueOf(userVo.getUserId())));
            if (!duplicated) {
                drivers.add(userVo);
            }
        }
        return result;
    }

    private static List<SysUserVo> parseTaskDrivers(Long taskId, DriverJobFeign driverJobFeign, UserFeign userFeign) {
        if (taskId == null || driverJobFeign == null || userFeign == null) {
            return new ArrayList<>();
        }
        DriverJobDTO query = new DriverJobDTO();
        query.setTaskTransportId(taskId);
        List<Long> driverIds = Rx.list(driverJobFeign.findAll(query)).stream()
                .map(DriverJobDTO::getDriverId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (driverIds.isEmpty()) {
            return new ArrayList<>();
        }
        return Rx.list(userFeign.list(driverIds, null, null, null)).stream()
                .map(user -> parseUser2Vo(user, null, null))
                .collect(Collectors.toList());
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
