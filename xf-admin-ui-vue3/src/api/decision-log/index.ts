import request from "@/utils/request";
import type { IPageResult } from "@/api/order-exception";

const DECISION_LOG_BASE_URL = "/api/manager/decision-log";

/**
 * 合规决策留痕（FR-75 / P0-9）
 *
 * 经网关 /api/manager 前缀访问 xf-web-manager 的 DecisionLogController（薄转发 xf-oms）：
 * - GET /decision-log/page   分页查询（businessType/businessId 过滤）
 * - GET /decision-log/export 监管导出（返回 JSON 行数组，前端组 CSV 下载）
 *
 * 留痕由订单改价 /{id}/reprice、取消 /{id}/cancel 自动落库（reasonEnum 白名单校验），
 * 本模块只读查询；改派（REASSIGN）埋点待 G2 落地接入。
 */

/** 决策类型（后端 oms DecisionLog 白名单口径，禁硬编码扩展须两端同步） */
export enum DecisionType {
  /** 运费调整 */
  REPRICE = "REPRICE",
  /** 拒单/取消 */
  CANCEL = "CANCEL",
  /** 改派（G2 落地时接入） */
  REASSIGN = "REASSIGN",
}

/** 枚举理由白名单（与 oms DecisionLogServiceImpl.REASON_ENUMS 保持一致，新增理由先扩后端） */
export const REASON_ENUMS: Record<DecisionType, Record<string, string>> = {
  [DecisionType.REPRICE]: {
    COST_CORRECTION: "成本修正",
    QUOTATION_ERROR: "报价错误",
    CUSTOMER_DISCOUNT: "客户折扣",
  },
  [DecisionType.CANCEL]: {
    CUSTOMER_CANCEL: "客户取消",
    CARGO_DAMAGE: "货物破损",
    ADDRESS_ERROR: "地址错误",
    OTHER: "其他",
  },
  [DecisionType.REASSIGN]: {
    DRIVER_OFFLINE: "司机离线",
    VEHICLE_FAULT: "车辆故障",
    CUSTOMER_REQUEST: "客户要求",
    SCHEDULE_CONFLICT: "调度冲突",
  },
};

/** 决策类型中文 */
export const DECISION_TYPE_LABELS: Record<DecisionType, string> = {
  [DecisionType.REPRICE]: "运费调整",
  [DecisionType.CANCEL]: "拒单/取消",
  [DecisionType.REASSIGN]: "改派",
};

/** 留痕记录（oms DecisionLog 实体直出） */
export interface DecisionLogRecord {
  id: string;
  businessType: DecisionType | string;
  businessId: string;
  reasonEnum: string;
  reasonDetail?: string;
  operator?: string;
  operateTime?: string;
  createTime?: string;
}

/** 分页查询条件 */
export interface DecisionLogPageQuery {
  page: number;
  pageSize: number;
  businessType?: DecisionType;
  businessId?: string;
}

/** 导出行（后端 exportRows 返回的 Map 结构） */
export interface DecisionExportRow {
  operateTime: string;
  businessType: string;
  businessId: string | number;
  reasonEnum: string;
  reasonDetail?: string;
  operator?: number | string;
}

/** 导出条件 */
export interface DecisionLogExportQuery {
  businessType?: DecisionType;
  businessId?: string;
  beginTime?: string;
  endTime?: string;
}

const DecisionLogAPI = {
  /** 分页查询 */
  page(params: DecisionLogPageQuery) {
    return request<unknown, IPageResult<DecisionLogRecord>>({
      url: `${DECISION_LOG_BASE_URL}/page`,
      method: "get",
      params,
    });
  },

  /** 监管导出（JSON 行数组，前端组 CSV） */
  export(params: DecisionLogExportQuery) {
    return request<unknown, DecisionExportRow[]>({
      url: `${DECISION_LOG_BASE_URL}/export`,
      method: "get",
      params,
    });
  },
};

export default DecisionLogAPI;
