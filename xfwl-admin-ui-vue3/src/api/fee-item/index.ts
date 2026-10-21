import request from "@/utils/request";
import type { IPageResult } from "@/api/order-exception";

const BASE_URL = "/api/manager/fee-item";

/**
 * 附加费费用项配置（POD-03 第一期 / FA-10）
 *
 * 经网关 /api/manager 前缀访问 xfwl-web-manager（薄转发 xfwl-oms）：
 * - POST /fee-item          新增/修改
 * - POST /fee-item/{id}/disable 停用
 * - GET  /fee-item/page     分页（feeCode/status 过滤）
 *
 * fee_code 与 RPT-01 成本项口径对齐，扩项须前后端同步。
 */

/** 费用项编码（后端 FeeItem 常量口径，禁硬编码扩展） */
export enum FeeCode {
  /** 路费 */
  TOLL = "TOLL",
  /** 装卸费 */
  LOADING = "LOADING",
  /** 等待费 */
  WAIT = "WAIT",
  /** 超重附加费 */
  OVERWEIGHT = "OVERWEIGHT",
  /** 燃油附加费 */
  FUEL = "FUEL",
  /** 返空费 */
  EMPTY_RETURN = "EMPTY_RETURN",
}

export const FEE_CODE_LABELS: Record<string, string> = {
  [FeeCode.TOLL]: "路费",
  [FeeCode.LOADING]: "装卸费",
  [FeeCode.WAIT]: "等待费",
  [FeeCode.OVERWEIGHT]: "超重附加费",
  [FeeCode.FUEL]: "燃油附加费",
  [FeeCode.EMPTY_RETURN]: "返空费",
};

/** 计费方式（第一期口径：1固定金额 2按运费比例） */
export enum FeeCalcType {
  FIXED = 1,
  RATIO = 2,
}

export const CALC_TYPE_LABELS: Record<number, string> = {
  [FeeCalcType.FIXED]: "固定金额",
  [FeeCalcType.RATIO]: "按运费比例",
};

/** 费用项状态（后端常量口径，禁硬编码扩展） */
export enum FeeItemStatus {
  /** 停用 */
  DISABLED = 0,
  /** 启用 */
  ENABLED = 1,
}

/** 费用项记录（oms FeeItem 实体直出） */
export interface FeeItemRecord {
  id: string;
  feeCode: string;
  feeName: string;
  calcType: number;
  fixedAmount?: number | null;
  ratio?: number | null;
  status: number;
  remark?: string;
  createTime?: string;
}

export interface FeeItemQuery {
  page: number;
  pageSize: number;
  feeCode?: string;
  status?: number;
}

const FeeItemAPI = {
  /** 新增/修改（id 空=新增） */
  save(data: Partial<FeeItemRecord>) {
    return request<unknown, unknown>({ url: BASE_URL, method: "post", data });
  },

  /** 停用 */
  disable(id: string) {
    return request<unknown, unknown>({ url: `${BASE_URL}/${id}/disable`, method: "post" });
  },

  /** 分页查询 */
  page(params: FeeItemQuery) {
    return request<unknown, IPageResult<FeeItemRecord>>({
      url: `${BASE_URL}/page`,
      method: "get",
      params,
    });
  },
};

export default FeeItemAPI;
