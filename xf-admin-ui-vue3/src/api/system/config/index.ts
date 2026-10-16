import request from "@/utils/request";
import type { PageResult } from "@/api/common";
import type { ConfigQueryParams, ConfigForm, ConfigItem } from "./types";

/**
 * 参数配置（CFG-01）
 *
 * youlai 契约 /api/v1/sys-config，网关 StripPrefix 后落到 xf-auth
 * SysConfigController；统一响应 ApiResult（code="00000"），分页 PageResult{list,total}。
 */
const CONFIG_BASE_URL = "/api/v1/sys-config";

const ConfigAPI = {
  /**
   * 参数配置分页
   */
  getPage(params: ConfigQueryParams): Promise<PageResult<ConfigItem>> {
    return request<unknown, PageResult<ConfigItem>>({
      url: `${CONFIG_BASE_URL}/page`,
      method: "get",
      params: {
        pageNum: params.pageNum,
        pageSize: params.pageSize,
        keywords: params.keywords || undefined,
        status: params.status === "" ? undefined : params.status,
      },
    });
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

  /**
   * 查看敏感项明文（独立权限 sys:config:reveal，编辑弹窗「查看当前值」用）
   */
  plaintext(id: string) {
    return request<unknown, string>({
      url: `${CONFIG_BASE_URL}/${id}/plaintext`,
      method: "get",
    });
  },

  /** 新增配置，返回新 ID */
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
