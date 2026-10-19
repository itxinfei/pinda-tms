import request from "@/utils/request";
import type { IPageResult } from "@/api/order-exception";

const BASE_URL = "/api/manager/freight-bill";

/**
 * 运费账单（对账）—— P0-6b 应收对账闭环
 *
 * 经网关 /api/manager 前缀访问 xf-web-manager（薄转发 xf-oms）：
 * - GET  /freight-bill/page           分页（billNo/customerId/status 过滤）
 * - POST /freight-bill/generate       生成账单（归集应收结算单）
 * - GET  /freight-bill/{id}           对账视图（账单头+结算单列表+差异标记）
 * - POST /freight-bill/{id}/confirm   确认（0→1，结算单联动）
 * - POST /freight-bill/{id}/write-off 核销（1→2，结算单联动）
 *
 * 权限码：finance:freight-bill:page|detail|generate|confirm|write-off
 */

/** 账单状态（后端 FreightBill 常量口径，禁硬编码扩展） */
export enum FreightBillStatus {
  /** 待确认 */
  PENDING = 0,
  /** 已确认 */
  CONFIRMED = 1,
  /** 已核销 */
  WRITTEN_OFF = 2,
}

export const FREIGHT_BILL_STATUS_LABELS: Record<number, string> = {
  [FreightBillStatus.PENDING]: "待确认",
  [FreightBillStatus.CONFIRMED]: "已确认",
  [FreightBillStatus.WRITTEN_OFF]: "已核销",
};

/** 结算单状态（后端 SettlementOrder 常量口径） */
export const SETTLEMENT_STATUS_LABELS: Record<number, string> = {
  0: "待对账",
  1: "已对账",
  2: "已结算",
};

/** 运费账单记录（oms FreightBill 实体直出） */
export interface FreightBillRecord {
  id: string;
  billNo: string;
  /** 客户id（对齐结算单 settle_object_id，散客=GUEST） */
  customerId: string;
  periodStart: string;
  periodEnd: string;
  /** 归集结算单条数 */
  orderCount: number;
  /** 应收总额（=Σ归集结算单 receivable_amount） */
  totalAmount: number;
  status: FreightBillStatus;
  remark?: string;
  createTime?: string;
}

/** 归集的应收结算单（oms SettlementOrder 实体直出，节选对账所需字段） */
export interface SettlementOrderRecord {
  id: string;
  settlementNo: string;
  orderId: number;
  settleObjectType: number;
  settleObjectId: string;
  receivableAmount: number;
  status: number;
  createTime?: string;
}

/** 对账视图（xf-oms reconcile 返回结构） */
export interface ReconcileView {
  bill: FreightBillRecord;
  settlements: SettlementOrderRecord[];
  /** Σ结算单应收 */
  sumReceivable: number;
  /** 差异 = 账单总额 - Σ应收（0 为平） */
  diff: number;
}

export interface FreightBillQuery {
  page: number;
  pageSize: number;
  billNo?: string;
  customerId?: string;
  status?: number;
}

const FreightBillAPI = {
  /** 分页查询 */
  page(params: FreightBillQuery) {
    return request<unknown, IPageResult<FreightBillRecord>>({
      url: `${BASE_URL}/page`,
      method: "get",
      params,
    });
  },

  /** 生成账单（归集该客户该账期应收结算单） */
  generate(customerId: string, periodStart: string, periodEnd: string) {
    return request<unknown, unknown>({
      url: `${BASE_URL}/generate`,
      method: "post",
      params: { customerId, periodStart, periodEnd },
    });
  },

  /** 对账视图 */
  reconcile(id: string) {
    return request<unknown, ReconcileView>({
      url: `${BASE_URL}/${id}`,
      method: "get",
    });
  },

  /** 确认账单（0→1） */
  confirm(id: string) {
    return request<unknown, unknown>({
      url: `${BASE_URL}/${id}/confirm`,
      method: "post",
    });
  },

  /** 核销账单（1→2） */
  writeOff(id: string) {
    return request<unknown, unknown>({
      url: `${BASE_URL}/${id}/write-off`,
      method: "post",
    });
  },
};

export default FreightBillAPI;
