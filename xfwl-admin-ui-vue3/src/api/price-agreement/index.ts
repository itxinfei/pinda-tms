import request from "@/utils/request";
import type { IPageResult } from "@/api/order-exception";

const BASE_URL = "/api/manager/price-agreement";

/**
 * 客户价格协议（FIN-1.1 / POD-06 客户侧费率卡）
 *
 * 经网关 /api/manager 前缀访问 xfwl-web-manager（薄转发 xfwl-oms）：
 * - POST /price-agreement          新增/修改
 * - POST /price-agreement/{id}/disable 停用
 * - GET  /price-agreement/page     分页（memberId/status 过滤）
 *
 * 计费口径：Drools 之前按 客户+重量段+业务日期 命中协议价，未命中走默认规则。
 */

/** 协议状态（后端常量口径，禁硬编码扩展） */
export enum AgreementStatus {
  /** 停用 */
  DISABLED = 0,
  /** 生效 */
  ACTIVE = 1,
}

/** 计价方式（后端 PriceAgreement 常量口径，禁硬编码扩展） */
export enum PriceType {
  /** 按票 */
  PER_WAYBILL = 1,
  /** 按吨 */
  PER_TON = 2,
  /** 按方（预留，暂同按票） */
  PER_CUBIC = 3,
}

export const PRICE_TYPE_LABELS: Record<number, string> = {
  [PriceType.PER_WAYBILL]: "按票",
  [PriceType.PER_TON]: "按吨",
  [PriceType.PER_CUBIC]: "按方",
};

/** 价格协议记录（oms PriceAgreement 实体直出） */
export interface PriceAgreementRecord {
  id: string;
  memberId: string;
  lineId?: number | null;
  goodsTypeId?: number | null;
  weightMin?: number | null;
  weightMax?: number | null;
  priceType: number;
  price: number;
  discount: number;
  effectiveFrom: string;
  effectiveTo?: string | null;
  status: number;
  remark?: string;
  createTime?: string;
}

export interface PriceAgreementQuery {
  page: number;
  pageSize: number;
  memberId?: string;
  status?: number;
}

const PriceAgreementAPI = {
  /** 新增/修改（id 空=新增） */
  save(data: Partial<PriceAgreementRecord>) {
    return request<unknown, unknown>({ url: BASE_URL, method: "post", data });
  },

  /** 停用 */
  disable(id: string) {
    return request<unknown, unknown>({ url: `${BASE_URL}/${id}/disable`, method: "post" });
  },

  /** 分页查询 */
  page(params: PriceAgreementQuery) {
    return request<unknown, IPageResult<PriceAgreementRecord>>({
      url: `${BASE_URL}/page`,
      method: "get",
      params,
    });
  },
};

export default PriceAgreementAPI;
