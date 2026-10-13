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
    path: "/dashboard/bigscreen",
    name: "BigScreen",
    component: () => import("@/views/bigscreen/index.vue"),
    meta: { hidden: true, title: "运营监控大屏" },
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

  // ↓↓↓ 业务菜单：按「仪表盘(首页) / 订单中心 / 调度中心 / 运输在途 / 基础档案 / 系统管理」分组。
  // 子路由 path 一律写成绝对路径，菜单层级变了但 URL 不变，旧书签与深链照旧可用。
  // 定案 D-42 落地（后端 sys_menu + getRouter 下发）后，本表随静态菜单一并下线。
  // 仪表盘（首页）：原"工作台"分组取消，仪表盘一级直挂，登录后打开即是首页。
  // 无标题父壳经 useLayout.flattenSidebarRoutes 拍平，菜单中仅呈现"仪表盘"一项。
  {
    path: "/dashboard",
    component: Layout,
    children: [
      {
        path: "",
        name: "Dashboard",
        component: () => import("@/views/dashboard/index.vue"),
        meta: { title: "仪表盘", icon: "el-icon-HomeFilled", affix: true, keepAlive: true },
      },
    ],
  },

  {
    path: "/order-center",
    name: "OrderCenter",
    component: Layout,
    redirect: "/order",
    meta: { title: "订单中心", icon: "el-icon-Document" },
    children: [
      {
        path: "/order",
        name: "Order",
        component: () => import("@/views/order/index.vue"),
        meta: { title: "订单管理", icon: "el-icon-Document", keepAlive: true },
      },
      {
        // 货物编辑（订单中心子页）：从订单列表「货物」或订单详情「管理货物」进入，hidden 不入菜单
        path: "/order/cargoEdit",
        name: "OrderCargoEdit",
        component: () => import("@/views/order/cargoEdit.vue"),
        meta: { title: "货物编辑", icon: "el-icon-Goods", hidden: true, keepAlive: true },
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
        meta: { title: "取派件任务", icon: "el-icon-Aim", keepAlive: true },
      },
      {
        path: "/driver-job",
        name: "DriverJob",
        component: () => import("@/views/driver-job/index.vue"),
        meta: { title: "司机作业单", icon: "el-icon-Avatar", keepAlive: true },
      },
      pendingRoute("/dispatch", "Dispatch", "调度一张图", "el-icon-LocationFilled"),
    ],
  },

  {
    path: "/transit",
    name: "TransitCenter",
    component: Layout,
    redirect: "/alarm",
    // 告警中心页面已建（TRK-02）入菜单；在途监控未开发仍 hidden，页面上线后去掉
    meta: { title: "运输在途", icon: "el-icon-Monitor" },
    children: [
      pendingRoute("/monitor", "Monitor", "在途监控", "el-icon-Location"),
      {
        path: "/alarm",
        name: "Alarm",
        component: () => import("@/views/alarm/index.vue"),
        meta: { title: "告警中心", icon: "el-icon-Warning", keepAlive: true },
      },
    ],
  },

  {
    path: "/archive",
    name: "ArchiveCenter",
    component: Layout,
    redirect: "/truck",
    meta: { title: "基础档案", icon: "el-icon-Files" },
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
      {
        // 会员管理（《55》USR-01）：页面/API/BFF 均已就绪，补静态菜单入口
        path: "/member",
        name: "Member",
        component: () => import("@/views/member/index.vue"),
        meta: { title: "会员管理", icon: "el-icon-UserFilled", keepAlive: true },
      },
    ],
  },

  {
    path: "/system",
    name: "System",
    component: Layout,
    // 首个可见子页是组织架构（用户/角色等 RBAC 页后端未就绪，已 hidden）
    redirect: "/org",
    meta: { title: "系统管理", icon: "el-icon-Setting" },
    children: [
      // —— 以下 RBAC/系统页后端尚未提供（新 pd-auth 无对应 CRUD，/api/v1 为模板残留 404）。
      // 路由与页面文件保留（《59》预置技术栈不删），暂 hidden 不入菜单，待后端落地后放开。 ——
      {
        path: "/user",
        name: "User",
        component: () => import("@/views/system/user/index.vue"),
        meta: { title: "用户管理", icon: "el-icon-User", keepAlive: true, hidden: true },
      },
      {
        path: "/role",
        name: "Role",
        component: () => import("@/views/system/role/index.vue"),
        meta: { title: "角色管理", icon: "el-icon-Stamp", keepAlive: true, hidden: true },
      },
      {
        path: "/menu",
        name: "Menu",
        component: () => import("@/views/system/menu/index.vue"),
        meta: { title: "菜单管理", icon: "el-icon-Menu", keepAlive: true, hidden: true },
      },
      {
        path: "/dept",
        name: "Dept",
        component: () => import("@/views/system/dept/index.vue"),
        meta: { title: "部门管理", icon: "el-icon-Grid", keepAlive: true, hidden: true },
      },
      {
        path: "/org",
        name: "Org",
        component: () => import("@/views/system/org/index.vue"),
        meta: { title: "组织架构", icon: "el-icon-Share", keepAlive: true },
      },
      {
        path: "/station",
        name: "Station",
        component: () => import("@/views/system/station/index.vue"),
        meta: { title: "岗位管理", icon: "el-icon-UserFilled", keepAlive: true },
      },
      {
        path: "/dict",
        name: "Dict",
        component: () => import("@/views/system/dict/index.vue"),
        meta: { title: "字典管理", icon: "el-icon-Notebook", keepAlive: true, hidden: true },
      },
      {
        path: "/config",
        name: "Config",
        component: () => import("@/views/system/config/index.vue"),
        meta: { title: "参数配置", icon: "el-icon-Tools", keepAlive: true },
      },
      {
        path: "/notice",
        name: "Notice",
        component: () => import("@/views/system/notice/index.vue"),
        meta: { title: "消息通知", icon: "el-icon-Bell", keepAlive: true, hidden: true },
      },
      {
        path: "/log",
        name: "Log",
        component: () => import("@/views/system/log/index.vue"),
        meta: { title: "操作日志", icon: "el-icon-DocumentCopy", keepAlive: true, hidden: true },
      },
      {
        path: "/tenant",
        name: "Tenant",
        component: () => import("@/views/system/tenant/index.vue"),
        meta: { title: "租户管理", icon: "el-icon-Briefcase", keepAlive: true, hidden: true },
      },
      {
        path: "/area",
        name: "Area",
        component: () => import("@/views/system/area/index.vue"),
        meta: { title: "行政区划", icon: "el-icon-MapLocation", keepAlive: true },
      },
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
