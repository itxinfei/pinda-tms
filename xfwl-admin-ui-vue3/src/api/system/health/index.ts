import request from "@/utils/request";
import type { HealthItem, HealthOverview, HealthQueryParams } from "./types";

const HEALTH_BASE_URL = "/api/v1/health";

const HealthAPI = {
  /**
   * 服务健康列表（实时探测）
   */
  getList(queryParams: HealthQueryParams) {
    return request<unknown, HealthItem[]>({
      url: `${HEALTH_BASE_URL}`,
      method: "get",
      params: queryParams,
    });
  },

  /**
   * 健康概览（UP/DOWN 计数）
   */
  getOverview(queryParams: HealthQueryParams) {
    return request<unknown, HealthOverview>({
      url: `${HEALTH_BASE_URL}/overview`,
      method: "get",
      params: queryParams,
    });
  },
};

export default HealthAPI;
export * from "./types";
