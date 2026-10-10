package com.itheima.pinda.service.transportline;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.itheima.pinda.entity.transportline.PdTransportLineType;

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
     */
    IPage<PdTransportLineType> findByPage(Integer page, Integer pageSize, String typeNumber, String name,
                                          Integer agencyType);

    /**
     * 获取线路类型列表
     */
    List<PdTransportLineType> findAll(List<Long> ids);

    /**
     * 禁用线路类型
     */
    void disableById(Long id);
}
