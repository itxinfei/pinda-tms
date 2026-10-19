import request from "@/utils/request";
import type { IPageResult } from "@/api/order-exception";

const BASE_URL = "/api/manager/driver-settlement";

/**
 * 司机趟次结算（工资单）—— FIN-2 / POD-05
 *
 * 经网关 /api/manager 前缀访问 xf-web-manager（薄转发 xf-work）：
 * - POST /generate           按司机+账期生成（归集已完成趟次，幂等拒绝重叠账期）
 * - GET  /drivers            候选司机（账期内有可归集趟次）
 * - POST /page               分页（settleNo/driverId/status 过滤）
 * - GET  /{id}               详情（头+逐趟明细）
 * - POST /{id}/audit         审核（0→1，可带审核备注）
 * - POST /{id}/pay           发放登记（1→2，线下打款后留痕，D-27 不接支付通道）
 *
 * 权限码：finance:driver-settlement:generate|drivers|page|detail|audit|pay（具体码禁通配）
 */

/** 结算单状态（《07》§11.3，禁硬编码扩展） */
export enum SettlementStatus {
  /** 待审 */
  PENDING = 0,
  /** 已审 */
  AUDITED = 1,
  /** 已发放 */
  PAID = 2,
}

export const SETTLEMENT_STATUS_LABELS: Record<number, string> = {
  [SettlementStatus.PENDING]: "待审",
  [SettlementStatus.AUDITED]: "已审",
  [SettlementStatus.PAID]: "已发放",
};

/** 应发 = 底薪 + 趟次费 + 补贴 − 扣款 */
export interface DriverSettlementRecord {
  id: string;
  /** 结算单号 DJS+yyyyMMdd+序号 */
  settleNo: string;
  driverId: string;
  /** 司机名（web-manager 装配，兜底"司机#id"） */
  driverName?: string;
  periodStart: string;
  periodEnd: string;
  tripCount: number;
  baseWage: number;
  tripFee: number;
  allowance: number;
  deduction: number;
  totalAmount: number;
  status: SettlementStatus;
  auditRemark?: string;
  paidTime?: string;
  remark?: string;
  createTime?: string;
}

/** 逐趟明细 */
export interface SettlementDetailRecord {
  id: string;
  settlementId: string;
  taskTransportId: string;
  driverJobId: string;
  amount: number;
  remark?: string;
  createTime?: string;
}

/** 候选司机（账期内有可归集趟次） */
export interface CandidateDriver {
  driverId: string;
  driverName?: string;
  tripCount: number;
}

export interface SettlementQuery {
  page: number;
  pageSize: number;
  settleNo?: string;
  driverId?: string;
  status?: number;
}

export interface GenerateParams {
  driverId: string;
  /** yyyy-MM-dd */
  periodStart: string;
  /** yyyy-MM-dd */
  periodEnd: string;
  baseWage?: number;
  allowance?: number;
  deduction?: number;
  remark?: string;
}

const DriverSettlementAPI = {
  /** 分页查询 */
  page(params: SettlementQuery) {
    return request<unknown, IPageResult<DriverSettlementRecord>>({
      url: `${BASE_URL}/page`,
      method: "post",
      params,
    });
  },

  /** 生成结算单（同司机账期重叠已有单由后端幂等拒绝 400） */
  generate(params: GenerateParams) {
    return request<unknown, DriverSettlementRecord>({
      url: `${BASE_URL}/generate`,
      method: "post",
      params,
    });
  },

  /** 候选司机（账期内有可归集趟次） */
  drivers(params: { periodStart: string; periodEnd: string }) {
    return request<unknown, CandidateDriver[]>({
      url: `${BASE_URL}/drivers`,
      method: "get",
      params,
    });
  },

  /** 详情（头+逐趟明细） */
  detail(id: string) {
    return request<unknown, { settlement: DriverSettlementRecord; details: SettlementDetailRecord[] }>({
      url: `${BASE_URL}/${id}`,
      method: "get",
    });
  },

  /** 审核（0→1，审核备注选填） */
  audit(id: string, auditRemark?: string) {
    return request<unknown, unknown>({
      url: `${BASE_URL}/${id}/audit`,
      method: "post",
      params: { auditRemark: auditRemark?.trim() || undefined },
    });
  },

  /** 发放登记（1→2） */
  pay(id: string) {
    return request<unknown, unknown>({
      url: `${BASE_URL}/${id}/pay`,
      method: "post",
    });
  },
};

export default DriverSettlementAPI;
