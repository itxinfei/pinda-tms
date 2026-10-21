// FormCreate 动态表单按需加载门面
// 表单引擎（element-plus 全量注册 + @form-create/element-ui + @form-create/designer，
// 内含 wangEditor/codemirror）体积大，不能静态挂在入口；本模块只保存 app 引用，
// 首次进入 dynamic-form/workflow 路由（含 /f/:formKey 分享页与未来菜单下发页）时，
// 由路由组件 loader 调 ensureFormCreate() 动态 import 安装模块并完成全局注册。
// 注册全局组件必须在页面渲染前完成，因此挂载点选在组件 loader（路由确认解析时执行），
// 而非页面 setup 内部。注册幂等：并发/多次调用只加载一次。

import type { App } from "vue";

let appRef: App | null = null;
let installing: Promise<void> | null = null;

/**
 * 入口调用：登记 app 引用（不产生任何加载）
 *
 * @param app Vue 应用实例
 */
export function registerFormCreateApp(app: App): void {
  appRef = app;
}

/**
 * 懒加载并注册表单引擎；已注册或注册中时复用同一 Promise
 */
export function ensureFormCreate(): Promise<void> {
  if (!installing) {
    const app = appRef;
    if (!app) {
      return Promise.reject(new Error("[form-create] app 未登记，registerFormCreateApp 必须先于路由调用"));
    }
    installing = import("./form-create-install").then((m) => m.installFormCreate(app));
  }
  return installing;
}
