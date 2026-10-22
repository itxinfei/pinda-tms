import request from "@/utils/request";
import type { LogQueryParams, LogItem } from "./types";
import type { PageResult } from "@/api/common";

const LOG_BASE_URL = "/api/v1/logs";

const LogAPI = {
  /**
   * 获取日志分页列表
   */
  getPage(queryParams: LogQueryParams) {
    return request<unknown, PageResult<LogItem>>({
      url: `${LOG_BASE_URL}`,
      method: "get",
      params: queryParams,
    });
  },

  /**
   * 审计日志导出（D-30）：xlsx 二进制流
   */
  export(queryParams: LogQueryParams) {
    return request<unknown, Blob>({
      url: `${LOG_BASE_URL}/export`,
      method: "get",
      params: queryParams,
      responseType: "blob",
    });
  },
};

export default LogAPI;

// 重导出类型
export * from "./types";
