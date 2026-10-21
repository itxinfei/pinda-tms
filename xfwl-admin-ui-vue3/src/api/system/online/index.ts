import request from "@/utils/request";
import type { PageResult } from "@/api/common";
import type { OnlineItem, OnlineQueryParams } from "./types";

const ONLINE_BASE_URL = "/api/v1/online";

const OnlineAPI = {
  /**
   * 在线会话分页
   */
  getPage(queryParams: OnlineQueryParams) {
    return request<unknown, PageResult<OnlineItem>>({
      url: `${ONLINE_BASE_URL}`,
      method: "get",
      params: queryParams,
    });
  },

  /**
   * 强制下线（kickout 指定会话）
   */
  kickout(token: string) {
    return request<unknown, void>({
      url: `${ONLINE_BASE_URL}/${token}`,
      method: "delete",
    });
  },
};

export default OnlineAPI;
export * from "./types";
