import request from "@/utils/request";
import type { IPageResult } from "@/api/order-exception";

const BASE_URL = "/api/manager/carrier";

/**
 * 承运商/外协运力池（DSP-2 第一期 / CAP-01）
 *
 * 经网关 /api/manager 前缀访问 xf-web-manager（薄转发 xf-base）：
 * - POST /carrier            新增/修改承运商
 * - GET  /carrier/page       分页（code/name/status 过滤）
 * - GET  /carrier/{id}       详情（含挂靠车辆/司机）
 * - POST /carrier/{id}/status 状态流转（1合作中/0暂停/2黑名单）
 * - POST /carrier/truck      挂靠车辆新增/修改
 * - POST /carrier/driver     挂靠司机新增/修改
 * - GET  /carrier/capacity   运力池查询
 */

/** 承运商状态（后端 XfCarrier 常量口径，禁硬编码扩展） */
export enum CarrierStatus {
  /** 暂停 */
  SUSPENDED = 0,
  /** 合作中 */
  ACTIVE = 1,
  /** 黑名单 */
  BLACKLIST = 2,
}

export const CARRIER_STATUS_LABELS: Record<number, string> = {
  [CarrierStatus.SUSPENDED]: "暂停",
  [CarrierStatus.ACTIVE]: "合作中",
  [CarrierStatus.BLACKLIST]: "黑名单",
};

/** 结算方式（后端 XfCarrier 常量口径） */
export enum SettleType {
  /** 现结 */
  CASH = 1,
  /** 月结 */
  MONTHLY = 2,
  /** 趟结 */
  PER_TRIP = 3,
}

export const SETTLE_TYPE_LABELS: Record<number, string> = {
  [SettleType.CASH]: "现结",
  [SettleType.MONTHLY]: "月结",
  [SettleType.PER_TRIP]: "趟结",
};

/** 外协车辆/司机状态（1正常 0禁用 2在途） */
export const CARRIER_MEMBER_STATUS_LABELS: Record<number, string> = {
  0: "禁用",
  1: "正常",
  2: "在途",
};

/** 承运商记录（base XfCarrier 实体直出） */
export interface CarrierRecord {
  id: string;
  code: string;
  name: string;
  licenseNo?: string | null;
  licenseExpiry?: string | null;
  contactName?: string | null;
  contactPhone?: string | null;
  settleType: number;
  status: number;
  remark?: string | null;
  createTime?: string;
}

export interface CarrierQuery {
  page: number;
  pageSize: number;
  code?: string;
  name?: string;
  status?: number;
}

/** 外协车辆记录 */
export interface CarrierTruckRecord {
  id: string;
  carrierId: string;
  licensePlate: string;
  truckTypeId?: number | string | null;
  allowableLoad?: number | null;
  allowableVolume?: number | null;
  status: number;
  createTime?: string;
}

/** 外协司机记录 */
export interface CarrierDriverRecord {
  id: string;
  carrierId: string;
  name: string;
  phone?: string | null;
  licenseNo?: string | null;
  status: number;
  createTime?: string;
}

/** 运力池条目（车辆字段 + carrierName） */
export interface CapacityItem {
  id: string;
  carrierId: string;
  carrierName?: string | null;
  licensePlate: string;
  truckTypeId?: number | string | null;
  allowableLoad?: number | null;
  allowableVolume?: number | null;
  status: number;
}

const CarrierAPI = {
  /** 新增/修改承运商（id 空=新增） */
  save(data: Partial<CarrierRecord>) {
    return request<unknown, unknown>({ url: BASE_URL, method: "post", data });
  },

  /** 分页查询 */
  page(params: CarrierQuery) {
    return request<unknown, IPageResult<CarrierRecord>>({
      url: `${BASE_URL}/page`,
      method: "get",
      params,
    });
  },

  /** 详情（含挂靠车辆/司机） */
  detail(id: string) {
    return request<unknown, { carrier: CarrierRecord; trucks: CarrierTruckRecord[]; drivers: CarrierDriverRecord[] }>({
      url: `${BASE_URL}/${id}`,
      method: "get",
    });
  },

  /** 状态流转（1合作中/0暂停/2黑名单） */
  changeStatus(id: string, status: CarrierStatus) {
    return request<unknown, unknown>({
      url: `${BASE_URL}/${id}/status`,
      method: "post",
      params: { status },
    });
  },

  /** 挂靠车辆新增/修改（id 空=新增） */
  saveTruck(data: Partial<CarrierTruckRecord>) {
    return request<unknown, unknown>({ url: `${BASE_URL}/truck`, method: "post", data });
  },

  /** 挂靠司机新增/修改（id 空=新增） */
  saveDriver(data: Partial<CarrierDriverRecord>) {
    return request<unknown, unknown>({ url: `${BASE_URL}/driver`, method: "post", data });
  },

  /** 运力池查询 */
  capacity(params: { keyword?: string; truckTypeId?: number | string; minLoad?: number }) {
    return request<unknown, CapacityItem[]>({
      url: `${BASE_URL}/capacity`,
      method: "get",
      params,
    });
  },
};

export default CarrierAPI;
