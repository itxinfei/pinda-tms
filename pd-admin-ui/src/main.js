import Vue from 'vue'

import 'normalize.css/normalize.css' // a modern alternative to CSS resets
import Element from 'element-ui'
import './styles/element-variables.scss'

import '@/styles/index.scss' // global css

import App from './App'
import store from './store'
import router from './router'

import i18n from './lang' // internationalization
import './icons' // icon
import './utils/error-log' // error log
import request from '@/utils/request'
import { bindResize, unbindResize } from '@/utils/resize'

import * as filters from './filters' // global filters
import { hasPermission, hasNoPermission, hasAnyPermission } from './utils/permissionDirect'


const Plugins = [
  hasPermission,
  hasNoPermission,
  hasAnyPermission
]

Plugins.map((plugin) => {
  Vue.use(plugin)
})

Vue.use(Element, {
  i18n: (key, value) => i18n.t(key, value)
})

Vue.prototype.$post = request.post
Vue.prototype.$get = request.get
Vue.prototype.$put = request.put
Vue.prototype.$delete = request.delete
Vue.prototype.$download = request.download
Vue.prototype.$upload = request.upload
Vue.prototype.$login = request.login
Vue.prototype.$bindResize = bindResize

// 修改点：全局 mixin，组件销毁时自动解绑其注册的所有 resize 监听，
// 与 src/utils/resize.js 的 WeakMap 注册表配合，避免监听器泄漏。
Vue.mixin({
  beforeDestroy() {
    unbindResize(this)
  }
})

// register global utility filters
Object.keys(filters).forEach(key => {
  Vue.filter(key, filters[key])
})

Vue.config.productionTip = false

new Vue({
  el: '#app',
  router,
  store,
  i18n,
  render: h => h(App)
})
