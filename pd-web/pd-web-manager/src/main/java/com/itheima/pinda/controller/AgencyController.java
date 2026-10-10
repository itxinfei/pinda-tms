package com.itheima.pinda.controller;

import com.itheima.pinda.DTO.AreaDTO;
import com.itheima.pinda.DTO.OrgDTO;
import com.itheima.pinda.DTO.OrgTreeDTO;
import com.itheima.pinda.DTO.UserDTO;
import com.itheima.pinda.DTO.angency.AgencyScopeDto;
import com.itheima.pinda.common.utils.EntCoordSyncJob;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.enums.org.OrgType;
import com.itheima.pinda.feign.AreaFeign;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.feign.RoleFeign;
import com.itheima.pinda.feign.UserFeign;
import com.itheima.pinda.feign.agency.AgencyScopeFeign;
import com.itheima.pinda.util.BeanUtil;
import com.itheima.pinda.util.Rx;
import com.itheima.pinda.vo.base.AreaSimpleVo;
import com.itheima.pinda.vo.base.angency.AgencyScopeVo;
import com.itheima.pinda.vo.base.angency.AgencySimpleVo;
import com.itheima.pinda.vo.base.angency.AgencyVo;
import com.itheima.pinda.vo.base.userCenter.SysUserVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("agency")
@Tag(name = "组织管理")
@Slf4j
public class AgencyController {
    @Autowired
    private OrgFeign orgFeign;
    @Autowired
    private AreaFeign areaFeign;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private RoleFeign roleFeign;
    @Autowired
    private AgencyScopeFeign agencyScopeFeign;

    @Operation(summary = "获取树状机构信息")
    @GetMapping("/tree")
    public List<AgencySimpleVo> treeAgency() {
        List<AgencySimpleVo> resultList = new ArrayList<>();

        List<OrgTreeDTO> tree = orgFeign.tree(null, true);
        if (tree != null && tree.size() > 0) {
            resultList.addAll(tree.stream().map(orgTreeDTO -> {
                AgencySimpleVo simpleVo = BeanUtil.parseOrg2SimpleVo(orgTreeDTO);
                simpleVo.setSubAgencies(getNode(orgTreeDTO.getChildren()));
                return simpleVo;
            }).collect(Collectors.toList()));
        }
        return resultList;
    }

    @Operation(summary = "获取机构详情")
    @GetMapping("/{id}")
    public AgencyVo findAgencyById(@PathVariable(name = "id") String id) {
        // 远程调用可能返回 null，直接判空，避免 NPE
        OrgDTO org = orgFeign.get(Long.valueOf(id));
        if (org != null) {
            return BeanUtil.parseOrg2Vo(org, orgFeign, areaFeign);
        }
        return null;
    }

    @Operation(summary = "获取员工详情")
    @GetMapping("/user/{id}")
    public SysUserVo findById(@PathVariable(name = "id") String id) {
        // 远程调用可能返回 null，直接判空，避免 NPE
        UserDTO user = userFeign.get(Long.valueOf(id));
        SysUserVo vo = null;
        if (user != null) {
            vo = BeanUtil.parseUser2Vo(user, roleFeign, orgFeign);
        }
        return vo;
    }

    @Operation(summary = "获取员工分页数据")
    @GetMapping("/user/page")
    public PageResponse<SysUserVo> findUserByPage(@RequestParam(name = "page") Integer page,
                                                  @RequestParam(name = "pageSize") Integer pageSize,
                                                  @RequestParam(name = "agencyId", required = false) String agencyId) {
        // 远程调用可能返回 null，直接判空，避免 NPE
        Long orgId = StringUtils.isNotEmpty(agencyId) ? Long.valueOf(agencyId) : null;
        PageResponse<UserDTO> userPage = userFeign.page(page.longValue(), pageSize.longValue(), orgId, null, null, null, null);
        if (userPage != null) {
            //处理对象转换：items 可能为 null，统一通过 Rx 安全取值
            List<SysUserVo> voList = Rx.items(userPage).stream().map(user -> BeanUtil.parseUser2Vo(user, roleFeign, orgFeign)).collect(Collectors.toList());
            return PageResponse.<SysUserVo>builder().items(voList).page(page).pagesize(pageSize).counts(userPage.getCounts()).pages(userPage.getPages()).build();
        }
        return PageResponse.<SysUserVo>builder().items(new ArrayList<>()).page(page).pagesize(pageSize).counts(0L).pages(0L).build();
    }

    @Operation(summary = "保存机构业务范围")
    @PostMapping("/scope")
    public Result saveScope(@RequestBody AgencyScopeVo vo) {
        //验证和处理范围和区域信息
        Result result = validateParam(vo);
        if (!"0".equals(result.get("code").toString())) {
            return result;
        }
        //保存前先清理一遍
        AgencyScopeDto deleteDto = new AgencyScopeDto();
        deleteDto.setAgencyId(vo.getAgency().getId());
        agencyScopeFeign.deleteAgencyScope(deleteDto);

        //保存数据
        List<AgencyScopeDto> saveList = vo.getAreas().stream().map(areaVo -> {
            AgencyScopeDto dto = new AgencyScopeDto();
            dto.setAreaId(areaVo.getId());
            dto.setAgencyId(vo.getAgency().getId());
            dto.setMutiPoints(areaVo.getMutiPoints());
            return dto;
        }).collect(Collectors.toList());
        agencyScopeFeign.batchSaveAgencyScope(saveList);
        return Result.ok();
    }

    /**
     * 验证范围参数设置区域id
     *
     * @param vo
     * @return
     */
    private Result validateParam(AgencyScopeVo vo) {
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
                                // 远程调用可能返回 null，直接判空，避免 NPE
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

    @Operation(summary = "获取机构业务范围")
    @GetMapping("/{id}/scope")
    public AgencyScopeVo findAllAgencyScope(@PathVariable(name = "id") String id) {
        // 远程调用可能返回 null，直接判空，避免 NPE
        OrgDTO org = orgFeign.get(Long.valueOf(id));
        if (org != null) {
            List<AgencyScopeDto> agencyScopeDtoList = null;
            if (org.getOrgType() != null) {
                if (org.getOrgType() == OrgType.BUSINESS_HALL.getType()) {
                    //当前机构为网点
                    agencyScopeDtoList = getAgencyScopes(id, null);
                } else if (org.getOrgType() == OrgType.SECONDARY_TRANSFER_CENTER.getType()) {
                    //当前机构为二级转运中心
                    List<String> agencyIds = getOrgIds(Long.valueOf(id), null).stream().map(item -> String.valueOf(item)).collect(Collectors.toList());
                    agencyScopeDtoList = getAgencyScopes(null, agencyIds);
                } else if (org.getOrgType() == OrgType.TOP_TRANSFER_CENTER.getType()) {
                    //当前机构为一级转运中心
                    List<Long> parentIds = getOrgIds(Long.valueOf(id), null);
                    if (parentIds.size() > 0) {
                        List<String> agencyIds = getOrgIds(null, parentIds).stream().map(item -> String.valueOf(item)).collect(Collectors.toList());
                        agencyScopeDtoList = getAgencyScopes(null, agencyIds);
                    }
                } else if (org.getOrgType() == OrgType.BRANCH_OFFICE.getType()) {
                    //当前机构为分公司
                    List<Long> firstLevels = getOrgIds(Long.valueOf(id), null);
                    if (firstLevels.size() > 0) {
                        List<Long> secondLevels = getOrgIds(null, firstLevels);
                        if (secondLevels.size() > 0) {
                            List<String> agencyIds = getOrgIds(null, secondLevels).stream().map(item -> String.valueOf(item)).collect(Collectors.toList());
                            agencyScopeDtoList = getAgencyScopes(null, agencyIds);
                        }
                    }
                }
            }
            //处理返回信息
            AgencyScopeVo vo = new AgencyScopeVo();
            AgencyVo agencyVo = BeanUtil.parseOrg2Vo(org, orgFeign, areaFeign);
            vo.setAgency(agencyVo);
            List<AreaSimpleVo> areas = new ArrayList<>();
            if (agencyScopeDtoList != null) {
                List<Long> areaIds = agencyScopeDtoList.stream().map(dto -> Long.valueOf(dto.getAreaId())).collect(Collectors.toList());
                if (areaIds.size() > 0) {
                    List<AreaDTO> areaList = areaFeign.findAll(null, areaIds);
                    if (areaList != null) {
                        areas.addAll(areaList.stream().map(BeanUtil::parseArea2Vo).collect(Collectors.toList()));
                    }
                }
            }
            vo.setAreas(addMutiPoints(areas, agencyScopeDtoList));
            return vo;
        }
        return null;
    }

    /**
     * 返回结果中添加区域内的作业范围
     *
     * @param areas
     * @param agencyScopeDtoList
     * @return
     */
    private List<AreaSimpleVo> addMutiPoints(List<AreaSimpleVo> areas, List<AgencyScopeDto> agencyScopeDtoList) {
        for (AreaSimpleVo areaSimpleVo : areas) {
            for (AgencyScopeDto agencyScopeDto : agencyScopeDtoList) {
                if (agencyScopeDto.getAreaId().equals(areaSimpleVo.getId())){
                    areaSimpleVo.setMutiPoints(agencyScopeDto.getMutiPoints());
                }
            }
        }
        return areas;
    }

    /**
     * 获取子级组织id列表
     *
     * @param id  组织id
     * @param ids 组织id列表
     * @return 子级组织id列表
     */
    private List<Long> getOrgIds(Long id, List<Long> ids) {
        List<OrgDTO> list = orgFeign.list(null, null, null, id, ids);
        if (list != null && list.size() > 0) {
            return list.stream().map(OrgDTO::getId).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<AgencyScopeDto> getAgencyScopes(String id, List<String> ids) {
        boolean idOk = StringUtils.isNotEmpty(id) && !id.equals("0");
        boolean idsOk = ids != null && ids.size() > 0;
        if (idOk || idsOk) {
            return agencyScopeFeign.findAllAgencyScope(null, id, ids, null);
        }
        return new ArrayList<>();
    }

    /**
     * 递归获取组织树
     *
     * @param dtoList
     * @return
     */
    private List<AgencySimpleVo> getNode(List<OrgTreeDTO> dtoList) {
        List<AgencySimpleVo> list = new ArrayList<>();
        if (dtoList != null && dtoList.size() > 0) {
            for (int i = 0; i < dtoList.size(); i++) {
                AgencySimpleVo vo = BeanUtil.parseOrg2SimpleVo(dtoList.get(i));
                vo.setSubAgencies(getNode(dtoList.get(i).getChildren()));
                list.add(vo);
            }
        }
        return list;
    }
}
