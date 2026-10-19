import request from "@/utils/request";
import type { IPageResult } from "@/api/order-exception";

const BASE_URL = "/api/manager/invoice";

/**
 * 电子发票（开票）—— P0-6c / FIN-1.2
 *
 * 经网关 /api/manager 前缀访问 xf-web-manager（薄转发 xf-oms）：
 * - GET  /invoice/page          分页（customerId/status 过滤）
 * - POST /invoice/apply         申请开票（按账单，金额与客户取自账单）
 * - POST /invoice/{id}/issue    开具（测试环境自编发票号 TEST-+ts）
 * - POST /invoice/{id}/mail     邮寄（3→4）
 * - POST /invoice/{id}/void     作废（→5，原因必填）
 *
 * 权限码：finance:invoice:apply|issue|mail|void|page（具体码禁通配）
 */

/** 发票状态（后端 Invoice 常量口径，禁硬编码扩展） */
export enum InvoiceStatus {
  /** 待申请 */
  APPLIED = 1,
  /** 已申请 */
  REQUESTED = 2,
  /** 已开具 */
  ISSUED = 3,
  /** 已邮寄 */
  MAILED = 4,
  /** 已作废 */
  VOIDED = 5,
}

export const INVOICE_STATUS_LABELS: Record<number, string> = {
  [InvoiceStatus.APPLIED]: "待申请",
  [InvoiceStatus.REQUESTED]: "已申请",
  [InvoiceStatus.ISSUED]: "已开具",
  [InvoiceStatus.MAILED]: "已邮寄",
  [InvoiceStatus.VOIDED]: "已作废",
};

/** 票种（后端 Invoice 常量口径） */
export enum InvoiceType {
  /** 增值税普票（缺省） */
  GENERAL = 1,
  /** 增值税专票 */
  SPECIAL = 2,
}

export const INVOICE_TYPE_LABELS: Record<number, string> = {
  [InvoiceType.GENERAL]: "增值税普票",
  [InvoiceType.SPECIAL]: "增值税专票",
};

/** 发票记录（oms Invoice 实体直出） */
export interface InvoiceRecord {
  id: string;
  /** 发票号（issue 自编 TEST-+ts；未开具为空） */
  invoiceNo?: string;
  /** 关联账单 id（oms_freight_bill.id） */
  billId: number | string;
  /** 客户id（对齐 settle_object_id，散客=GUEST） */
  customerId: string;
  /** 开票金额（=账单应收总额） */
  amount: number;
  /** 票种：1普票 2专票 */
  type: InvoiceType;
  /** 状态：1待申请 2已申请 3已开具 4已邮寄 5已作废 */
  status: InvoiceStatus;
  /** 客户税号 */
  taxNo?: string;
  /** 邮寄地址 */
  mailAddress?: string;
  /** 开具时间 */
  invoiceTime?: string;
  /** 作废原因（status=5 必填） */
  voidReason?: string;
  remark?: string;
  createTime?: string;
}

export interface InvoiceQuery {
  page: number;
  pageSize: number;
  customerId?: string;
  status?: number;
}

const InvoiceAPI = {
  /** 分页查询 */
  page(params: InvoiceQuery) {
    return request<unknown, IPageResult<InvoiceRecord>>({
      url: `${BASE_URL}/page`,
      method: "get",
      params,
    });
  },

  /** 申请开票（按账单；同账单已有未作废发票由后端幂等拒绝 400） */
  apply(params: { billId: number | string; type?: InvoiceType; taxNo?: string; mailAddress?: string }) {
    return request<unknown, InvoiceRecord>({
      url: `${BASE_URL}/apply`,
      method: "post",
      params,
    });
  },

  /** 开具（测试环境自编发票号） */
  issue(id: string) {
    return request<unknown, InvoiceRecord>({
      url: `${BASE_URL}/${id}/issue`,
      method: "post",
    });
  },

  /** 邮寄（3→4） */
  mail(id: string) {
    return request<unknown, unknown>({
      url: `${BASE_URL}/${id}/mail`,
      method: "post",
    });
  },

  /** 作废（→5，原因必填） */
  void(id: string, reason: string) {
    return request<unknown, unknown>({
      url: `${BASE_URL}/${id}/void`,
      method: "post",
      params: { reason },
    });
  },
};

export default InvoiceAPI;
