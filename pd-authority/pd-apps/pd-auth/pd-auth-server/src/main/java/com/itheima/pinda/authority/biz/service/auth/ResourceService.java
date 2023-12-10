package com.itheima.pinda.authority.biz.service.auth;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.pinda.authority.dto.auth.ResourceQueryDTO;
import com.itheima.pinda.authority.entity.auth.Resource;

import java.util.List;

/**
 * <p>
 * 业务接口
 * 资源
 * </p>
 */
public interface ResourceService extends IService<Resource> {

    /**
     * 查询 拥有的资源
     *
     * @param resource
     * @return
     */
    List<Resource> findVisibleResource(ResourceQueryDTO resource);

    /**
     * 根据ID查
     *
     * @param id 主键
     * @return
     */
    Resource getByIdWithCache(Long id);

    /**
     * 根据ID删除
     *
     * @param ids
     * @return
     */
    boolean removeByIdWithCache(List<Long> ids);

    /**
     * 修改
     *
     * @param resource
     * @return
     */
    boolean updateWithCache(Resource resource);

    /**
     * 保存
     *
     * @param resource
     * @return
     */
    boolean saveWithCache(Resource resource);


    /**
     * 根据菜单id删除资源
     *
     * @param menuIds
     */
    void removeByMenuIdWithCache(List<Long> menuIds);

    /**
     * 根据资源id 查询菜单id
     *
     * @param resourceIdList
     * @return
     */
    List<Long> findMenuIdByResourceId(List<Long> resourceIdList);

    /**
     * 根据角色id查询资源
     *
     * @param id
     * @return
     */
    List<Resource> findResourceByRoleId(Long id);
}
