import Vue from 'vue'
import Router from 'vue-router'
import Layout from '@/layout'
import db from '@/utils/localstorage'
import store from '@/store/index'
import loginApi from '@/api/Login.js'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { staticMenu } from './menu'
Vue.use(Router)

const constRouter = [
  {
    path: '/redirect',
    component: Layout,
    redirect: '/redirect',
    hidden: true,
    children: [
      {
        path: '/redirect/:path*',
        component: () => import('@/views/redirect/index')
      }
    ]
  },
  {
    path: '/404',
    component: () => import('@/views/error-page/404'),
    hidden: true
  },
  {
    path: '/login',
    name: '登录页',
    component: () => import('@/views/login1/index')
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        component: () => import('@/views/dashboard/index'),
        name: 'Dashboard',
        meta: { title: 'dashboard', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '/profile',
    component: Layout,
    redirect: '/profile/index',
    hidden: true,
    children: [
      {
        path: 'index',
        component: () => import('@/views/profile/index'),
        name: 'Profile',
        meta: { title: 'profile', icon: 'user', noCache: true }
      },
      {
        hidden: true,
        path: '/sms/manage/edit',
        component: () => import('@/views/pinda/sms/manage/Edit'),
        name: 'smsEdit',
        meta: {
          title: '发送短信', icon: '', noCache: true
        }
      }
    ]
  },
  {
    path: '/error',
    component: Layout,
    redirect: 'noRedirect',
    name: 'ErrorPages',
    meta: {
      title: 'errorPages',
      icon: '404'
    },
    children: [
      {
        path: '404',
        component: () => import('@/views/error-page/404'),
        name: 'Page404',
        meta: { title: 'page404', noCache: true }
      }
    ]
  },
  {
    path: '/myiframe',
    component: Layout,
    redirect: '/myiframe',
    children: [{
      path: ":routerPath",
      name: 'iframe',
      component: () => import('@/views/iframe/index'),
      props: true
    }]

  }

]

const router = new Router({
  scrollBehavior: () => ({ y: 0 }),
  routes: constRouter
})
const whiteList = ['/login']

let asyncRouter

// 守卫诊断: 生产构建会删除console.log, 改用全局数组记录
function glog (msg) {
  try {
    (window.__guardLog = window.__guardLog || []).push(String(msg))
  } catch (e) {}
}

// 导航守卫，渲染动态路由
router.beforeEach((to, from, next) => {
  NProgress.start()
  if (whiteList.indexOf(to.path) !== -1) {
    next()
  } else {
    const token = db.get('TOKEN')
    const user = db.get('USER')
    const userRouter = get('USER_ROUTER')
    glog('path=' + to.path + ' token=' + (token && token.length ? 'Y' : 'N') + ' user=' + (user ? 'Y' : 'N') + ' asyncRouter=' + (asyncRouter ? 'Y' : 'N') + ' userRouter=' + (userRouter ? userRouter.length : 'null'))
    if (token && token.length && user) {
      if (!asyncRouter) {
        // 2026-10-06 重构: 静态菜单直出。不再依赖后端 menu/router 接口 + localStorage 缓存,
        // 彻底解决浏览器缓存旧JS/空缓存/接口抖动导致的侧边栏空白。开发测试环境固定全量菜单。
        glog('static menu render len=' + staticMenu.length)
        asyncRouter = staticMenu
        store.commit('account/setRoutes', asyncRouter)
        save('USER_ROUTER', asyncRouter)
        go(to, next)
      } else {
        glog('asyncRouter already set -> next')
        next()
      }
    } else {
      if (to.path === '/login') {
        next()
      } else {
        next('/login')
      }
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})

function go(to, next) {
  asyncRouter = filterAsyncRouter(asyncRouter)
  router.addRoutes(asyncRouter)
  next({ ...to, replace: true })
}

function save(name, data) {
  localStorage.setItem(name, JSON.stringify(data))
}

// 修改点：读取本地持久化路由时增加 try-catch，避免脏数据导致解析异常
function get(name) {
  const item = localStorage.getItem(name)
  if (item === null || item === undefined) {
    return null
  }
  try {
    return JSON.parse(item)
  } catch (e) {
    console.error('router local cache parse error for key:', name, e)
    return null
  }
}

function filterAsyncRouter(routes) {
  return routes.filter((route) => {
    const component = route.component
    if (component) {
      // 2026-10-06 修复：后端返回的菜单可能缺 meta。FEBS 侧边栏 SidebarItem 依赖
      // meta.title 渲染——叶子菜单外层有 v-if="onlyOneChild.meta"，缺 meta 会整个
      // 不渲染，父级 el-submenu 也因 v-if="item.meta" 没有标题，最终登录成功但左侧
      // 菜单空白。此处兜底，用菜单 name 作为标题构造 meta。
      if (!route.meta) {
        route.meta = { title: route.name, icon: route.icon || '' }
      }
      if (route.component === 'Layout') {
        route.component = Layout
      } else {
        route.component = view(component)
      }
      if (route.children && route.children.length) {
        route.children = filterAsyncRouter(route.children)
      }
      return true
    }
  })
}

function view(path) {
  return function (resolve) {
    import(`@/views/${path}.vue`).then(mod => {
      resolve(mod)
    })
  }
}

export default router
