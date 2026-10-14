import { useRoute, type RouteRecordRaw } from "vue-router";
import path from "path-browserify";
import { useAppStore, usePermissionStore, useSettingsStore } from "@/stores";
import { DeviceEnum } from "@/enums/settings";
import { defaults } from "@/settings";

const isVisibleRoute = (route: RouteRecordRaw) => !route.meta?.hidden;

/**
 * 拍平「无标题根布局壳」。
 * 后端动态菜单常以一个 path 为 "/"、且无 meta.title 的根 Layout 包裹全部业务菜单；
 * 该壳只承担路由层的 Layout（sidebar + navbar + content），不应在侧边栏渲染成
 * el-sub-menu——否则展开态多出一个空的 el-sub-menu__title 行（logo 下方空白），
 * 折叠态还会把全部菜单收进一个多余弹层。
 *
 * 规则：仅拍平「无标题 + 有可见子项」的纯容器，子项提升为同级并补全绝对路径；
 * 带标题的真实目录分组（后端 sys_menu 配的 children 目录）原样保留，可正常折叠。
 */
function flattenSidebarRoutes(list: RouteRecordRaw[]): RouteRecordRaw[] {
  const result: RouteRecordRaw[] = [];
  for (const route of list) {
    const visibleChildren = (route.children ?? []).filter(isVisibleRoute);
    const isUntitledShell = !route.meta?.title && visibleChildren.length > 0;
    if (isUntitledShell) {
      const base = route.path || "/";
      for (const child of visibleChildren) {
        result.push({ ...child, path: path.resolve(base, child.path || "") });
      }
    } else if (isVisibleRoute(route)) {
      result.push(route);
    }
  }
  return result;
}

/**
 * 提供布局组件共用的响应式状态
 */
export function useLayout() {
  const route = useRoute();
  const appStore = useAppStore();
  const settingsStore = useSettingsStore();
  const permissionStore = usePermissionStore();

  const isMobile = computed(() => appStore.device === DeviceEnum.MOBILE);
  const currentLayout = computed(() => settingsStore.layout);
  const isSidebarOpen = computed(() => appStore.sidebar.opened);
  const showTagsView = computed(() => settingsStore.showTagsView);
  const showSettings = computed(() => defaults.showSettings);
  const showLogo = computed(() => settingsStore.showAppLogo);

  const layoutClass = computed(() => ({
    "is-sidebar-collapsed": !appStore.sidebar.opened,
    "is-sidebar-open": appStore.sidebar.opened,
    "is-mobile": appStore.device === DeviceEnum.MOBILE,
    [`layout--${settingsStore.layout}`]: true,
  }));

  // 左侧和顶部菜单共用的路由列表（拍平无标题根布局壳，避免空 el-sub-menu 标题行）
  const routes = computed(() => flattenSidebarRoutes(permissionStore.routes));

  // 混合布局中随顶部菜单切换的侧边菜单
  const sideMenuRoutes = computed(() => permissionStore.mixLayoutSideMenus);

  // 混合布局当前激活的顶部菜单路径
  const activeTopMenuPath = computed(() => appStore.activeTopMenuPath);

  // 当前侧边菜单激活路径
  const activeMenu = computed(() => {
    const { meta, path } = route;
    return meta?.activeMenu || path;
  });

  /**
   * 切换侧边栏展开状态
   */
  function toggleSidebar() {
    appStore.toggleSidebar();
  }

  /**
   * 收起侧边栏
   */
  function closeSidebar() {
    appStore.closeSidebar();
  }

  return {
    isMobile,
    currentLayout,
    layoutClass,
    isSidebarOpen,
    showTagsView,
    showSettings,
    showLogo,
    routes,
    sideMenuRoutes,
    activeMenu,
    activeTopMenuPath,
    toggleSidebar,
    closeSidebar,
  };
}
