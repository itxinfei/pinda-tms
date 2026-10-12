import type { LocationQueryRaw, RouteRecordRaw } from "vue-router";
import { useLayout } from "./useLayout";
import { useAppStore, usePermissionStore } from "@/stores";
import { isExternal } from "@/utils/index";

/**
 * 混合布局的顶部菜单与激活路径
 */
export function useMixMenu() {
  const route = useRoute();
  const router = useRouter();

  const appStore = useAppStore();
  const permissionStore = usePermissionStore();

  const { activeTopMenuPath, sideMenuRoutes } = useLayout();

  // 顶部菜单
  const topMenuItems = computed(() => permissionStore.routes.filter((item) => !item.meta?.hidden));

  // 解析当前侧边菜单激活路径
  const activeSideMenuPath = computed(() => {
    const { meta, path } = route;
    return typeof meta?.activeMenu === "string" ? meta.activeMenu : path;
  });

  /**
   * 解析混合布局侧边菜单路径
   *
   * 静态路由的子路由使用绝对路径（如 /order，2026-10-11 定案：菜单层级变了 URL 不变），
   * 动态后端菜单的子路由是相对路径。两类都要正确：
   * - 绝对路径原样返回（它本身就是注册路径，拼前缀反而 404）；
   * - 相对路径拼在当前激活的顶部分组下。
   */
  function resolvePath(routePath: string): string {
    if (isExternal(routePath)) return routePath;
    if (!routePath) return activeTopMenuPath.value;
    if (routePath.startsWith("/")) return routePath;
    return `${activeTopMenuPath.value}/${routePath}`;
  }

  /**
   * 从当前路由提取顶部菜单路径
   *
   * 优先取路由匹配链的第一段（顶层记录的真实 path，如 /order-center）；
   * 静态路由子路由是绝对路径（/order），按字符串首段切会错切出 /order。
   */
  function extractTopMenuPath(currentRoute: { matched: Array<{ path: string }>; path: string }): string {
    const topRecord = currentRoute.matched[0];
    if (topRecord?.path && topRecord.path !== "/") return topRecord.path;
    return "/" + (currentRoute.path.split("/").filter(Boolean)[0] ?? "");
  }

  /**
   * 切换顶部菜单并进入第一个可访问页面
   */
  function handleTopMenuSelect(menuPath: string) {
    if (menuPath === activeTopMenuPath.value) return;

    appStore.setActiveTopMenuPath(menuPath);
    permissionStore.setMixLayoutSideMenus(menuPath);
    navigateToFirstMenu(permissionStore.mixLayoutSideMenus);
  }

  /**
   * 进入菜单树中的第一个叶子页面
   */
  function navigateToFirstMenu(menus: RouteRecordRaw[]) {
    if (!menus.length) return;

    const [first] = menus;
    if (first.children?.length) {
      navigateToFirstMenu(first.children as RouteRecordRaw[]);
    } else if (first.name) {
      router.push({
        name: first.name,
        query:
          typeof first.meta?.params === "object"
            ? (first.meta.params as LocationQueryRaw)
            : undefined,
      });
    }
  }

  // 路由变化时保持顶部菜单和侧边菜单同步
  watch(
    () => route.path,
    () => {
      const topMenuPath = extractTopMenuPath(route);
      const isTopMenuChanged = topMenuPath !== activeTopMenuPath.value;

      if (isTopMenuChanged) {
        appStore.setActiveTopMenuPath(topMenuPath);
      }

      if (isTopMenuChanged || permissionStore.mixLayoutSideMenus.length === 0) {
        permissionStore.setMixLayoutSideMenus(topMenuPath);
      }
    },
    { immediate: true }
  );

  return {
    topMenuItems,
    activeSideMenuPath,
    activeTopMenuPath,
    sideMenuRoutes,
    resolvePath,
    handleTopMenuSelect,
  };
}
