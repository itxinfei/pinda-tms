import request from "@/utils/request";
import type { PageResponse } from "@/api/common";
import type { OrderRecord } from "@/api/order";
import type { AgencySimple, SysUser, TaskTransportRecord } from "@/api/transport-task";

const ORDER_BASE_URL = "/api/manager/transport-order-manager";

/**
 * 运单状态（口径见 TransportOrderVo 注释）
 */
export enum TransportOrderStatus {
  /** 新建 */
  CREATED = 1,
  /** 已装车，发往转运中心 */
  LOADED = 2,
  /** 到达 */
  ARRIVED = 3,
  /** 到达终端网点 */
  AT_TERMINAL = 4,
}

/** 调度状态 */
export enum SchedulingStatus {
  /** 待调度 */
  PENDING = 1,
  /** 未匹配线路 */
  NO_LINE = 2,
  /** 已调度 */
  SCHEDULED = 3,
}

/** 取派件任务信息（运单详情） */
export interface TaskPickupDispatch {
  id: string;
  /** 任务类型：1取件 2派件 */
  taskType: number;
  status: number;
  /** 签收状态：1已签收 2拒收 */
  signStatus?: number;
  agency?: AgencySimple;
  courier?: SysUser;
  estimatedStartTime?: string;
  actualStartTime?: string;
  estimatedEndTime?: string;
  actualEndTime?: string;
  confirmTime?: string;
  cancelTime?: string;
  assignedStatus?: number;
  mark?: string;
}

/** 运单列表项字段 */
export interface TransportOrderRecord {
  id: string;
  order?: OrderRecord;
  status: number;
  schedulingStatus?: number;
  createTime?: string;
}

/** 运单详情：含取派件任务与运输信息 */
export interface TransportOrderDetail extends TransportOrderRecord {
  taskPickup?: TaskPickupDispatch;
  taskDispatch?: TaskPickupDispatch;
  taskTransports?: TaskTransportRecord[];
}

/** 运单订单维度的过滤条件（后端在 order 子对象内取值） */
export interface TransportOrderOrderFilter {
  senderName?: string;
  senderPhone?: string;
  receiverName?: string;
  receiverPhone?: string;
  senderProvince?: { id: string };
  senderCity?: { id: string };
  senderCounty?: { id: string };
  receiverProvince?: { id: string };
  receiverCity?: { id: string };
  receiverCounty?: { id: string };
}

/** 运单分页查询条件（空字段不参与过滤） */
export interface TransportOrderPageQuery {
  page: number;
  pageSize: number;
  id?: string;
  status?: number;
  order?: TransportOrderOrderFilter;
}

const TransportOrderAPI = {
  /** 运单分页 */
  page(data: TransportOrderPageQuery) {
    return request<unknown, PageResponse<TransportOrderRecord>>({
      url: `${ORDER_BASE_URL}/page`,
      method: "post",
      data,
    });
  },
  /** 运单详情 */
  getDetail(id: string) {
    return request<unknown, TransportOrderDetail>({
      url: `${ORDER_BASE_URL}/${id}`,
      method: "get",
    });
  },
};

export default TransportOrderAPI;
