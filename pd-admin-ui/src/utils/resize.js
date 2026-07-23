// 修改点：集中管理 window resize 监听，解决原 `window.onresize = fn` 直接覆盖全局处理器，
// 导致后挂载组件覆盖前者、多图表 resize 失效的问题；并在组件销毁时自动解绑，避免内存泄漏与已销毁图表被误调用。

const registry = new WeakMap()

/**
 * 为组件注册一个 resize 处理器（可多次调用，互不覆盖）
 * @param {Object} vm 组件实例（this）
 * @param {Function} fn 原 resize 回调
 */
export function bindResize (vm, fn) {
  if (!vm || typeof fn !== 'function') return
  const wrapped = () => {
    // 组件已销毁/失活则不执行，避免调用已 dispose 的图表
    if (vm._isDestroyed || vm._inactive) return
    fn()
  }
  const list = registry.get(vm) || []
  list.push(wrapped)
  registry.set(vm, list)
  window.addEventListener('resize', wrapped)
}

/**
 * 解绑某组件注册的全部 resize 处理器（通常由全局 mixin 在 beforeDestroy 调用）
 * @param {Object} vm 组件实例（this）
 */
export function unbindResize (vm) {
  const list = registry.get(vm)
  if (!list) return
  list.forEach(wrapped => window.removeEventListener('resize', wrapped))
  registry.delete(vm)
}
