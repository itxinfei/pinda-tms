import request from "@/utils/request";

/**
 * 运输闭环看板（DSP-01）
 *
 * 聚合接口由 xf-web-manager 提供（经网关 /api/manager 前缀），数据源 xf-aggregation。
 * 阶段/状态的中文名与下钻参数均由后端按枚举真源下发，前端不做状态码映射与硬编码。
 */
const BOARD_BASE_URL = "/api/manager/transport-task-manager";

/** 看板阶段统计项（六阶段流程条） */
export interface BoardStage {
  /** 阶段标识：DISPATCH/ASSIGN/DEPART/TRANSIT/ARRIVE/DELIVER */
  key: string;
  /** 阶段中文名 */
  name: string;
  /** 该阶段当前停留量 */
  count: number;
  /** 下钻数据源：task=运输任务分页 transportOrder=运单分页 */
  drillType: "task" | "transportOrder";
  /** 下钻过滤字段：status | schedulingStatus */
  drillField?: string;
  /** 下钻过滤状态值列表（到达阶段含到达/到达终端网点两个状态） */
  drillStatuses?: number[];
}

/** 状态-计数统计项（含状态码，供图表点击下钻；中文名由后端给出） */
export interface StatusCount {
  code: number;
  name: string;
  count: number;
}

/** 运输闭环看板聚合数据 */
export interface TransportBoard {
  stages: BoardStage[];
  taskStatusCounts: StatusCount[];
  transportOrderStatusCounts: StatusCount[];
}

const TransportBoardAPI = {
  /** 看板聚合数据（六阶段 + 任务/运单状态分布） */
  getBoard() {
    return request<unknown, TransportBoard>({
      url: `${BOARD_BASE_URL}/board`,
      method: "get",
    });
  },
};

export default TransportBoardAPI;
