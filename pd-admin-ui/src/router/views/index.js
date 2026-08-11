import Layout from '@/page/index/'

export default [{
  path: '/wel',
  component: Layout,
  redirect: '/wel/index',
  children: [{
    path: 'index',
    name: '首页',
    meta: {
      i18n: 'dashboard'
    },
    component: () =>
      import( /* webpackChunkName: "views" */ '@/views/wel/index')
  }, {
    path: 'dashboard',
    name: '控制台',
    meta: {
      i18n: 'dashboard',
      menu: false,
    },
    component: () =>
      import( /* webpackChunkName: "views" */ '@/views/wel/dashboard')
  }]
}, {
  path: '/test',
  component: Layout,
  redirect: '/test/index',
  children: [{
    path: 'index',
    name: '测试页',
    meta: {
      i18n: 'test'
    },
    component: () =>
      import( /* webpackChunkName: "views" */ '@/views/util/test')
  }]
}, {
  path: '/dict-horizontal',
  component: Layout,
  redirect: '/dict-horizontal/index',
  children: [{
    path: 'index',
    name: '字典管理',
    meta: {
      i18n: 'dict'
    },
    component: () =>
      import( /* webpackChunkName: "views" */ '@/views/util/demo/dict-horizontal')
  }]
}, {
  path: '/dict-vertical',
  component: Layout,
  redirect: '/dict-vertical/index',
  children: [{
    path: 'index',
    name: '字典管理',
    meta: {
      i18n: 'dict'
    },
    component: () =>
      import( /* webpackChunkName: "views" */ '@/views/util/demo/dict-vertical')
  }]
}, {
  path: '/info',
  component: Layout,
  redirect: '/info/index',
  children: [{
    path: 'index',
    name: '个人信息',
    meta: {
      i18n: 'info'
    },
    component: () =>
      import( /* webpackChunkName: "views" */ '@/views/system/userinfo')
  }]
}, {
  path: '/work/process/leave',
  component: Layout,
  redirect: '/work/process/leave/form',
  children: [{
    path: 'form/:processDefinitionId',
    name: '请假流程',
    meta: {
      i18n: 'work'
    },
    component: () =>
      import( /* webpackChunkName: "views" */ '@/views/work/process/leave/form')
  }, {
    path: 'handle/:taskId/:processInstanceId/:businessId',
    name: '处理请假流程',
    meta: {
      i18n: 'work'
    },
    component: () =>
      import( /* webpackChunkName: "views" */ '@/views/work/process/leave/handle')
  }, {
    path: 'detail/:processInstanceId/:businessId',
    name: '请假流程详情',
    meta: {
      i18n: 'work'
    },
    component: () =>
      import( /* webpackChunkName: "views" */ '@/views/work/process/leave/detail')
  }]
}, {
  path: '/pinda/trace',
  component: Layout,
  redirect: '/pinda/trace/index',
  children: [{
    path: 'index',
    name: '车辆轨迹监控',
    meta: {
      i18n: 'trace'
    },
    component: () =>
      import( /* webpackChunkName: "views" */ '@/views/pinda/trace/index')
  }]
}, {
  path: '/ofpay',
  component: Layout,
  redirect: '/ofpay/customer',
  children: [{
    path: 'index',
    name: '支付中心',
    meta: {
      i18n: 'ofpay'
    },
    component: () =>
      import( /* webpackChunkName: "views" */ '@/views/pinda/ofpay/index')
  }, {
    path: 'customer',
    name: '客户支付',
    meta: {
      i18n: 'ofpay'
    },
    component: () =>
      import( /* webpackChunkName: "views" */ '@/views/pinda/ofpay/customer/index')
  }, {
    path: 'platform',
    name: '平台支付',
    meta: {
      i18n: 'ofpay'
    },
    component: () =>
      import( /* webpackChunkName: "views" */ '@/views/pinda/ofpay/platform/index')
  }, {
    path: 'receive',
    name: '收款管理',
    meta: {
      i18n: 'ofpay'
    },
    component: () =>
      import( /* webpackChunkName: "views" */ '@/views/pinda/ofpay/receive/index')
  }, {
    path: 'send',
    name: '付款管理',
    meta: {
      i18n: 'ofpay'
    },
    component: () =>
      import( /* webpackChunkName: "views" */ '@/views/pinda/ofpay/send/index')
  }]
}]
