package com.itheima.pinda.authority.biz.service.common;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.pinda.authority.entity.common.Area;

import java.util.List;

/**
 * <p>
 * 业务接口
 * 行政区域
 * </p>
 *
 */
public interface AreaService extends IService<Area> {
    /**
     * 获取行政区域列表
     *
     * @param parentId 父级id
     * @param ids      id列表
     * @return 行政区域列表
     */
    List<Area> findAll(Long parentId, List<Long> ids);

    Area  getByCode(String code);
}
