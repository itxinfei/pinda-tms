package com.itheima.pinda.service.agency;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.itheima.pinda.entity.agency.PdFleet;

import java.util.List;

/**
 * 车队 服务类
 */
public interface IPdFleetService extends IService<PdFleet> {

    /**
     * 添加车队
     */
    PdFleet saveFleet(PdFleet fleet);

    /**
     * 车队分页数据
     */
    IPage<PdFleet> findByPage(Integer page, Integer pageSize, String name, String fleetNumber, String manager);

    /**
     * 获取车队列表
     *
     * @param ids 车队id列表
     */
    List<PdFleet> findAll(List<Long> ids, Long orgId);

    /**
     * 禁用车队
     */
    void disableById(Long id);
}
