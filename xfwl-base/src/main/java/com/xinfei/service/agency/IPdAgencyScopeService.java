package com.xinfei.service.agency;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.agency.XfAgencyScope;

import java.util.List;

/**
 * 机构作业范围 服务类
 */
public interface IPdAgencyScopeService extends IService<XfAgencyScope> {

    /**
     * 批量保存机构作业范围
     */
    void batchSave(List<XfAgencyScope> scopeList);

    /**
     * 删除机构作业范围
     *
     * @param areaId 行政区域id
     * @param orgId  机构id
     */
    void delete(Integer areaId, Long orgId);

    List<XfAgencyScope> findAll(Integer areaId, Long orgId, List<Long> orgIds, List<Integer> areaIds);
}
