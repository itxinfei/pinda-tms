package com.itheima.pinda.authority.biz.service.core;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.pinda.authority.entity.core.Org;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 业务接口
 * 组织
 * </p>
 */
public interface OrgService extends IService<Org> {

    /**
     * 模糊匹配并分页查询 复杂排序
     *
     * @return
     */
    IPage<Org> pageLike(Page<Org> page, Map params);

    /**
     * 查询指定id集合下的所有子集
     *
     * @param ids
     * @return
     */
    List<Org> findChildren(List<Long> ids);

    /**
     * 批量删除以及删除其子节点
     *
     * @param ids
     * @return
     */
    boolean remove(List<Long> ids);

    IPage<Org> findByPage(IPage<Org> page, Wrapper<Org> queryWrapper);

    Org findById(Long id);

    boolean saveOrg(Org org);

    boolean updateOrg(Org org);

    List<Org> findAll(Integer orgType, List<Long> ids, Long countyId, Long pid, List<Long> pids);
}
