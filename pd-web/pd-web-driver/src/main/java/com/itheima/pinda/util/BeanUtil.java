package com.itheima.pinda.util;

import com.itheima.pinda.DTO.AreaDTO;
import com.itheima.pinda.DTO.OrgDTO;
import com.itheima.pinda.DTO.RoleDTO;
import com.itheima.pinda.DTO.UserDTO;
import com.itheima.pinda.common.utils.Constant;
import com.itheima.pinda.constant.StaticStation;
import com.itheima.pinda.enums.org.OrgType;
import com.itheima.pinda.feign.AreaFeign;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.feign.RoleFeign;
import com.itheima.pinda.vo.*;
import org.springframework.beans.BeanUtils;

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
        // 员工编号自动生成：EMP + 用户ID（左补零至至少8位，保留完整ID，唯一）
        vo.setWorkNumber(generateWorkNumber(user));
        //处理角色信息
        if (roleFeign != null) {
            List<RoleDTO> result = roleFeign.list(user.getId());
            List<RoleVo> roles = new ArrayList<>();
            if (result != null) {
                result.forEach(role -> roles.add(parseRole2Vo(role)));
            }
            vo.setRoles(roles);
        }
        //处理所属机构信息
        if (orgFeign != null && user.getOrgId() != null && user.getOrgId() != 0) {
            OrgDTO result = orgFeign.get(user.getOrgId());
            if (result != null) {
                vo.setAgency(parseOrg2SimpleVo(result));
            }
        }
        //处理岗位信息
        if (user.getStationId() != null && user.getStationId() != 0) {
            if (StaticStation.COURIER_ID.equals(user.getStationId())) {
                vo.setStation(Constant.UserStation.COURIER.getStation());
                vo.setStationName(Constant.UserStation.COURIER.getName());
            } else if (StaticStation.DRIVER_ID.equals(user.getStationId())) {
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
            OrgDTO result = orgFeign.get(org.getParentId());
            if (result != null && result.getId() != null) {
                AgencySimpleVo simpleVo = new AgencySimpleVo();
                BeanUtils.copyProperties(result, simpleVo);
                simpleVo.setId(String.valueOf(result.getId()));
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
            List<AreaDTO> result = areaFeign.findAll(null, new ArrayList<>(areaIds));
            if (result != null) {
                Map<Long, AreaDTO> areaMap = result.stream().collect(Collectors.toMap(AreaDTO::getId, area -> area));
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
