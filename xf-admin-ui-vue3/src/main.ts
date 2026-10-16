import { createApp } from "vue";
import { ElMessage } from "element-plus";
import "element-plus/es/components/message/style/css";
import App from "./App.vue";

import "element-plus/theme-chalk/dark/css-vars.css";
import "@/styles/index.scss";
import "uno.css";
import "animate.css";

import { setupDirective } from "@/directives";
import { setupRouter } from "@/router";
import { setupStore } from "@/stores";
import { setupI18n } from "@/lang";
import { registerFormCreateApp } from "@/plugins/form-create";
import * as ElementPlusIcons from "@element-plus/icons-vue";
import { setupPermissionGuard } from "@/router/guards";
import { useSse } from "@/utils/sse";
import { useDictStoreHook } from "@/stores/dict";

const app = createApp(App);

// 全局错误兜底：组件渲染/生命周期异常不再只进 console；3 秒去重防止批量报错刷屏
let lastErrorShownAt = 0;
app.config.errorHandler = (err, _instance, info) => {
  console.error("[app:errorHandler]", info, err);
  const now = Date.now();
  if (now - lastErrorShownAt > 3000) {
    lastErrorShownAt = now;
    ElMessage.error("页面出现异常，请刷新重试；若反复出现请联系管理员");
  }
};

setupDirective(app);
setupI18n(app);
setupRouter(app);
setupStore(app);
// 只登记 app 引用；表单引擎（element-plus 全量 + form-create/设计器及其内置富文本）
// 体积大，首访 dynamic-form/workflow/分享页路由时经 loader 按需加载注册，不再压首屏入口
registerFormCreateApp(app);

Object.entries(ElementPlusIcons).forEach(([name, comp]) => app.component(name, comp));

setupPermissionGuard();

// 临时关掉：SSE 实时推送和字典同步（心飞后端暂无这些接口）
// useSse().connect();
// useDictStoreHook().setupDictSync();

app.mount("#app");
