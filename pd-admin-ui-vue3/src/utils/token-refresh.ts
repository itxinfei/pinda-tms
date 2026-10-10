import type { AxiosError, AxiosInstance } from "axios";

import { useUserStoreHook } from "@/stores/user";

/** HTTP 未授权状态码 */
const HTTP_UNAUTHORIZED = 401;

/**
 * 注册令牌失效拦截器
 *
 * 品达无 refreshToken：收到 401 一律清状态并跳登录页，不再尝试自动续期。
 */
export function installTokenRefresh(http: AxiosInstance) {
  http.interceptors.response.use(undefined, async (error: AxiosError) => {
    if (error.response?.status === HTTP_UNAUTHORIZED) {
      await useUserStoreHook().redirectToLogin("expired");
      return Promise.reject(new Error("Token Invalid"));
    }

    // 其它错误交回基础拦截器处理
    return Promise.reject(error);
  });
}
