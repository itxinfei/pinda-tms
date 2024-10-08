package com.itheima.pinda.authority.biz.service.auth;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.pinda.authority.entity.auth.Menu;
import com.itheima.pinda.authority.entity.auth.Menu;

/**
 * <p>
 * 业务接口
 * 菜单
 * </p>
 *
 */
public interface MenuService extends IService<Menu> {

    /**
     * 根据ID查菜单
     *
     * @param id 主键
     * @return
     */
    Menu getByIdWithCache(Long id);

    /**
     * 根据ID删除
     *
     * @param ids
     * @return
     */
    boolean removeByIdWithCache(List<Long> ids);

    /**
     * 修改菜单
     *
     * @param menu
     * @return
     */
    boolean updateWithCache(Menu menu);

    /**
     * 保存菜单
     *
     * @param menu
     * @return
     */
    boolean saveWithCache(Menu menu);

    /**
     * 查询用户可用菜单
     *
     * @param group
     * @param userId
     * @return
     */
    List<Menu> findVisibleMenu(String group, String userId);

}
