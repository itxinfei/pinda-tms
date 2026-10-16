import request from "@/utils/request";

/**
 * 地图配置（xf-web-manager BFF 下发）
 *
 * 百度地图 AK 属第三方凭证，禁止写进前端环境变量/仓库；
 * 登录态下经网关从 BFF 获取（GlobalResponseAdvice 统一壳，拦截器解包）。
 */
const MAP_CONFIG_URL = "/api/manager/map-config-manager";

const MapConfigAPI = {
  /** 获取百度地图 AK（未配置返回空串，页面据此提示） */
  get() {
    return request<unknown, { ak: string }>({
      url: MAP_CONFIG_URL,
      method: "get",
    });
  },
};

export default MapConfigAPI;
