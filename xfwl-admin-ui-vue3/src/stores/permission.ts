import type { RouteRecordRaw } from "vue-router";
import { constantRoutes } from "@/router";
import { resolveComponent } from "@/router/views";
import { store } from "@/stores";
import router from "@/router";
import { useUserStoreHook } from "@/stores/user";
import { isExternal, joinRoutePath, resolveExternalUrl } from "@/utils";

import MenuAPI from "@/api/system/menu";
import type { RouteItem } from "@/api/system/menu";

const Layout = () => import("../layouts/index.vue");

export const usePermissionStore = defineStore("permission", () => {
  const routes = ref<RouteRecordRaw[]>(constantRoutes);
  const mixLayoutSideMenus = ref<RouteRecordRaw[]>([]);
  // D-42 动态菜单：初值 false，守卫在首次导航/刷新时经 generateRoutes 从后端拉取并注册
  const isRouteGenerated = ref(false);

  /**
   * 生成动态路由
   */
  async function generateRoutes(): Promise<RouteRecordRaw[]> {
    try {
      const routeData = await MenuAPI.getRoutes();
      // 「系统管理」置底 + 注入前端合成的「授权管理」入口（见 normalizeMenus）
      normalizeMenus(routeData);
      // 菜单直接平级铺开，不做前端归组（不生成 el-sub-menu / el-sub-menu__title 分组标题层）；
      // 后端若下发真实 children 树，buildRoutes 仍正常渲染层级。
      const menuRoutes = buildRoutes(routeData);
      const registerRoutes = filterRoutes(menuRoutes);

      routes.value = [...constantRoutes, ...menuRoutes];
      isRouteGenerated.value = true;

      return registerRoutes;
    } catch (error) {
      isRouteGenerated.value = false;
      throw error;
    }
  }

  /**
   * 设置混合布局左侧菜单
   */
  const setMixLayoutSideMenus = (parentPath: string) => {
    const parentMenu = routes.value.find((item: RouteRecordRaw) => item.path === parentPath);
    mixLayoutSideMenus.value = parentMenu?.children || [];
  };

  /**
   * 移除已注册的动态路由（静态路由保留）
   */
  const removeDynamicRoutes = (routeList: RouteRecordRaw[]) => {
    const constantNames = new Set(constantRoutes.map((route) => route.name).filter(Boolean));
    routeList.forEach((route) => {
      if (route.name && !constantNames.has(route.name)) {
        router.removeRoute(route.name);
      }
    });
  };

  /**
   * 重置路由状态
   */
  const resetRouter = () => {
    removeDynamicRoutes(routes.value);

    routes.value = [...constantRoutes];
    mixLayoutSideMenus.value = [];
    isRouteGenerated.value = false;
  };

  let pendingReload: Promise<RouteRecordRaw[]> | null = null;

  /**
   * 重新加载动态路由：并发复用同一请求，避免路由空窗
   */
  async function reloadRoutes(): Promise<RouteRecordRaw[]> {
    if (pendingReload) return pendingReload;

    pendingReload = (async () => {
      try {
        const staleRoutes = [...routes.value];
        const dynamicRoutes = await generateRoutes();

        removeDynamicRoutes(staleRoutes);
        dynamicRoutes.forEach((route: RouteRecordRaw) => {
          router.addRoute(route);
        });
        mixLayoutSideMenus.value = [];
        return dynamicRoutes;
      } finally {
        pendingReload = null;
      }
    })();

    return pendingReload;
  }

  let pendingPermissionRefresh: Promise<void> | null = null;

  /**
   * 刷新权限：重新拉取用户信息并重建动态路由
   */
  async function refreshPermissions(): Promise<void> {
    if (pendingPermissionRefresh) return pendingPermissionRefresh;

    pendingPermissionRefresh = (async () => {
      try {
        const userStore = useUserStoreHook();
        await userStore.getUserInfo();
        await reloadRoutes();
      } finally {
        pendingPermissionRefresh = null;
      }
    })();

    return pendingPermissionRefresh;
  }

  return {
    routes,
    mixLayoutSideMenus,
    isRouteGenerated,
    generateRoutes,
    setMixLayoutSideMenus,
    resetRouter,
    reloadRoutes,
    refreshPermissions,
  };
});

/**
 * 侧边栏菜单策略（2026-10-11 定案）：
 * 菜单一律按后端下发结构原样渲染，前端**不做归组**、不生成 el-sub-menu 分组标题层（el-sub-menu__title）。
 * 后端 sys_menu 若下发 children 树，buildRoutes 正常渲染真实层级；平级下发则平铺为 el-menu-item。
 * 需要业务分组时，应在后端 sys_menu 配目录层级，而不是前端硬编码映射。
 */

// 目录菜单 component 的占位值，标记该级只作路由容器
const LAYOUT_COMPONENT = "Layout";

/**
 * 顶层统一套 Layout：目录即容器，页面下沉为 path 为空的子路由
 */
const buildRoutes = (menus: RouteItem[]): RouteRecordRaw[] => menus.map(buildTopLevelRoute);

/** 「系统管理」目录标题（后端 sys_menu 下发的原样标题），置底与注入均按它匹配 */
const SYSTEM_MANAGE_TITLE = "系统管理";

/**
 * 顶层菜单整理（体验版授权，2026-10-19）：
 * 1. 「授权管理」入口由前端合成、注入「系统管理」目录末尾——授权校验是框架级能力，
 *    不依赖后端 sys_menu 配置与角色授权过滤，所有登录用户（含体验用户）均可见可操作；
 * 2. 「系统管理」强制置底，始终作为菜单栏最后一项；Array.sort 为稳定排序，其余菜单保持后端原序。
 */
function normalizeMenus(menus: RouteItem[]): void {
  const licenseEntry: RouteItem = {
    path: "license",
    name: "License",
    component: "system/license/index",
    meta: { title: "授权管理", icon: "el-icon-Key" },
    children: [],
  };
  menus.forEach((menu) => {
    if (menu.meta?.title === SYSTEM_MANAGE_TITLE) {
      menu.children = [
        ...(menu.children ?? []).filter((child) => child.name !== "License"),
        licenseEntry,
      ];
    }
  });
  menus.sort(
    (a, b) =>
      Number(a.meta?.title === SYSTEM_MANAGE_TITLE) -
      Number(b.meta?.title === SYSTEM_MANAGE_TITLE)
  );
}

/**
 * 套 Layout 壳承载顶层菜单，使页面具备侧边栏与顶栏
 */
function buildTopLevelRoute(menu: RouteItem): RouteRecordRaw {
  // 新标签页外链不注册路由，保留原始数据供侧边栏直接跳转
  const externalUrl = resolveMenuExternalUrl(menu);
  if (externalUrl) return toExternalRoute(menu, externalUrl);

  const path = joinRoutePath("", menu.path);

  const meta = { ...menu.meta };

  // 首页标签固定（对齐原静态 Dashboard 的 affix）：动态菜单契约不携带 affix，此处特判补齐
  if (menu.name === "Dashboard") meta.affix = true;

  // 目录：自身就是侧边栏分组，标题与图标保留在壳层
  if (isContainer(menu)) {
    return {
      path,
      name: path,
      component: Layout,
      meta,
      redirect: menu.redirect || firstVisiblePath(menu.children, path),
      children: buildChildRoutes(menu.children, path),
    };
  }

  // 页面：标题与图标下沉到子路由，避免面包屑与标签页多出一层
  return {
    path,
    name: path,
    component: Layout,
    meta: { hidden: meta.hidden },
    children: [
      {
        path: "",
        name: menu.name,
        component: resolveComponent(menu.component as string),
        meta,
        children: buildChildRoutes(menu.children, path),
      },
    ],
  };
}

/**
 * 子级菜单路由：目录保留为路径分组，页面挂载实际组件
 */
function buildChildRoutes(menus: RouteItem[] | undefined, basePath: string): RouteRecordRaw[] {
  return (menus ?? []).map((menu) => {
    const fullPath = joinRoutePath(basePath, menu.path);

    const externalUrl = resolveMenuExternalUrl(menu);
    if (externalUrl) return toExternalRoute(menu, externalUrl);

    if (isContainer(menu)) {
      const children = buildChildRoutes(menu.children, fullPath);
      return {
        path: menu.path ?? "",
        meta: { ...menu.meta },
        children,
        ...(children.length
          ? { redirect: menu.redirect || firstVisiblePath(menu.children, fullPath) }
          : {}),
      };
    }

    return {
      path: menu.path ?? "",
      name: menu.name,
      component: resolveComponent(menu.component as string),
      meta: { ...menu.meta },
    };
  });
}

/**
 * 新标签页外链：不注册路由，仅保留侧边栏跳转所需信息
 */
function toExternalRoute(menu: RouteItem, path: string): RouteRecordRaw {
  const { children: _children, ...rest } = menu;
  return { ...rest, path } as RouteRecordRaw;
}

/**
 * 外链地址：取 meta.externalUrl；存量数据的外链直接写在 path 上
 */
function resolveMenuExternalUrl(menu: RouteItem): string {
  if (menu.component) return "";

  const externalUrl = menu.meta?.externalUrl;
  if (externalUrl) return resolveExternalUrl(externalUrl);

  const path = menu.path ?? "";
  return isExternal(path) ? path : "";
}

/**
 * 目录菜单：没有页面组件，只作路由容器
 */
function isContainer(menu: RouteItem): boolean {
  return !menu.component || menu.component === LAYOUT_COMPONENT;
}

/**
 * 默认跳转：第一个非外链的可见子菜单完整路径
 */
function firstVisiblePath(menus: RouteItem[] | undefined, basePath: string): string | undefined {
  const first = (menus ?? []).find((menu) => !menu.meta?.hidden && !resolveMenuExternalUrl(menu));
  return first ? joinRoutePath(basePath, first.path) : undefined;
}

/**
 * 过滤掉新标签页外链，仅保留可注册的路由
 */
function filterRoutes(routes: RouteRecordRaw[]): RouteRecordRaw[] {
  return routes.reduce<RouteRecordRaw[]>((result, route) => {
    if (isExternal(route.path)) return result;

    const filtered = { ...route };
    const children = route.children ? filterRoutes(route.children) : [];

    if (children.length > 0) {
      filtered.children = children;
    } else {
      delete filtered.children;
    }

    result.push(filtered);
    return result;
  }, []);
}

/**
 * 非组件环境获取 permission store
 */
export function usePermissionStoreHook() {
  return usePermissionStore(store);
}
