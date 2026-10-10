package com.itheima.pinda.service.agency;

import com.baomidou.mybatisplus.spring.service.IService;
import com.itheima.pinda.entity.agency.PdAgencyScope;

import java.util.List;

/**
 * 机构作业范围 服务类
 */
public interface IPdAgencyScopeService extends IService<PdAgencyScope> {

    /**
     * 批量保存机构作业范围
     */
    void batchSave(List<PdAgencyScope> scopeList);

    /**
     * 删除机构作业范围
     *
     * @param areaId 行政区域id
     * @param orgId  机构id
     */
    void delete(Integer areaId, Long orgId);

    List<PdAgencyScope> findAll(Integer areaId, Long orgId, List<Long> orgIds, List<Integer> areaIds);
}
