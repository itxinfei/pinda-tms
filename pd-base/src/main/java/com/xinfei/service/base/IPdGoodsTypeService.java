package com.xinfei.service.base;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.base.PdGoodsType;

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
     *
     * @param status 状态过滤：1=启用 0=停用，null=不过滤（查全部）
     */
    IPage<PdGoodsType> findByPage(Integer page, Integer pageSize, String name, Long truckTypeId, String truckTypeName,
                                  Integer status);

    /**
     * 按id列表查询货物类型
     */
    List<PdGoodsType> findAll(List<Long> ids);
}
