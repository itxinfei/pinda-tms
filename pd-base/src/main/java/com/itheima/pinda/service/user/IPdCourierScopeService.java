package com.itheima.pinda.service.user;

import com.baomidou.mybatisplus.spring.service.IService;
import com.itheima.pinda.entity.user.PdCourierScope;

import java.util.List;

/**
 * 快递员作业范围 服务类
 */
public interface IPdCourierScopeService extends IService<PdCourierScope> {

    /**
     * 批量保存快递员作业范围
     */
    void batchSave(List<PdCourierScope> scopeList);

    /**
     * 删除快递员作业范围
     *
     * @param areaId    行政区域id
     * @param courierId 快递员账号id
     */
    void delete(Integer areaId, Long courierId);

    List<PdCourierScope> findAll(Integer areaId, Long courierId);
}
