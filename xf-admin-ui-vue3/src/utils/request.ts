import axios, { type InternalAxiosRequestConfig, type AxiosResponse } from "axios";
import qs from "qs";

import { ApiCodeEnum } from "@/enums/api";
import { appConfig } from "@/settings";
import { usePermissionStoreHook } from "@/stores/permission";
import { AuthStorage } from "@/utils/auth";
import type { ApiResult } from "@/api/common";
import { installTokenRefresh } from "@/utils/token-refresh";

// 403 提示的翻译函数由入口（main.ts）在 i18n 初始化后注入：本模块若静态依赖
// @/lang 会形成 request → lang → stores → request 循环依赖，rolldown 打包后
// 模块求值顺序改变，stores chunk 顶层拿到的 pinia 实例为 undefined 启动即崩（_s 报错）
let translate: (key: string) => string = (key) => key;

/** 注册请求层文案翻译函数（main.ts 在 setupI18n 后调用） */
export function registerRequestTranslator(fn: (key: string) => string) {
  translate = fn;
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE,
  timeout: 50000,
  headers: { "Content-Type": "application/json;charset=utf-8" },
  // 数组参数序列化为 ids=1&ids=2，而非 ids[]=1&ids=2
  paramsSerializer: (params) => qs.stringify(params, { arrayFormat: "repeat" }),
});

// 注入访问令牌
http.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (!config.anonymous) {
      const token = AuthStorage.getAccessToken();
      if (token) {
        // 心飞网关用 token 头，不是 Authorization: Bearer
        config.headers.token = token;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 令牌续期拦截器：响应拦截器按注册顺序执行，先于业务拦截器接管 401，业务拦截器只负责业务错误的提示
installTokenRefresh(http);

// 解包业务数据
http.interceptors.response.use(
  (response: AxiosResponse<ApiResult>): AxiosResponse | any => {
    const { responseType } = response.config;

    // 二进制数据直接透传
    if (responseType === "blob" || responseType === "arraybuffer") {
      return response;
    }

    const { code, data, msg } = response.data;

    // 兼容两种成功码：前端模板的 "00000" 和心飞的 0
    if (code === ApiCodeEnum.SUCCESS || code === 0) {
      return data;
    }

    ElMessage.error(msg || "系统出错");
    return Promise.reject(new Error(msg || "系统出错"));
  },

  async (error) => {
    // 令牌续期拦截器已终结的错误（Token Invalid 等）不再提示
    if (!axios.isAxiosError(error)) {
      return Promise.reject(error);
    }

    const { response } = error;

    if (!response) {
      ElMessage.error("网络连接失败");
      return Promise.reject(error);
    }

    const { code, msg } = response.data as ApiResult;

    // 令牌失效：透传原始响应给续期拦截器处理，不在此提示
    if (code === ApiCodeEnum.ACCESS_TOKEN_INVALID || code === ApiCodeEnum.REFRESH_TOKEN_INVALID) {
      return Promise.reject(error);
    }

    // 端级角色闸拒绝（HTTP 403，如非 ADMIN 账号登录管理端）：引导切换管理员账号
    if (response.status === 403) {
      const message = translate("request.forbidden");
      ElMessage.error(message);
      return Promise.reject(new Error(message));
    }

    // 权限不足：D-42 权限下发未落地前无真实用户信息端点可刷新，跳过（避免再发必 404 请求）
    if (code === ApiCodeEnum.PERMISSION_DENIED) {
      if (appConfig.userInfoEndpointEnabled) {
        const permissionStore = usePermissionStoreHook();
        await permissionStore.refreshPermissions();
      }
      ElMessage.error(msg || "权限不足");
      return Promise.reject(new Error(msg || "权限不足"));
    }

    ElMessage.error(msg || "请求失败");
    return Promise.reject(new Error(msg || "请求失败"));
  }
);

export default http;
