import request from "@/utils/request";
import type { PageResult } from "@/api/common";
import type { CacheKeyItem, CacheKeyQueryParams } from "./types";

const CACHE_BASE_URL = "/api/v1/cache";

const CacheAPI = {
  /**
   * 缓存键分页（SCAN 匹配，单次上限 2000 条）
   */
  getPage(queryParams: CacheKeyQueryParams) {
    return request<unknown, PageResult<CacheKeyItem>>({
      url: `${CACHE_BASE_URL}`,
      method: "get",
      params: queryParams,
    });
  },

  /**
   * 删除单键（不可恢复，前端须二次确认）
   */
  delete(key: string) {
    return request<unknown, void>({
      url: `${CACHE_BASE_URL}`,
      method: "delete",
      params: { key },
    });
  },
};

export default CacheAPI;
export * from "./types";
