package com.itheima.pinda.authority.biz.service.core.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.authority.biz.dao.core.OrgMapper;
import com.itheima.pinda.authority.biz.service.auth.UserService;
import com.itheima.pinda.authority.biz.service.common.AreaService;
import com.itheima.pinda.authority.biz.service.core.OrgService;
import com.itheima.pinda.authority.biz.utils.LocationUtil;
import com.itheima.pinda.authority.entity.auth.User;
import com.itheima.pinda.authority.entity.common.Area;
import com.itheima.pinda.authority.entity.core.Org;
import com.itheima.pinda.database.mybatis.conditions.Wraps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 业务实现类
 * 组织
 * </p>
 */
@Slf4j
@Service
public class OrgServiceImpl extends ServiceImpl<OrgMapper, Org> implements OrgService {
    @Autowired
    private AreaService areaService;
    @Autowired
    private UserService userService;
    @Autowired
    private LocationUtil locationUtil;

    @Override
    public IPage<Org> pageLike(Page<Org> page, Map params) {

        return this.baseMapper.pageLike(page, params);
    }

    @Override
    public List<Org> findChildren(List<Long> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        // MySQL 全文索引
        String applySql = String.format(" MATCH(tree_path) AGAINST('%s' IN BOOLEAN MODE) ", StringUtils.join(ids, " "));
        return super.list(Wraps.<Org>lbQ().in(Org::getId, ids).or(query -> query.apply(applySql)));
    }

    @Override
    public boolean remove(List<Long> ids) {
        List<Org> list = this.findChildren(ids);
        List<Long> idList = list.stream().mapToLong(Org::getId).boxed().collect(Collectors.toList());
        return !idList.isEmpty() ? super.removeByIds(idList) : true;
    }

    @Override
    public IPage<Org> findByPage(IPage<Org> page, Wrapper<Org> queryWrapper) {
        this.page(page, queryWrapper);
        //补充数据
        if (page.getRecords().size() > 0) {
            Set<Long> areaIds = new HashSet<>();
            Set<Long> parentIds = new HashSet<>();
            Set<Long> managerIds = new HashSet<>();
            Map<Long, Org> orgMap = new HashMap<>();
            Map<Long, Area> areaMap = new HashMap<>();
            Map<Long, User> managerMap = new HashMap<>();
            page.getRecords().stream().forEach(org -> {
                if (org.getProvinceId() != null) {
                    areaIds.add(org.getProvinceId());
                }
                if (org.getCityId() != null) {
                    areaIds.add(org.getCityId());
                }
                if (org.getCountyId() != null) {
                    areaIds.add(org.getCountyId());
                }
                if (org.getParentId() != null && org.getParentId() != 0) {
                    parentIds.add(org.getParentId());
                }
                if (org.getManagerId() != null) {
                    managerIds.add(org.getManagerId());
                }
            });
            if (areaIds.size() > 0) {
                areaMap.putAll(areaService.listByIds(areaIds).stream().collect(Collectors.toMap(Area::getId, area -> area)));
            }
            if (parentIds.size() > 0) {
                orgMap.putAll(this.listByIds(parentIds).stream().collect(Collectors.toMap(Org::getId, org -> org)));
            }
            if (managerIds.size() > 0) {
                managerMap.putAll(userService.listByIds(managerIds).stream().collect(Collectors.toMap(User::getId, user -> user)));
            }
            page.getRecords().stream().forEach(org -> {
                if (org.getProvinceId() != null && areaMap.containsKey(org.getProvinceId())) {
                    org.setProvinceName(areaMap.get(org.getProvinceId()).getName());
                }
                if (org.getCityId() != null && areaMap.containsKey(org.getCityId())) {
                    org.setCityName(areaMap.get(org.getCityId()).getName());
                }
                if (org.getCountyId() != null && areaMap.containsKey(org.getCountyId())) {
                    org.setCountyName(areaMap.get(org.getCountyId()).getName());
                }
                if (org.getParentId() != null && org.getParentId() != 0 && orgMap.containsKey(org.getParentId())) {
                    org.setParentName(orgMap.get(org.getParentId()).getName());
                }
                if (org.getManagerId() != null && managerMap.containsKey(org.getManagerId())) {
                    org.setManager(managerMap.get(org.getManagerId()).getName());
                }
            });
        }
        return page;
    }

    @Override
    public Org findById(Long id) {
        Org org = this.getById(id);
        if (org != null) {
            Set<Long> areaIds = new HashSet<>();
            if (org.getProvinceId() != null) {
                areaIds.add(org.getProvinceId());
            }
            if (org.getCityId() != null) {
                areaIds.add(org.getCityId());
            }
            if (org.getCountyId() != null) {
                areaIds.add(org.getCountyId());
            }
            if (areaIds.size() > 0) {
                Map<Long, Area> areaMap = areaService.listByIds(areaIds).stream().collect(Collectors.toMap(Area::getId, area -> area));
                if (org.getProvinceId() != null) {
                    org.setProvinceName(areaMap.get(org.getProvinceId()).getName());
                }
                if (org.getCityId() != null) {
                    org.setCityName(areaMap.get(org.getCityId()).getName());
                }
                if (org.getCountyId() != null) {
                    org.setCountyName(areaMap.get(org.getCountyId()).getName());
                }
            }
            if (org.getParentId() != null && org.getParentId() != 0) {
                Org parent = this.getById(org.getParentId());
                if (parent != null) {
                    org.setParentName(parent.getName());
                }
            }
            if (org.getManagerId() != null) {
                User manager = userService.getById(org.getManagerId());
                if (manager != null) {
                    org.setManager(manager.getName());
                }
            }
        }
        return org;
    }

    @Override
    public boolean saveOrg(Org org) {
        //补充经纬度信息
        setLocationInfo(org);
        return this.save(org);
    }

    @Override
    public boolean updateOrg(Org org) {
        //补充经纬度信息
        setLocationInfo(org);
        return this.updateById(org);
    }

    @Override
    public List<Org> findAll(Integer orgType, List<Long> ids, Long countyId, Long pid, List<Long> pids) {
        LambdaQueryWrapper<Org> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        boolean conditionsNotOk = true;
        if (orgType != null) {
            lambdaQueryWrapper.eq(Org::getOrgType, orgType);
            conditionsNotOk = false;
        }
        if (ids != null && ids.size() > 0) {
            lambdaQueryWrapper.in(Org::getId, ids);
            conditionsNotOk = false;
        }
        if (countyId != null) {
            lambdaQueryWrapper.eq(Org::getCountyId, countyId);
            conditionsNotOk = false;
        }
        if (pid != null) {
            lambdaQueryWrapper.eq(Org::getParentId, pid);
            conditionsNotOk = false;
        }
        if (pids != null && pids.size() > 0) {
            lambdaQueryWrapper.in(Org::getParentId, pids);
            conditionsNotOk = false;
        }
        if (conditionsNotOk) {
            //防止无条件导致内容过多，撑爆内存
            lambdaQueryWrapper.last("limit 10");
        }
        return this.list(lambdaQueryWrapper);
    }

    /**
     * 补充经纬度信息
     *
     * @param org
     */
    private void setLocationInfo(Org org) {
        boolean provinceOk = org.getProvinceId() != null && org.getProvinceId() != 0;
        boolean cityOk = org.getCityId() != null && org.getCityId() != 0;
        boolean countyOk = org.getCountyId() != null && org.getCountyId() != 0;
        if (StringUtils.isNotEmpty(org.getAddress()) && provinceOk && cityOk && countyOk) {
            Set<Long> areaIds = new HashSet<>(3);
            areaIds.add(org.getProvinceId());
            areaIds.add(org.getCityId());
            areaIds.add(org.getCountyId());
            Map<Long, Area> areaMap = areaService.listByIds(areaIds).stream().collect(Collectors.toMap(Area::getId, area -> area));
            if (areaMap != null && areaMap.size() > 0) {
                String address = areaMap.get(org.getProvinceId()).getName() + areaMap.get(org.getCityId()).getName() + areaMap.get(org.getCountyId()).getName() + org.getAddress();
                Map<String, String> locationMap = locationUtil.getLatitude(address);
                if (locationMap != null) {
                    org.setLatitude(locationMap.get(LocationUtil.KEY_LAT));
                    org.setLongitude(locationMap.get(LocationUtil.KEY_LNG));
                }
            }
        }
    }
}
