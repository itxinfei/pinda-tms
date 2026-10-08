import Vue from 'vue'
import { Notification } from 'element-ui'

// 全局错误上抛：把原本"静默空白"的运行时错误，变成界面可见的通知。
// 这样以后再出现"菜单点了没反应/页面空白"，用户能直接看到报错信息，而不是瞎猜。

let lastMsg = ''
let lastTs = 0

function showError(err, info) {
  try {
    const text = (err && (err.message || err.stack)) || String(err) || '未知错误'
    const now = Date.now()
    // 同质错误 4 秒内只提示一次，避免刷屏
    if (text === lastMsg && now - lastTs < 4000) return
    lastMsg = text
    lastTs = now
    Notification({
      title: '前端运行时错误',
      message: (info ? info + '：' : '') + text.slice(0, 240),
      type: 'error',
      duration: 0
    })
  } catch (e) {
    // 兜底：至少打到控制台
    console.error('[global-error-handler] 展示失败', e)
  }
}

export function installGlobalErrorHandler() {
  // Vue 渲染 / 生命周期钩子里的错误
  Vue.config.errorHandler = function (err, vm, info) {
    console.error('[Vue error]', err, info)
    showError(err, info)
  }
  // 资源加载 / 脚本错误
  window.onerror = function (message, source, lineno, colno, error) {
    console.error('[window.onerror]', message, source, lineno, colno, error)
    showError(error || message, 'window.onerror')
    return false
  }
  // Promise 未捕获 rejection（异步请求/动态 import 失败常见来源）
  window.addEventListener('unhandledrejection', function (event) {
    const reason = event.reason
    console.error('[unhandledrejection]', reason)
    showError(reason, 'unhandledrejection')
  })
}
