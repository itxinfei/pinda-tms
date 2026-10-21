import request from "@/utils/request";

const BASE_URL = "/api/manager/order-fee";

/**
 * 订单附加费明细（POD-03 第一期）
 *
 * 经网关 /api/manager 前缀访问 xfwl-web-manager（薄转发 xfwl-oms）：
 * - GET  /order-fee/list            按订单查明细+合计
 * - POST /order-fee                 人工补录
 * - POST /order-fee/calculate/{orderId} 按配置自动生成（幂等）
 *
 * 明细独立存表不改写订单 amount，展示层汇总（ORD-03 明细可解释口径）。
 */

/** 附加费明细记录（oms OrderFee 实体直出） */
export interface OrderFeeRecord {
  id: string;
  orderId: string;
  feeCode: string;
  feeName: string;
  amount: number;
  /** 来源：1自动计算 2人工补录 */
  source: number;
  calcBasis?: string;
  operatorName?: string;
  remark?: string;
  createTime?: string;
}

/** list 端点返回结构 */
export interface OrderFeeListResult {
  items: OrderFeeRecord[];
  total: number;
}

const OrderFeeAPI = {
  /** 按订单查明细+合计 */
  list(orderId: string) {
    return request<unknown, OrderFeeListResult>({
      url: `${BASE_URL}/list`,
      method: "get",
      params: { orderId },
    });
  },

  /** 人工补录 */
  addManual(data: { orderId: string; feeCode: string; amount: number; calcBasis?: string; remark?: string }) {
    return request<unknown, OrderFeeRecord>({ url: BASE_URL, method: "post", data });
  },

  /** 按配置自动生成（幂等，返回本次新增明细） */
  calculate(orderId: string) {
    return request<unknown, OrderFeeRecord[]>({ url: `${BASE_URL}/calculate/${orderId}`, method: "post" });
  },
};

export default OrderFeeAPI;
