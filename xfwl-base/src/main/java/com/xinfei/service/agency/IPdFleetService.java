package com.xinfei.service.agency;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.agency.XfFleet;

import java.util.List;

/**
 * 车队 服务类
 */
public interface IPdFleetService extends IService<XfFleet> {

    /**
     * 添加车队
     */
    XfFleet saveFleet(XfFleet fleet);

    /**
     * 车队分页数据
     *
     * @param status 状态过滤：1=启用 0=停用，null=不过滤（查全部）
     */
    IPage<XfFleet> findByPage(Integer page, Integer pageSize, String name, String fleetNumber, String manager,
                              Integer status);

    /**
     * 获取车队列表
     *
     * @param ids 车队id列表
     */
    List<XfFleet> findAll(List<Long> ids, Long orgId);

    /**
     * 禁用车队（status=0，可逆）
     */
    void disableById(Long id);

    /**
     * 启用车队（status=1）
     */
    void enableById(Long id);
}
