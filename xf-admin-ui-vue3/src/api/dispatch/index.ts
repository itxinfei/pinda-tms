import request from "@/utils/request";

/**
 * 调度工作台（FR-04 / DSP-03，原"调度一张图"，2026-10-19 菜单 IA 优化更名）
 *
 * 经网关 /api/manager 前缀访问 xf-web-manager 的 DispatchBoardController：
 * - POST /dispatch/board/page   待调度订单 + 可用线路聚合
 * - GET  /dispatch/line/{fromId}/{toId} 线路方案
 * - POST /dispatch/assign       一键派单
 *
 * 车辆实时位置不在本模块：直连 xf-netty 的 /api/netty-service/trace/latestAll（见 @/api/trace）。
 */
const DISPATCH_BASE_URL = "/api/manager/dispatch";

/** 待调度订单地图点（网点入库状态） */
export interface DispatchOrderPoint {
  /** 订单 id */
  id: string;
  /** 订单号 */
  orderNo: string;
  /** 发件人 */
  senderName?: string;
  /** 收件人 */
  receiverName?: string;
  /** 当前入库网点 id */
  currentOrgId?: string;
  /** 当前入库网点名称 */
  currentOrgName?: string;
  /** 网点经度（WGS84，需按地图供应商转换） */
  longitude?: string;
  /** 网点纬度 */
  latitude?: string;
  /** 下单时间 */
  createTime?: string;
  /** 货物件数（OrderCargo 汇总，右栏候选车辆载重匹配用） */
  quantity?: number;
  /** 货物总重 kg */
  totalWeight?: number;
  /** 货物总体积 m³ */
  totalVolume?: number;
}

/** 线路 */
export interface DispatchLine {
  id: string;
  name?: string;
  startOrgId?: string;
  endOrgId?: string;
  startOrgName?: string;
  endOrgName?: string;
  /** 起始机构经度（WGS84，前端按地图供应商转换） */
  startOrgLng?: string;
  /** 起始机构纬度 */
  startOrgLat?: string;
  /** 目的机构经度（WGS84） */
  endOrgLng?: string;
  /** 目的机构纬度 */
  endOrgLat?: string;
  /** 里程（公里） */
  distance?: number;
  /** 成本 */
  cost?: number;
  /** 预计时长 */
  estimatedTime?: number;
}

/** 汇总统计 */
export interface DispatchSummary {
  /** 待调度订单数 */
  pendingOrderCount: number;
  /** 线路数 */
  lineCount: number;
  /** 覆盖网点数 */
  orgCount: number;
}

/** 聚合返回 */
export interface DispatchBoard {
  orders: DispatchOrderPoint[];
  lines: DispatchLine[];
  summary: DispatchSummary;
}

/** 派单入参 */
export interface DispatchAssignParams {
  /** 待调度订单 id 列表 */
  orderIds: string[];
  /** 承运车辆 id */
  truckId: string;
  /** 车次 id（可选） */
  tripsId?: string;
}

/** 派单结果 */
export interface DispatchAssignResult {
  /** 创建的运输单数 */
  transportOrderCount: number;
  /** 新建的运输任务 id */
  taskId?: string;
  startOrgId?: string;
  endOrgId?: string;
}

const DispatchAPI = {
  /** 调度工作台聚合数据 */
  board(currentAgencyId?: string) {
    return request<unknown, DispatchBoard>({
      url: `${DISPATCH_BASE_URL}/board/page`,
      method: "post",
      params: { currentAgencyId },
    });
  },

  /** 线路方案 */
  line(fromId: string, toId: string) {
    return request<unknown, DispatchLine[]>({
      url: `${DISPATCH_BASE_URL}/line/${fromId}/${toId}`,
      method: "get",
    });
  },

  /** 一键派单 */
  assign(data: DispatchAssignParams) {
    return request<unknown, DispatchAssignResult>({
      url: `${DISPATCH_BASE_URL}/assign`,
      method: "post",
      data,
    });
  },
};

export default DispatchAPI;