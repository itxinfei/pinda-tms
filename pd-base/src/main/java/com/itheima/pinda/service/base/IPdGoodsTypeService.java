package com.itheima.pinda.service.base;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.itheima.pinda.entity.base.PdGoodsType;

import java.util.List;

/**
 * 货物类型操作接口
 */
public interface IPdGoodsTypeService extends IService<PdGoodsType> {

    /**
     * 保存货物类型
     */
    PdGoodsType saveGoodsType(PdGoodsType pdGoodsType);

    /**
     * 查询所有启用的货物类型
     */
    List<PdGoodsType> findAll();

    /**
     * 分页查询货物类型
     */
    IPage<PdGoodsType> findByPage(Integer page, Integer pageSize, String name, Long truckTypeId, String truckTypeName);

    /**
     * 按id列表查询货物类型
     */
    List<PdGoodsType> findAll(List<Long> ids);
}
