package com.itheima.pinda.authority.controller.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.pinda.auth.client.properties.AuthClientProperties;
import com.itheima.pinda.auth.server.utils.JwtTokenServerUtils;
import com.itheima.pinda.authority.dto.auth.MenuSaveDTO;
import com.itheima.pinda.authority.dto.auth.MenuTreeDTO;
import com.itheima.pinda.authority.dto.auth.MenuUpdateDTO;
import com.itheima.pinda.authority.dto.auth.RouterMeta;
import com.itheima.pinda.authority.dto.auth.VueRouter;
import com.itheima.pinda.authority.entity.auth.Menu;
import com.itheima.pinda.authority.biz.service.auth.MenuService;
import com.itheima.pinda.base.BaseController;
import com.itheima.pinda.base.R;
import com.itheima.pinda.base.entity.SuperEntity;
import com.itheima.pinda.context.BaseContextConstants;
import com.itheima.pinda.database.mybatis.conditions.Wraps;
import com.itheima.pinda.database.mybatis.conditions.query.LbqWrapper;
import com.itheima.pinda.dozer.DozerUtils;
import com.itheima.pinda.log.annotation.SysLog;
import com.itheima.pinda.utils.TreeUtil;

import com.itheima.pinda.database.mybatis.conditions.Wraps;
import com.itheima.pinda.database.mybatis.conditions.query.LbqWrapper;
import com.itheima.pinda.log.annotation.SysLog;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 前端控制器
 * 菜单
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/menu")
@Api(value = "Menu", tags = "菜单")
public class MenuController extends BaseController {
    @Autowired
    private MenuService menuService;
    @Autowired
    private DozerUtils dozer;

    /**
     * 分页查询菜单
     */
    @ApiOperation(value = "分页查询菜单", notes = "分页查询菜单")
    @ApiImplicitParams({@ApiImplicitParam(name = "current", value = "当前页", dataType = "long", paramType = "query", defaultValue = "1"), @ApiImplicitParam(name = "size", value = "每页显示几条", dataType = "long", paramType = "query", defaultValue = "10"),})
    @GetMapping("/page")
    @SysLog("分页查询菜单")
    public R<IPage<Menu>> page(Menu data) {
        IPage<Menu> page = getPage();
        // 构建值不为null的查询条件
        LbqWrapper<Menu> query = Wraps.lbQ(data).orderByDesc(Menu::getUpdateTime);
        menuService.page(page, query);
        return success(page);
    }

    /**
     * 查询菜单
     */
    @ApiOperation(value = "查询菜单", notes = "查询菜单")
    @GetMapping("/{id}")
    @SysLog("查询菜单")
    public R<Menu> get(@PathVariable Long id) {
        return success(menuService.getById(id));
    }

    /**
     * 新增菜单
     */
    @ApiOperation(value = "新增菜单", notes = "新增菜单不为空的字段")
    @PostMapping
    @SysLog("新增菜单")
    public R<Menu> save(@RequestBody @Validated MenuSaveDTO data) {
        Menu menu = dozer.map(data, Menu.class);
        menuService.save(menu);
        return success(menu);
    }

    /**
     * 修改菜单
     */
    @ApiOperation(value = "修改菜单", notes = "修改菜单不为空的字段")
    @PutMapping
    @SysLog("修改菜单")
    public R<Menu> update(@RequestBody @Validated(SuperEntity.Update.class) MenuUpdateDTO data) {
        Menu menu = dozer.map(data, Menu.class);
        menuService.updateById(menu);
        return success(menu);
    }

    /**
     * 删除菜单
     */
    @ApiOperation(value = "删除菜单", notes = "根据id物理删除菜单")
    @DeleteMapping
    @SysLog("删除菜单")
    public R<Boolean> delete(@RequestParam("ids[]") List<Long> ids) {
        menuService.removeByIds(ids);
        return success(true);
    }

    /**
     * 查询用户可用的所有资源
     *
     * @param group 菜单分组
     */
    @ApiImplicitParams({@ApiImplicitParam(name = "group", value = "菜单组", dataType = "string", paramType = "query")})
    @ApiOperation(value = "查询用户可用的所有菜单", notes = "查询用户可用的所有菜单")
    @GetMapping
    @Deprecated
    public R<List<MenuTreeDTO>> myMenus(@RequestParam(value = "group", required = false) String group) {
        // 2026-10-08 与 myRouter 同步：不再接受 userId 查询参数，身份取自网关注入的头/上下文
        String userId = String.valueOf(getUserId());
        List<Menu> list = menuService.findVisibleMenu(group, userId);
        List<MenuTreeDTO> treeList = dozer.mapList(list, MenuTreeDTO.class);

        List<MenuTreeDTO> tree = TreeUtil.build(treeList);
        return success(tree);
    }

    /**
     * @return
     */
    private List<VueRouter> buildSuperAdminRouter() {
        List<VueRouter> tree = new ArrayList<>();
        List<VueRouter> children = new ArrayList<>();

        VueRouter defaults = new VueRouter();
        defaults.setPath("/defaults");
        defaults.setComponent("Layout");
        defaults.setHidden(false);
        defaults.setAlwaysShow(true);
        defaults.setMeta(RouterMeta.builder().title("系统设置").icon("el-icon-coin").breadcrumb(true).build());
        defaults.setId(-1L);
        defaults.setChildren(children);

        tree.add(defaults);
        return tree;
    }

    @Autowired
    private JwtTokenServerUtils jwtTokenServerUtils;

    /**
     * 查询用户可用的所有菜单路由树
     *
     * @param group 菜单组
     */
    @ApiImplicitParams({@ApiImplicitParam(name = "group", value = "菜单组", dataType = "string", paramType = "query")})
    @ApiOperation(value = "查询用户可用的所有菜单路由树", notes = "查询用户可用的所有菜单路由树")
    @GetMapping("/router")
    public R<List<VueRouter>> myRouter(@RequestParam(value = "group", required = false) String group, HttpServletRequest request) {
        log.info("查询用户可用的所有菜单路由树");
        // 2026-10-06 修复：原代码取 userId 的逻辑被整段注释，导致前端不传 userId 时
        // 一直以 null 查询（缓存 key 变成 "user_menu:null"），进而查不到任何菜单、登录后菜单为空。
        // 2026-10-08 去掉 userId 查询参数：身份只能来自网关注入的 userid 头，
        // 否则任何登录用户都能带 ?userId=1 读到别人的菜单（暴露未授权功能入口）。
        //（ContextHandlerInterceptor 依赖 springfox 的 PropertySourcedRequestMappingHandlerMapping,
        // 实测部分容器环境未注册到 ThreadLocal, 导致 getUserId() 返回 0 查询空。）
        String userId = request.getHeader(BaseContextConstants.JWT_KEY_USER_ID);
        if (StrUtil.isBlank(userId)) {
            userId = String.valueOf(getUserId());
        }
        List<Menu> list = menuService.findVisibleMenu(group, userId);
        log.info("查询用户可用的所有菜单路由树:{}", list);
        List<VueRouter> treeList = dozer.mapList(list, VueRouter.class);
        log.info("查询用户可用的所有菜单路由树:{}", treeList);
        // 2026-10-06 修复：dozer 只映射 path/name/component，VueRouter.meta 为 null，
        // 而类上 @JsonInclude(NON_NULL) 会让 meta 字段在 JSON 中直接消失。前端
        // SidebarItem 依赖 meta.title 渲染菜单，缺 meta 会导致叶子菜单不渲染、父级
        // submenu 无标题，登录成功但侧边栏空白。这里按每个菜单的 name/icon 补齐 meta。
        Map<Long, String> iconMap = list.stream().collect(Collectors.toMap(
                Menu::getId,
                menu -> menu.getIcon() == null ? "" : menu.getIcon(),
                (oldVal, newVal) -> oldVal));
        treeList.forEach(router -> router.setMeta(RouterMeta.builder()
                .title(router.getName())
                .icon(iconMap.getOrDefault(router.getId(), ""))
                .build()));

        List<VueRouter> build = TreeUtil.build(treeList);
        log.info("查询用户可用的所有菜单路由树:{}", build);
        if (build == null) {
            return success(buildSuperAdminRouter());
        }
        return success(build);
    }

    /**
     * @return
     */
    @ApiOperation(value = "查询超管菜单路由树", notes = "查询超管菜单路由树")
    @GetMapping("/admin/router")
    public R<List<VueRouter>> adminRouter() {
        return success(buildSuperAdminRouter());
    }

    /**
     * 查询系统中所有的的菜单树结构， 不用缓存，因为该接口很少会使用，就算使用，也会管理员维护菜单时使用
     */
    @ApiOperation(value = "查询系统所有的菜单", notes = "查询系统所有的菜单")
    @GetMapping("/tree")
    @SysLog("查询系统所有的菜单")
    public R<List<MenuTreeDTO>> allTree() {
        List<Menu> list = menuService.list(Wraps.<Menu>lbQ().orderByAsc(Menu::getSortValue));
        List<MenuTreeDTO> treeList = dozer.mapList(list, MenuTreeDTO.class);
        return success(TreeUtil.build(treeList));
    }
}