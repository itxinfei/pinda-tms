package com.xinfei.auth.controller.v1;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xinfei.auth.common.v1.ApiResult;
import com.xinfei.auth.common.v1.MenuForm;
import com.xinfei.auth.common.v1.MenuVO;
import com.xinfei.auth.common.v1.OptionVO;
import com.xinfei.auth.common.v1.RouteDTO;
import com.xinfei.auth.config.StpInterfaceImpl;
import com.xinfei.auth.service.v1.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 菜单管理（youlai 契约 /api/v1/menus，经网关 StripPrefix=2 后为本路径）。
 * /routes 动态路由当前对登录用户返回全部菜单；角色过滤随 StpInterface（#8）接入。
 */
@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final StpInterfaceImpl stpInterface;

    /** 当前用户动态路由：按角色授权菜单过滤（超管返回全部） */
    @GetMapping("/routes")
    public ApiResult<List<RouteDTO>> routes() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<Long> authorizedIds = stpInterface.authorizedMenuIds(userId);
        return ApiResult.success(menuService.buildRoutes(authorizedIds));
    }

    /** 菜单管理树（含按钮，可选关键字） */
    @GetMapping
    @SaCheckPermission("sys:menu:list")
    public ApiResult<List<MenuVO>> list(@RequestParam(value = "keywords", required = false) String keywords) {
        return ApiResult.success(menuService.voTree(keywords, true));
    }

    /** 上级菜单/目录选项（types 可限定 C,M） */
    @GetMapping("/options")
    @SaCheckPermission("sys:menu:list")
    public ApiResult<List<OptionVO>> options(@RequestParam(value = "types", required = false) String types) {
        List<String> ts = (types == null || types.isBlank())
                ? null : Arrays.stream(types.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return ApiResult.success(menuService.options(ts));
    }

    @GetMapping("/{id}/form")
    @SaCheckPermission("sys:menu:list")
    public ApiResult<MenuForm> form(@PathVariable Long id) {
        MenuVO vo = menuService.formData(id);
        MenuForm f = new MenuForm();
        if (vo != null) {
            f.setId(vo.getId());
            f.setParentId(vo.getParentId());
            f.setType(vo.getType());
            f.setName(vo.getName());
            f.setPath(vo.getPath());
            f.setRoutePath(vo.getRoutePath());
            f.setRouteName(vo.getRouteName());
            f.setComponent(vo.getComponent());
            f.setPerm(vo.getPerm());
            f.setIcon(vo.getIcon());
            f.setSort(vo.getSort());
            f.setVisible(vo.getVisible());
            f.setRedirect(vo.getRedirect());
            f.setKeepAlive(vo.getKeepAlive());
            f.setExternalUrl(vo.getExternalUrl());
        }
        return ApiResult.success(f);
    }

    @PostMapping
    @SaCheckPermission("sys:menu:create")
    public ApiResult<Long> create(@RequestBody MenuForm form) {
        return ApiResult.success(menuService.saveForm(form));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("sys:menu:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody MenuForm form) {
        form.setId(String.valueOf(id));
        menuService.updateForm(form);
        return ApiResult.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:menu:delete")
    public ApiResult<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return ApiResult.success();
    }
}
