// 静态菜单(开发测试环境直出方案)
// 2026-10-06 重构: 菜单不再依赖后端 menu/router 接口 + localStorage 缓存 + 动态拉取,
// 改为前端内置常量, 登录后守卫直接渲染。任何浏览器(含缓存残留/窄视口)刷新必然显示菜单。
// 注意: 静态菜单不按角色过滤(开发测试环境固定全量), 生产环境如需RBAC请改回动态拉取。
export const staticMenu = [
  {
    path: '/user',
    name: '系统管理',
    component: 'Layout',
    hidden: false,
    alwaysShow: true,
    meta: { title: '系统管理', icon: 'user' },
    children: [
      { path: '/user/org', name: '组织管理', component: 'pinda/user/org/Index', meta: { title: '组织管理', icon: 'tree' } },
      { path: '/user/station', name: '岗位管理', component: 'pinda/user/station/Index', meta: { title: '岗位管理', icon: 'office-building' } },
      { path: '/user/user', name: '用户管理', component: 'pinda/user/user/Index', meta: { title: '用户管理', icon: 'peoples' } },
      { path: '/user/menu', name: '菜单配置', component: 'pinda/auth/menu/Index', meta: { title: '菜单配置', icon: 'menu' } },
      { path: '/user/role', name: '角色管理', component: 'pinda/auth/role/Index', meta: { title: '角色管理', icon: 'people' } }
    ]
  },
  {
    path: '/developer',
    name: '监控管理',
    component: 'Layout',
    hidden: false,
    alwaysShow: true,
    meta: { title: '监控管理', icon: 'monitor' },
    children: [
      { path: '/developer/optLog', name: '操作日志', component: 'pinda/developer/optLog/Index', meta: { title: '操作日志', icon: 'documentation' } },
      { path: '/developer/loginLog', name: '登录日志', component: 'pinda/developer/loginLog/Index', meta: { title: '登录日志', icon: 'log' } },
      { path: '/developer/api', name: '接口文档', component: 'pinda/developer/systemApi/Index', meta: { title: '接口文档', icon: 'link' } },
      { path: '/developer/router', name: '接口路由', component: 'pinda/developer/Index', meta: { title: '接口路由', icon: 'route' } },
      { path: '/developer/application', name: '调用记录', component: 'pinda/developer/application/Index', meta: { title: '调用记录', icon: 'list' } },
      { path: '/developer/db', name: '请求记录', component: 'pinda/developer/db/Index', meta: { title: '请求记录', icon: 'database' } }
    ]
  },
  {
    path: '/ofpay',
    name: '支付管理',
    component: 'Layout',
    hidden: false,
    alwaysShow: true,
    meta: { title: '支付管理', icon: 'money' },
    children: [
      { path: '/ofpay/customer', name: '客户管理', component: 'pinda/ofpay/customer/Index', meta: { title: '客户管理', icon: 'customer' } },
      { path: '/ofpay/platform', name: '平台管理', component: 'pinda/ofpay/platform/Index', meta: { title: '平台管理', icon: 'platform' } },
      { path: '/ofpay/send', name: '发送管理', component: 'pinda/ofpay/send/Index', meta: { title: '发送管理', icon: 'send' } },
      { path: '/ofpay/receive', name: '接收管理', component: 'pinda/ofpay/receive/Index', meta: { title: '接收管理', icon: 'receive' } }
    ]
  }
]
