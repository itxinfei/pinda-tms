# CHANGELOG

格式：日期 | 模块 | 更新内容 | 开发者

---

2026-07-23 | pd-admin-ui（前端安全/P0） | 修复 8 项安全问题：① `utils/request.js` 移除硬编码 Basic Auth 凭据 `febc:123456`，改由 `.env` 的 `VUE_APP_CLIENT_ID`/`VUE_APP_CLIENT_SECRET` 注入；② `settings.js` 移除硬编码生产 IP，Druid/onlinePreview 地址改由 `VUE_APP_DRUID_AUTHORITY_PROD`/`VUE_APP_DRUID_FILE_PROD`/`VUE_APP_ONLINE_PREVIEW` 注入；③ `utils/index.js` 的 `html2Text` 由 `innerHTML` 改为 `DOMParser` 解析（防 XSS）；④ `views/pinda/sms/manage/Edit.vue` 短信预览 `v-html` 改为文本插值（防 XSS）；⑤ `api/Common.js` 的 `loadImg` Token 由 URL 参数改为请求头 + Blob URL；⑥ `utils/localstorage.js` 与 `router/index.js` 增加 `try-catch` 异常保护；⑦ `store/modules/tagsView.js` 修复 `UPDATE_VISITED_VIEW` mutation 静默失效（改用 splice 响应式更新）。 | 开发者：心飞为你飞

2026-07-23 | pd-admin-ui（构建产物清理） | 移除提交至版本库的旧构建产物 `pinda/`（176 个文件，含旧凭据/IP 的打包 JS），加入 `.gitignore` 并通过 `git rm --cached` + 磁盘删除从跟踪与本地移除；重新 `npm run build` 可重新生成。同时清理 `.env.development` 中残留的注释旧 IP 行。 | 开发者：心飞为你飞

2026-07-23 | pd-admin-ui（P1 架构整改·批次B） | 修复 `window.onresize = fn` 覆盖式赋值导致的系统性 resize 监听互相覆盖、多图表/多弹窗 resize 失效问题（原 24 处）。新增 `src/utils/resize.js`，以 WeakMap 注册表集中管理每个组件实例的 resize 监听（`bindResize(vm, fn)` / `unbindResize(vm)`）；`main.js` 注册 `Vue.prototype.$bindResize` 并加全局 `beforeDestroy` mixin 自动解绑，避免监听器泄漏。将原 22 处 `window.onresize = () => {...}` 改为 `this.$bindResize(() => {...})`，将原 2 处 ECharts 的 `window.onresize = myChart.resize` 改为 `this.$bindResize(() => myChart.resize())`。 | 开发者：心飞为你飞
