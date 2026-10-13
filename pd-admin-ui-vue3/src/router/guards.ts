import type { RouteRecordRaw } from "vue-router";
import NProgress from "@/plugins/nprogress";
import router from "@/router";
import { appConfig } from "@/settings";
import { usePermissionStore, useUserStore } from "@/stores";
import { useTenantStoreHook } from "@/stores/tenant";
import { isTenantEnabled } from "@/utils/tenant";

/**
 * D-42 落地前的临时登录身份（管理员全权限）。
 * 后端真实用户信息端点上线（appConfig.userInfoEndpointEnabled=true）后不再使用。
 */
const FALLBACK_USER_INFO = {
  nickname: "pinda",
  avatar: "",
  roles: ["ADMIN"],
  perms: ["*"],
};

/**
 * 路由守卫
 *
 * 处理登录验证、动态路由生成、404 检测、页面标题与进度条
 */
export function setupPermissionGuard() {
  // 白名单支持前缀匹配：/f 命中所有公开表单分享页 /f/:formKey
  const whiteList = ["/login", "/f"];

  router.beforeEach(async (to, _from) => {
    NProgress.start();

    try {
      const isLoggedIn = useUserStore().isLoggedIn();

      // 未登录处理
      if (!isLoggedIn) {
        const isWhiteListed = whiteList.some(
          (path) => to.path === path || to.path.startsWith(`${path}/`)
        );
        if (isWhiteListed) {
          return;
        }
        NProgress.done();
        return `/login?redirect=${encodeURIComponent(to.fullPath)}`;
      }

      // 已登录访问登录页，重定向到首页
      if (to.path === "/login") {
        return { path: "/" };
      }

      const permissionStore = usePermissionStore();
      const userStore = useUserStore();

      // 用户信息（按钮权限 v-hasPerm 与昵称头像的数据源）
      // 必须独立于动态路由开关：permission store 的 isRouteGenerated 初值即 true（临时走静态路由），
      // 放在它下面会导致 getUserInfo 永不调用、userInfo 恒为空、全站 v-hasPerm 按钮被摘除。
      // D-42（用户信息/菜单权限下发）未落地前后端无 /api/v1/users/me 端点：
      // 开关关闭时直接使用临时身份，不发"必 404"的请求（消除每次刷新的 404 红字）；
      // 绝不能因此重置会话——否则登录后立即被弹回登录页形成死循环。
      if (!userStore.userInfo?.roles?.length) {
        if (appConfig.userInfoEndpointEnabled) {
          try {
            await userStore.getUserInfo();
          } catch {
            console.warn("[guard] 用户信息端点不可用，降级使用临时 mock 用户身份");
            userStore.userInfo = { ...FALLBACK_USER_INFO } as any;
          }
        } else {
          // D-42 未落地：直接用临时管理员身份，无网络请求
          userStore.userInfo = { ...FALLBACK_USER_INFO } as any;
        }
      }

      // 动态路由生成（定案 D-42 落地后恢复：改为 !isRouteGenerated 时 generateRoutes()）
      if (!permissionStore.isRouteGenerated) {
        permissionStore.isRouteGenerated = true;

        return { ...to, replace: true };
      }

      // 路由 404 检查
      if (to.matched.length === 0) {
        // 从登录页跳转且目标路径无效，回退首页（避免不同用户权限不同导致的 404）
        if (_from.path === "/login") {
          return { path: "/", replace: true };
        }
        return "/404";
      }

      // 动态标题
      const title = (to.params.title as string) || (to.query.title as string);
      if (title) {
        to.meta.title = title;
      }
    } catch (error) {
      console.error("Route guard error:", error);
      await useUserStore().resetAllState();
      NProgress.done();
      return "/login";
    }
  });

  router.afterEach(() => {
    NProgress.done();
  });
}

/**
 * 初始化多租户上下文，未启用或失败时静默跳过
 */
async function initTenantContext(): Promise<void> {
  if (!isTenantEnabled()) return;

  try {
    await useTenantStoreHook().loadTenant();
  } catch {
    // 静默失败，不影响主流程
  }
}
