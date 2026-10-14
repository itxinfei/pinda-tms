package com.xinfei.service.transportline;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.transportline.PdTransportLineType;

import java.util.List;

/**
 * 线路类型 服务类
 */
public interface IPdTransportLineTypeService extends IService<PdTransportLineType> {

    /**
     * 添加线路类型
     */
    PdTransportLineType saveTransportLineType(PdTransportLineType pdTransportLineType);

    /**
     * 获取线路类型分页数据
     *
     * @param status 状态过滤：1=启用 0=停用，null=不过滤（查全部）
     */
    IPage<PdTransportLineType> findByPage(Integer page, Integer pageSize, String typeNumber, String name,
                                          Integer agencyType, Integer status);

    /**
     * 获取线路类型列表
     */
    List<PdTransportLineType> findAll(List<Long> ids);

    /**
     * 禁用线路类型（status=0，可逆）
     */
    void disableById(Long id);

    /**
     * 启用线路类型（status=1）
     */
    void enableById(Long id);
}
