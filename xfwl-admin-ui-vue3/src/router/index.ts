import type { App } from "vue";
import { createRouter, createWebHashHistory, type RouteRecordRaw } from "vue-router";
import { ensureFormCreate } from "@/plugins/form-create";

/**
 * 布局组件，供各路由复用（懒加载）
 */
export const Layout = () => import("@/layouts/index.vue");

/**
 * 布局外大屏路由：不套后台 Layout，整页铺满视口，菜单以站内外链形式新标签页打开
 */
export const screenRoutes: RouteRecordRaw[] = [
  {
    path: "/dashboard/bigscreen",
    name: "BigScreen",
    component: () => import("@/views/bigscreen/index.vue"),
    meta: { hidden: true, title: "运营监控大屏" },
  },
];

// 静态路由：仅框架页（登录/重定向/错误/个人中心/表单分享/大屏）。
// 业务菜单自 D-42 落地起由后端 sys_menu 经 GET /menus/routes 动态下发
// （见 stores/permission.ts buildRoutes），URL 与原静态路由保持一致，旧书签与深链不受影响。
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: "/redirect",
    component: Layout,
    meta: { hidden: true },
    children: [
      {
        path: "/redirect/:path(.*)",
        component: () => import("@/views/redirect.vue"),
      },
    ],
  },

  {
    path: "/login",
    component: () => import("@/views/login/index.vue"),
    meta: { hidden: true },
  },

  // 公开表单分享页（匿名访问，守卫白名单，不套管理端 Layout）
  {
    path: "/f/:formKey",
    name: "FormShare",
    // 分享页是 form-create 渲染器消费方：loader 先完成引擎注册，再挂载视图
    component: async () => {
      await ensureFormCreate();
      return import("@/views/dynamic-form/share.vue");
    },
    meta: { hidden: true, title: "表单填写" },
  },

  // 大屏演示页（独立成页，不经后台框架）
  ...screenRoutes,

  // 无菜单入口的框架页与错误页
  {
    path: "/",
    name: "/",
    component: Layout,
    redirect: "/dashboard",
    meta: { hidden: true },
    children: [
      {
        path: "401",
        component: () => import("@/views/error/401.vue"),
        meta: { hidden: true },
      },
      {
        path: "404",
        component: () => import("@/views/error/404.vue"),
        meta: { hidden: true },
      },
      {
        path: "profile",
        name: "Profile",
        component: () => import("@/views/profile/index.vue"),
        meta: { title: "个人中心", icon: "el-icon-User", hidden: true },
      },
    ],
  },
];

/**
 * 创建路由
 */
const router = createRouter({
  history: createWebHashHistory(),
  routes: constantRoutes,
  // 刷新时，滚动条位置还原
  scrollBehavior: () => ({ left: 0, top: 0 }),
});

/**
 * 全局注册 router
 */
export function setupRouter(app: App<Element>) {
  app.use(router);
}

export default router;
