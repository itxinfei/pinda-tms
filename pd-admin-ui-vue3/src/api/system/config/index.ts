import request from "@/utils/request";
import type { PageResponse, PageResult } from "@/api/common";
import type { ConfigQueryParams, ConfigForm, ConfigItem } from "./types";

/**
 * 参数配置（CFG-01）
 *
 * BFF pd-web-manager 薄转发至 pd-auth，网关前缀 /api/manager。
 * 后端分页为 PageResponse（counts/items，参数 page），在此适配为
 * 列表页通用的 PageResult（list/total，参数 pageNum），供 usePageTable 直接消费。
 */
const CONFIG_BASE_URL = "/api/manager/sys-config";

const ConfigAPI = {
  /**
   * 参数配置分页（pageNum → 后端 page；counts/items → total/list）
   */
  async getPage(params: ConfigQueryParams): Promise<PageResult<ConfigItem>> {
    const res = await request<unknown, PageResponse<ConfigItem>>({
      url: `${CONFIG_BASE_URL}/page`,
      method: "get",
      params: {
        page: params.pageNum,
        pageSize: params.pageSize,
        keywords: params.keywords || undefined,
        status: params.status === "" ? undefined : params.status,
      },
    });
    return { list: res?.items ?? [], total: res?.counts ?? 0 };
  },

  /**
   * 参数配置详情（敏感项 configValue 为 null，仅返回 valueMasked）
   */
  detail(id: string) {
    return request<unknown, ConfigItem>({
      url: `${CONFIG_BASE_URL}/${id}`,
      method: "get",
    });
  },

  /** 新增配置 */
  create(data: ConfigForm) {
    return request({ url: `${CONFIG_BASE_URL}`, method: "post", data });
  },

  /** 修改配置（敏感值留空表示不修改） */
  update(id: string, data: ConfigForm) {
    return request({ url: `${CONFIG_BASE_URL}/${id}`, method: "put", data });
  },

  /** 删除配置（物理删除） */
  deleteById(id: string) {
    return request({ url: `${CONFIG_BASE_URL}/${id}`, method: "delete" });
  },
};

export default ConfigAPI;

// 重导出类型
export * from "./types";
