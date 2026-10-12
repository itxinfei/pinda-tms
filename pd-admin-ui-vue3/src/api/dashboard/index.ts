import request from "@/utils/request";

/**
 * 管理端工作台（dashboard）
 *
 * 聚合接口由 pd-web-manager 提供（经网关 /api/manager 前缀）。
 * 订单统计源自 pd-oms，在途车辆源自 pd-work；异常告警走独立的 netty 接口（见 @/api/alarm）。
 */
const DASHBOARD_BASE_URL = "/api/manager/dashboard";

/** 工作台总览（KPI + 运单三桶 + 在途车辆） */
export interface DashboardOverview {
  /** 今日新增运单数 */
  todayOrderCount: number;
  /** 在途车辆数（进行中运输任务数） */
  onTruckCount: number;
  /** 待处理运单数（订单状态 1-5） */
  pendingCount: number;
  /** 运输中运单数（订单状态 6-9） */
  inTransitCount: number;
  /** 已签收运单数（订单状态 10） */
  completedCount: number;
}

/** 运输量趋势单点 */
export interface OrderTrendPoint {
  /** 日期 yyyy-MM-dd */
  date: string;
  /** 当日下单量 */
  orderCount: number;
  /** 当日签收量（按 update_time 近似） */
  completedCount: number;
}

/** 待处理运单列表项（中文状态由后端给出，前端不做状态码映射） */
export interface PendingOrder {
  id: string;
  orderNo: string;
  senderName?: string;
  status: number;
  statusText: string;
  createTime?: string;
}

const DashboardAPI = {
  /** 工作台总览 */
  getOverview() {
    return request<unknown, DashboardOverview>({
      url: `${DASHBOARD_BASE_URL}/overview`,
      method: "get",
    });
  },

  /** 近 N 日运输量趋势（后端仅支持 7/30，越界会被钳制到 1~30） */
  getTrend(days = 7) {
    return request<unknown, OrderTrendPoint[]>({
      url: `${DASHBOARD_BASE_URL}/trend`,
      method: "get",
      params: { days },
    });
  },

  /** 最新待处理运单 */
  getTodo(limit = 5) {
    return request<unknown, PendingOrder[]>({
      url: `${DASHBOARD_BASE_URL}/todo`,
      method: "get",
      params: { limit },
    });
  },
};

export default DashboardAPI;
