import request from "@/utils/request";

/**
 * 管理端工作台（dashboard）
 *
 * 聚合接口由 pd-web-manager 提供（经网关 /api/manager 前缀）。
 * 订单统计源自 pd-oms，在途车辆源自 pd-work；异常告警走独立的 netty 接口（见 @/api/alarm）。
 */
const DASHBOARD_BASE_URL = "/api/manager/dashboard";

/** 名称-数值统计项（中文名由后端给出，前端不做状态码映射） */
export interface NameValue {
  name: string;
  value: number;
}

/** 工作台总览（KPI + 订单三桶 + 今日签收 + 在途车辆 + 任务五态） */
export interface DashboardOverview {
  /** 今日新增订单数 */
  todayOrderCount: number;
  /** 在途车辆数（有进行中任务的去重车辆数） */
  onTruckCount: number;
  /** 待处理订单数（订单状态 1-5） */
  pendingCount: number;
  /** 运输中订单数（订单状态 6-9） */
  inTransitCount: number;
  /** 已签收订单数（订单状态 10） */
  completedCount: number;
  /** 今日签收数（update_time 当日且状态10近似） */
  todayCompletedCount: number;
  /** 今日新增运输任务数 */
  todayTaskCount: number;
  /** 运输任务五态计数 */
  taskStatusCounts: NameValue[];
}

/** 大屏线路运量项 */
export interface LineVolume {
  /** 线路名称（补不到线路时用车次名） */
  name: string;
  /** 近 7 日运输任务量 */
  taskCount: number;
}

/** 运营大屏汇总（告警数/车辆定位由前端直连 netty，不在此对象内） */
export interface BigScreenSummary {
  /** 今日订单数 */
  todayOrderCount: number;
  /** 今日运单数（去重 order_id） */
  todayTransportOrderCount: number;
  /** 今日运输任务数 */
  todayTaskCount: number;
  /** 在途车辆数 */
  activeTruckCount: number;
  /** 待执行车辆数 */
  pendingTruckCount: number;
  /** 空闲车辆数（总数-在途-待执行） */
  idleTruckCount: number;
  /** 正常车辆总数 */
  totalTruckCount: number;
  /** 运输任务五态计数 */
  taskStatusCounts: NameValue[];
  /** 近 7 日各线路运量 Top10 */
  lineVolumes: LineVolume[];
  /** 准时率（百分比，0~100；无已送达任务为 null，前端显示 --） */
  onTimeRate?: number | null;
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

  /** 运营大屏汇总（订单/运单/车辆/任务五态/线路运量） */
  getBigScreenSummary() {
    return request<unknown, BigScreenSummary>({
      url: `${DASHBOARD_BASE_URL}/bigscreen/summary`,
      method: "get",
    });
  },
};

export default DashboardAPI;
