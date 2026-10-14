package com.xinfei.auth.service.v1;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xinfei.auth.common.BizException;
import com.xinfei.auth.common.v1.MenuForm;
import com.xinfei.auth.common.v1.MenuVO;
import com.xinfei.auth.common.v1.OptionVO;
import com.xinfei.auth.common.v1.RouteDTO;
import com.xinfei.auth.entity.SysMenu;
import com.xinfei.auth.mapper.SysMenuMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单/按钮管理：管理树、选项、动态路由构建、增删改。
 *
 * <p>sys_menu 为全局平台表（租户插件忽略）。type：C目录 M菜单 E外链 B按钮；
 * 按钮(B)不进路由树、不出现在目录/菜单选项。组树/过滤全部在内存完成，避免 N+1。</p>
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final SysMenuMapper menuMapper;

    private static final String T_CATALOG = "C";
    private static final String T_MENU = "M";
    private static final String T_EXTLINK = "E";
    private static final String T_BUTTON = "B";
    private static final String LAYOUT = "Layout";
    private static final List<String> ROUTER_TYPES = List.of(T_CATALOG, T_MENU, T_EXTLINK);

    /** 管理菜单树（MenuNode，内部/通用） */
    public List<MenuNode> treeNodes(Integer status) {
        List<SysMenu> menus = loadRouters(status);
        return toNodes(groupByParent(menus).getOrDefault(0L, new ArrayList<>()), groupByParent(menus));
    }

    /** 管理菜单树（对齐前端 MenuItem，含隐藏/按钮可选） */
    public List<MenuVO> voTree(String keywords, boolean includeButtons) {
        List<SysMenu> menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .in(includeButtons, SysMenu::getType, List.of(T_CATALOG, T_MENU, T_EXTLINK, T_BUTTON))
                .and(!includeButtons, w -> w.in(SysMenu::getType, ROUTER_TYPES))
                .like(keywords != null && !keywords.isBlank(), SysMenu::getName, keywords)
                .orderByAsc(SysMenu::getSortValue).orderByAsc(SysMenu::getId));
        Map<Long, List<SysMenu>> byParent = groupByParent(menus);
        return toVOs(byParent.getOrDefault(0L, new ArrayList<>()), byParent);
    }

    private List<MenuVO> toVOs(List<SysMenu> level, Map<Long, List<SysMenu>> byParent) {
        List<MenuVO> out = new ArrayList<>();
        for (SysMenu m : level) {
            MenuVO v = new MenuVO();
            v.setId(String.valueOf(m.getId()));
            v.setParentId(String.valueOf(m.getParentId() == null ? 0L : m.getParentId()));
            v.setType(m.getType());
            v.setName(m.getName());
            v.setRouteName(m.getRouteName());
            v.setPath(m.getPath());
            v.setRoutePath(m.getPath());
            v.setComponent(m.getComponent());
            v.setPerm(m.getPerm());
            v.setIcon(m.getIcon());
            v.setSort(m.getSortValue());
            v.setVisible(m.getVisible());
            v.setScope(1);
            v.setRedirect(m.getRedirect());
            v.setKeepAlive(m.getKeepAlive());
            v.setExternalUrl(m.getExternalUrl());
            v.setChildren(toVOs(byParent.getOrDefault(m.getId(), new ArrayList<>()), byParent));
            out.add(v);
        }
        return out;
    }

    /** 选项树（目录/菜单，前端 options 用），types 可限定 */
    public List<OptionVO> options(List<String> types) {
        List<String> ts = (types == null || types.isEmpty()) ? ROUTER_TYPES : types;
        List<SysMenu> menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getStatus, 1)
                .in(SysMenu::getType, ts)
                .orderByAsc(SysMenu::getSortValue).orderByAsc(SysMenu::getId));
        Map<Long, List<SysMenu>> byParent = groupByParent(menus);
        return toOptions(byParent.getOrDefault(0L, new ArrayList<>()), byParent);
    }

    private List<OptionVO> toOptions(List<SysMenu> level, Map<Long, List<SysMenu>> byParent) {
        List<OptionVO> out = new ArrayList<>();
        for (SysMenu m : level) {
            OptionVO o = new OptionVO();
            o.setValue(String.valueOf(m.getId()));
            o.setLabel(m.getName());
            o.setChildren(toOptions(byParent.getOrDefault(m.getId(), new ArrayList<>()), byParent));
            out.add(o);
        }
        return out;
    }

    public MenuVO formData(Long id) {
        getById(id);
        return findVo(voTree(null, true), id);
    }

    private MenuVO findVo(List<MenuVO> tree, Long id) {
        for (MenuVO v : tree) {
            if (String.valueOf(id).equals(v.getId())) {
                return v;
            }
            MenuVO hit = findVo(v.getChildren(), id);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    public Long saveForm(MenuForm f) {
        return save(toEntity(f, null));
    }

    public void updateForm(MenuForm f) {
        SysMenu m = toEntity(f, Long.valueOf(f.getId()));
        update(m);
    }

    private SysMenu toEntity(MenuForm f, Long id) {
        SysMenu m = new SysMenu();
        m.setId(id);
        m.setParentId(f.getParentId() == null || f.getParentId().isBlank() ? 0L : Long.valueOf(f.getParentId()));
        m.setType(f.getType());
        m.setName(f.getName());
        m.setRouteName(f.getRouteName());
        // path 与 routePath 任一非空即取（前端两字段同源）
        String p = (f.getPath() != null && !f.getPath().isBlank()) ? f.getPath() : f.getRoutePath();
        m.setPath(p);
        m.setComponent(f.getComponent());
        m.setPerm(f.getPerm());
        m.setIcon(f.getIcon());
        m.setSortValue(f.getSort());
        m.setVisible(f.getVisible() == null ? 1 : f.getVisible());
        m.setStatus(1);
        m.setRedirect(f.getRedirect());
        m.setKeepAlive(parseKeepAlive(f.getKeepAlive()));
        m.setExternalUrl(f.getExternalUrl());
        return m;
    }

    private Integer parseKeepAlive(Object v) {
        if (v == null) {
            return 1;
        }
        if (v instanceof Boolean b) {
            return b ? 1 : 0;
        }
        return Boolean.parseBoolean(String.valueOf(v)) ? 1 : 0;
    }

    /**
     * 动态路由：仅启用(status=1)的目录/菜单/外链（按钮不参与）。
     * @param authorizedIds 被授权的菜单/按钮 id 集合；null=不限制（超管）。
     *                       传入时保留授权节点及其祖先目录；隐藏但被授权的页面也保留（可路由、不入菜单）。
     */
    public List<RouteDTO> buildRoutes(List<Long> authorizedIds) {
        List<SysMenu> menus = loadRouters(1);
        if (authorizedIds != null) {
            menus = retainAuthorizedWithAncestors(menus, authorizedIds);
        }
        Map<Long, List<SysMenu>> byParent = groupByParent(menus);
        return byParent.getOrDefault(0L, new ArrayList<>()).stream()
                .map(m -> toRoute(m, byParent))
                .collect(Collectors.toList());
    }

    /** 保留被授权菜单 + 其全部祖先目录（按钮 id 不直接在路由里，但授权按钮说明其父菜单可见） */
    private List<SysMenu> retainAuthorizedWithAncestors(List<SysMenu> all, List<Long> authorizedIds) {
        Set<Long> keep = new java.util.HashSet<>();
        Map<Long, SysMenu> byId = all.stream().collect(Collectors.toMap(SysMenu::getId, m -> m, (a, b) -> a));
        // 按钮→父菜单；先把授权 id 映射到路由节点
        List<SysMenu> raw = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .in(SysMenu::getId, authorizedIds));
        for (SysMenu m : raw) {
            SysMenu cur = m;
            while (cur != null) {
                if (T_BUTTON.equals(cur.getType())) {
                    cur = byId.get(cur.getParentId());
                    continue;
                }
                keep.add(cur.getId());
                Long pid = cur.getParentId();
                if (pid == null || pid == 0L) {
                    break;
                }
                cur = byId.get(pid);
            }
        }
        return all.stream().filter(m -> keep.contains(m.getId())).collect(Collectors.toList());
    }

    private List<SysMenu> loadRouters(Integer status) {
        List<SysMenu> list = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .eq(status != null, SysMenu::getStatus, status)
                .in(SysMenu::getType, ROUTER_TYPES));
        return sort(list);
    }

    private Map<Long, List<SysMenu>> groupByParent(List<SysMenu> menus) {
        return menus.stream().collect(Collectors.groupingBy(
                m -> m.getParentId() == null ? 0L : m.getParentId(),
                LinkedHashMap::new, Collectors.toList()));
    }

    private List<SysMenu> sort(List<SysMenu> list) {
        list.sort(Comparator
                .comparing((SysMenu m) -> m.getSortValue() == null ? 0 : m.getSortValue())
                .thenComparing(SysMenu::getId));
        return list;
    }

    private List<MenuNode> toNodes(List<SysMenu> level, Map<Long, List<SysMenu>> byParent) {
        List<MenuNode> nodes = new ArrayList<>();
        for (SysMenu m : level) {
            MenuNode n = new MenuNode();
            n.setId(m.getId());
            n.setParentId(m.getParentId());
            n.setType(m.getType());
            n.setName(m.getName());
            n.setRouteName(m.getRouteName());
            n.setPath(m.getPath());
            n.setComponent(m.getComponent());
            n.setPerm(m.getPerm());
            n.setIcon(m.getIcon());
            n.setSort(m.getSortValue());
            n.setVisible(m.getVisible());
            n.setStatus(m.getStatus());
            n.setRedirect(m.getRedirect());
            n.setKeepAlive(m.getKeepAlive());
            n.setExternalUrl(m.getExternalUrl());
            n.setChildren(toNodes(byParent.getOrDefault(m.getId(), new ArrayList<>()), byParent));
            nodes.add(n);
        }
        return nodes;
    }

    /** 实体 → 路由节点（目录 component=Layout），按内存树递归 */
    private RouteDTO toRoute(SysMenu m, Map<Long, List<SysMenu>> byParent) {
        RouteDTO r = new RouteDTO();
        r.setPath(m.getPath());
        r.setName(blankToNull(m.getRouteName()));
        if (T_EXTLINK.equals(m.getType())) {
            r.setComponent(null);
        } else if (T_CATALOG.equals(m.getType())) {
            r.setComponent(LAYOUT);
        } else {
            r.setComponent(m.getComponent());
        }
        r.setRedirect(m.getRedirect());

        RouteDTO.Meta meta = new RouteDTO.Meta();
        meta.setTitle(m.getName());
        meta.setIcon(blankToNull(m.getIcon()));
        meta.setHidden(m.getVisible() != null && m.getVisible() == 0);
        meta.setKeepAlive(m.getKeepAlive() == null || m.getKeepAlive() == 1);
        meta.setExternalUrl(blankToNull(m.getExternalUrl()));
        r.setMeta(meta);

        List<SysMenu> children = byParent.getOrDefault(m.getId(), new ArrayList<>());
        if (!children.isEmpty()) {
            r.setChildren(children.stream().map(c -> toRoute(c, byParent)).collect(Collectors.toList()));
        }
        return r;
    }

    public SysMenu getById(Long id) {
        SysMenu m = menuMapper.selectById(id);
        if (m == null) {
            throw BizException.validFail("菜单不存在");
        }
        return m;
    }

    public Long save(SysMenu menu) {
        validate(menu);
        menu.setId(null);
        if (menu.getStatus() == null) {
            menu.setStatus(1);
        }
        if (menu.getVisible() == null) {
            menu.setVisible(1);
        }
        if (menu.getKeepAlive() == null) {
            menu.setKeepAlive(1);
        }
        menuMapper.insert(menu);
        return menu.getId();
    }

    public void update(SysMenu menu) {
        if (menu.getId() == null) {
            throw BizException.validFail("缺少菜单ID");
        }
        getById(menu.getId());
        validate(menu);
        if (menu.getParentId() != null && menu.getParentId().equals(menu.getId())) {
            throw BizException.validFail("上级菜单不能选择自己");
        }
        menuMapper.updateById(menu);
    }

    public void delete(Long id) {
        Long childCount = menuMapper.selectCount(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw BizException.validFail("存在子菜单或按钮，请先删除子项");
        }
        menuMapper.deleteById(id);
    }

    private void validate(SysMenu m) {
        if (m.getType() == null) {
            throw BizException.validFail("菜单类型不能为空");
        }
        if (T_BUTTON.equals(m.getType())) {
            if (m.getPerm() == null || m.getPerm().isBlank()) {
                throw BizException.validFail("按钮权限标识不能为空");
            }
            if (m.getParentId() == null || m.getParentId() == 0L) {
                throw BizException.validFail("按钮必须挂在某个菜单下");
            }
        } else if (m.getName() == null || m.getName().isBlank()) {
            throw BizException.validFail("菜单名称不能为空");
        }
        if (m.getParentId() == null) {
            m.setParentId(0L);
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /** 管理端菜单树节点 VO */
    @Data
    public static class MenuNode {
        private Long id;
        private Long parentId;
        private String type;
        private String name;
        private String routeName;
        private String path;
        private String component;
        private String perm;
        private String icon;
        private Integer sort;
        private Integer visible;
        private Integer status;
        private String redirect;
        private Integer keepAlive;
        private String externalUrl;
        private List<MenuNode> children;
    }
}
