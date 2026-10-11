import request from "@/utils/request";
import type { PageResponse } from "@/api/common";
import type { GoodsTypeSimple } from "@/api/common-simple";

const TRUCK_TYPE_BASE_URL = "/api/manager/transfor-center/bussiness/truckType";

/** 车辆类型 */
export interface TruckTypeRecord {
  id?: string;
  /** 车辆类型名称 */
  name: string;
  /** 准载重量（t） */
  allowableLoad?: number;
  /** 准载体积（m³） */
  allowableVolume?: number;
  /** 车厢长（m） */
  measureLong?: number;
  /** 车厢宽（m） */
  measureWidth?: number;
  /** 车厢高（m） */
  measureHigh?: number;
  /** 关联货物类型（详情/分页由后端回填，保存时只传 [{id}]） */
  goodsTypes?: GoodsTypeSimple[];
  /** 状态：1 启用，0 停用（分页接口默认返回全部，按 status 筛选时只传 0/1） */
  status?: number;
}

/** 车辆类型保存入参：关联货物类型只提交 id */
export interface TruckTypeSavePayload {
  id?: string;
  name: string;
  allowableLoad?: number;
  allowableVolume?: number;
  measureLong?: number;
  measureWidth?: number;
  measureHigh?: number;
  goodsTypes?: { id: string }[];
}

/** 车辆类型分页查询条件（GET 参数，空字段不参与过滤） */
export interface TruckTypePageQuery {
  page: number;
  pageSize: number;
  name?: string;
  /** 状态筛选：1 启用 / 0 停用；不传查全部 */
  status?: number;
}

const TruckTypeAPI = {
  /** 车辆类型分页 */
  page(params: TruckTypePageQuery) {
    return request<unknown, PageResponse<TruckTypeRecord>>({
      url: `${TRUCK_TYPE_BASE_URL}/page`,
      method: "get",
      params,
    });
  },
  /** 车辆类型详情（含关联货物类型） */
  getDetail(id: string) {
    return request<unknown, TruckTypeRecord>({
      url: `${TRUCK_TYPE_BASE_URL}/${id}`,
      method: "get",
    });
  },
  /** 新增车辆类型 */
  save(data: TruckTypeSavePayload) {
    return request<unknown, TruckTypeRecord>({
      url: TRUCK_TYPE_BASE_URL,
      method: "post",
      data,
    });
  },
  /** 更新车辆类型 */
  update(id: string, data: TruckTypeSavePayload) {
    return request<unknown, TruckTypeRecord>({
      url: `${TRUCK_TYPE_BASE_URL}/${id}`,
      method: "put",
      data,
    });
  },
  /** 停用车辆类型（软停用；关联校验在 pd-base 侧实现） */
  disable(id: string) {
    return request<unknown, unknown>({
      url: `${TRUCK_TYPE_BASE_URL}/${id}`,
      method: "delete",
    });
  },
  /** 启用车辆类型（恢复停用项） */
  enable(id: string) {
    return request<unknown, unknown>({
      url: `${TRUCK_TYPE_BASE_URL}/${id}/enable`,
      method: "put",
    });
  },
};

export default TruckTypeAPI;
