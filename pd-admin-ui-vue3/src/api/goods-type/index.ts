import request from "@/utils/request";
import type { PageResponse } from "@/api/common";

const GOODS_TYPE_BASE_URL = "/api/manager/transfor-center/bussiness/goodsType";

/** 货物类型 */
export interface GoodsTypeRecord {
  id?: string;
  /** 货物类型名称 */
  name: string;
  /** 默认重量（kg） */
  defaultWeight?: number;
  /** 默认体积（方） */
  defaultVolume?: number;
  /** 说明 */
  remark?: string;
  /** 状态：1 启用，0 停用（分页接口默认返回全部，按 status 筛选时只传 0/1） */
  status?: number;
}

/** 货物类型分页查询条件（GET 参数，空字段不参与过滤） */
export interface GoodsTypePageQuery {
  page: number;
  pageSize: number;
  name?: string;
  /** 状态筛选：1 启用 / 0 停用；不传查全部 */
  status?: number;
}

const GoodsTypeAPI = {
  /** 货物类型分页 */
  page(params: GoodsTypePageQuery) {
    return request<unknown, PageResponse<GoodsTypeRecord>>({
      url: `${GOODS_TYPE_BASE_URL}/page`,
      method: "get",
      params,
    });
  },
  /** 货物类型详情 */
  getDetail(id: string) {
    return request<unknown, GoodsTypeRecord>({
      url: `${GOODS_TYPE_BASE_URL}/${id}`,
      method: "get",
    });
  },
  /** 新增货物类型 */
  save(data: GoodsTypeRecord) {
    return request<unknown, GoodsTypeRecord>({
      url: GOODS_TYPE_BASE_URL,
      method: "post",
      data,
    });
  },
  /** 更新货物类型 */
  update(id: string, data: GoodsTypeRecord) {
    return request<unknown, GoodsTypeRecord>({
      url: `${GOODS_TYPE_BASE_URL}/${id}`,
      method: "put",
      data,
    });
  },
  /** 停用货物类型（软停用；被车辆类型关联时后端返回 400） */
  disable(id: string) {
    return request<unknown, unknown>({
      url: `${GOODS_TYPE_BASE_URL}/${id}`,
      method: "delete",
    });
  },
  /** 启用货物类型（恢复停用项） */
  enable(id: string) {
    return request<unknown, unknown>({
      url: `${GOODS_TYPE_BASE_URL}/${id}/enable`,
      method: "put",
    });
  },
};

export default GoodsTypeAPI;
