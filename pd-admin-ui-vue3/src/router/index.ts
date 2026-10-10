import type { App } from "vue";
import { createRouter, createWebHashHistory, type RouteRecordRaw } from "vue-router";

/**
 * 布局组件，供各路由复用（懒加载）
 */
export const Layout = () => import("@/layouts/index.vue");

/**
 * 布局外大屏路由：不套后台 Layout，整页铺满视口，菜单以站内外链形式新标签页打开
 */
export const screenRoutes: RouteRecordRaw[] = [
  {
    path: "/data-screen",
    name: "DataScreen",
    component: () => import("@/views/data-screen/index.vue"),
    meta: { hidden: true, title: "数据大屏" },
  },
];

// 静态路由
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
    component: () => import("@/views/dynamic-form/share.vue"),
    meta: { hidden: true, title: "表单填写" },
  },

  // 大屏演示页（独立成页，不经后台框架）
  ...screenRoutes,

  {
    path: "/",
    name: "/",
    component: Layout,
    redirect: "/dashboard",
    children: [
      {
        path: "dashboard",
        component: () => import("@/views/dashboard/index.vue"),
        name: "Dashboard",
        meta: {
          title: "仪表盘",
          icon: "homepage",
          affix: true,
          keepAlive: true,
        },
      },
      {
        path: "org",
        component: () => import("@/views/system/org/index.vue"),
        name: "Org",
        meta: {
          title: "组织架构",
          icon: "tree",
          keepAlive: true,
        },
      },
      {
        path: "station",
        component: () => import("@/views/system/station/index.vue"),
        name: "Station",
        meta: {
          title: "岗位管理",
          icon: "group",
          keepAlive: true,
        },
      },
      {
        path: "area",
        component: () => import("@/views/system/area/index.vue"),
        name: "Area",
        meta: {
          title: "行政区划",
          icon: "cascader",
          keepAlive: true,
        },
      },
      {
        path: "order",
        component: () => import("@/views/order/index.vue"),
        name: "Order",
        meta: {
          title: "订单管理",
          icon: "document",
          keepAlive: true,
        },
      },
      {
        path: "transport",
        component: () => import("@/views/placeholder/index.vue"),
        name: "Transport",
        meta: {
          title: "运单管理",
          icon: "train",
        },
      },
      {
        path: "task",
        component: () => import("@/views/placeholder/index.vue"),
        name: "Task",
        meta: {
          title: "运输任务",
          icon: "van",
        },
      },
      {
        path: "monitor",
        component: () => import("@/views/placeholder/index.vue"),
        name: "Monitor",
        meta: {
          title: "在途监控",
          icon: "location",
        },
      },
      {
        path: "dispatch",
        component: () => import("@/views/placeholder/index.vue"),
        name: "Dispatch",
        meta: {
          title: "调度一张图",
          icon: "position",
        },
      },
      {
        path: "truck",
        component: () => import("@/views/placeholder/index.vue"),
        name: "Truck",
        meta: {
          title: "车辆管理",
          icon: "truck",
        },
      },
      {
        path: "driver",
        component: () => import("@/views/placeholder/index.vue"),
        name: "Driver",
        meta: {
          title: "司机管理",
          icon: "user-filled",
        },
      },
      {
        path: "alarm",
        component: () => import("@/views/placeholder/index.vue"),
        name: "Alarm",
        meta: {
          title: "告警中心",
          icon: "warning",
        },
      },
      {
        path: "audit",
        component: () => import("@/views/placeholder/index.vue"),
        name: "Audit",
        meta: {
          title: "审计日志",
          icon: "document-copy",
        },
      },
      {
        path: "system",
        component: () => import("@/views/placeholder/index.vue"),
        name: "System",
        meta: {
          title: "系统管理",
          icon: "setting",
        },
      },
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
        meta: { title: "个人中心", icon: "user", hidden: true },
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
