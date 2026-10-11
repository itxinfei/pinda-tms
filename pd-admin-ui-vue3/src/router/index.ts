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

/**
 * 未开发页面：路由保留（深链不 404），hidden 不入菜单，页面显示"开发中"
 */
const pendingRoute = (
  path: string,
  name: string,
  title: string,
  icon: string
): RouteRecordRaw => ({
  path,
  name,
  component: () => import("@/views/placeholder/index.vue"),
  meta: { title, icon, hidden: true },
});

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

  // ↓↓↓ 业务菜单：按「工作台 / 订单中心 / 调度中心 / 运输在途 / 基础档案 / 系统管理」分组。
  // 子路由 path 一律写成绝对路径，菜单层级变了但 URL 不变，旧书签与深链照旧可用。
  // 定案 D-42 落地（后端 sys_menu + getRouter 下发）后，本表随静态菜单一并下线。
  {
    path: "/workbench",
    name: "Workbench",
    component: Layout,
    redirect: "/dashboard",
    meta: { title: "工作台", icon: "homepage" },
    children: [
      {
        path: "/dashboard",
        name: "Dashboard",
        component: () => import("@/views/dashboard/index.vue"),
        meta: { title: "仪表盘", icon: "homepage", affix: true, keepAlive: true },
      },
    ],
  },

  {
    path: "/order-center",
    name: "OrderCenter",
    component: Layout,
    redirect: "/order",
    meta: { title: "订单中心", icon: "document" },
    children: [
      {
        path: "/order",
        name: "Order",
        component: () => import("@/views/order/index.vue"),
        meta: { title: "订单管理", icon: "document", keepAlive: true },
      },
      {
        path: "/transport",
        name: "Transport",
        component: () => import("@/views/transport/index.vue"),
        meta: { title: "运单管理", icon: "el-icon-Tickets", keepAlive: true },
      },
    ],
  },

  {
    path: "/dispatch-center",
    name: "DispatchCenter",
    component: Layout,
    redirect: "/task",
    meta: { title: "调度中心", icon: "el-icon-Operation" },
    children: [
      {
        path: "/task",
        name: "Task",
        component: () => import("@/views/task/index.vue"),
        meta: { title: "运输任务", icon: "el-icon-Odometer", keepAlive: true },
      },
      {
        path: "/pickup-dispatch",
        name: "PickupDispatch",
        component: () => import("@/views/pickup-dispatch/index.vue"),
        meta: { title: "取派件任务", icon: "scan", keepAlive: true },
      },
      {
        path: "/driver-job",
        name: "DriverJob",
        component: () => import("@/views/driver-job/index.vue"),
        meta: { title: "司机作业单", icon: "role", keepAlive: true },
      },
      pendingRoute("/dispatch", "Dispatch", "调度一张图", "position"),
    ],
  },

  {
    path: "/transit",
    name: "TransitCenter",
    component: Layout,
    redirect: "/monitor",
    // 子页全部未开发：整组不入菜单，避免出现"点开只有一个建设中"的空分组；页面上线后去掉 hidden
    meta: { title: "运输在途", icon: "monitor", hidden: true },
    children: [
      pendingRoute("/monitor", "Monitor", "在途监控", "location"),
      pendingRoute("/alarm", "Alarm", "告警中心", "warning"),
    ],
  },

  {
    path: "/archive",
    name: "ArchiveCenter",
    component: Layout,
    redirect: "/truck",
    meta: { title: "基础档案", icon: "file" },
    children: [
      {
        path: "/truck",
        name: "Truck",
        component: () => import("@/views/truck/index.vue"),
        meta: { title: "车辆管理", icon: "el-icon-Van", keepAlive: true },
      },
      {
        path: "/truck-type",
        name: "TruckType",
        component: () => import("@/views/truck-type/index.vue"),
        meta: { title: "车型管理", icon: "el-icon-Files", keepAlive: true },
      },
      {
        path: "/fleet",
        name: "Fleet",
        component: () => import("@/views/fleet/index.vue"),
        meta: { title: "车队管理", icon: "el-icon-OfficeBuilding", keepAlive: true },
      },
      {
        path: "/driver",
        name: "Driver",
        component: () => import("@/views/driver/index.vue"),
        meta: { title: "司机管理", icon: "el-icon-User", keepAlive: true },
      },
      {
        path: "/transport-line",
        name: "TransportLine",
        component: () => import("@/views/transport-line/index.vue"),
        meta: { title: "线路管理", icon: "el-icon-Guide", keepAlive: true },
      },
      {
        path: "/transport-line-type",
        name: "TransportLineType",
        component: () => import("@/views/transport-line-type/index.vue"),
        meta: { title: "线路类型", icon: "el-icon-Connection", keepAlive: true },
      },
      {
        path: "/goods-type",
        name: "GoodsType",
        component: () => import("@/views/goods-type/index.vue"),
        meta: { title: "货物类型", icon: "el-icon-Goods", keepAlive: true },
      },
    ],
  },

  {
    path: "/system",
    name: "System",
    component: Layout,
    redirect: "/org",
    meta: { title: "系统管理", icon: "setting" },
    children: [
      {
        path: "/org",
        name: "Org",
        component: () => import("@/views/system/org/index.vue"),
        meta: { title: "组织架构", icon: "tree", keepAlive: true },
      },
      {
        path: "/station",
        name: "Station",
        component: () => import("@/views/system/station/index.vue"),
        meta: { title: "岗位管理", icon: "group", keepAlive: true },
      },
      {
        path: "/area",
        name: "Area",
        component: () => import("@/views/system/area/index.vue"),
        meta: { title: "行政区划", icon: "cascader", keepAlive: true },
      },
      pendingRoute("/audit", "Audit", "审计日志", "document-copy"),
    ],
  },

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
