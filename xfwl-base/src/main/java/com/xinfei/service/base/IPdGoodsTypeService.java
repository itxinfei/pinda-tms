package com.xinfei.service.base;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.base.XfGoodsType;

import java.util.List;

/**
 * 货物类型操作接口
 */
public interface IPdGoodsTypeService extends IService<XfGoodsType> {

    /**
     * 保存货物类型
     */
    XfGoodsType saveGoodsType(XfGoodsType xfGoodsType);

    /**
     * 查询所有启用的货物类型
     */
    List<XfGoodsType> findAll();

    /**
     * 分页查询货物类型
     *
     * @param status 状态过滤：1=启用 0=停用，null=不过滤（查全部）
     */
    IPage<XfGoodsType> findByPage(Integer page, Integer pageSize, String name, Long truckTypeId, String truckTypeName,
                                  Integer status);

    /**
     * 按id列表查询货物类型
     */
    List<XfGoodsType> findAll(List<Long> ids);
}
