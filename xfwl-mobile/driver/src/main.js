import { createSSRApp } from "vue";
import uviewPlus from "uview-plus";
import { createPinia } from "pinia";
import App from "./App.vue";

export function createApp() {
	const app = createSSRApp(App);
	// 状态管理
	app.use(createPinia());
	// uview-plus 组件库
	app.use(uviewPlus);
	return {
		app,
	};
}
