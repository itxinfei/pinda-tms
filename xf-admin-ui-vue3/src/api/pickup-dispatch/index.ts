import request from "@/utils/request";
import type { PageResponse } from "@/api/common";
import type { OrderRecord } from "@/api/order";
import type { AgencySimple, SysUser } from "@/api/transport-task";

const PICKUP_DISPATCH_BASE_URL = "/api/manager/pickup-dispatch-task-manager";

/**
 * 取派件任务类型
 */
export enum PickupDispatchTaskType {
  /** 取件任务 */
  PICKUP = 1,
  /** 派件任务 */
  DISPATCH = 2,
}

/**
 * 取派件任务状态（2 为保留状态，当前不使用）
 */
export enum PickupDispatchStatus {
  /** 待执行（待上门/须交接） */
  PENDING = 1,
  /** 待确认（待妥投/须交件） */
  TO_CONFIRM = 3,
  /** 已完成 */
  FINISHED = 4,
  /** 已取消 */
  CANCELLED = 5,
}

/** 签收状态 */
export enum SignStatus {
  /** 已签收 */
  SIGNED = 1,
  /** 拒收 */
  REJECTED = 2,
}

/** 取派件任务列表项字段（列表 VO 已含全部展示字段） */
export interface PickupDispatchRecord {
  id: string;
  order?: OrderRecord;
  /** 任务类型：1取件 2派件 */
  taskType: number;
  status: number;
  /** 签收状态：1已签收 2拒收 */
  signStatus?: number;
  agency?: AgencySimple;
  courier?: SysUser;
  transportOrder?: { id: string };
  estimatedStartTime?: string;
  actualStartTime?: string;
  estimatedEndTime?: string;
  actualEndTime?: string;
  confirmTime?: string;
  cancelTime?: string;
  assignedStatus?: number;
  mark?: string;
  createTime?: string;
}

/** 订单维度过滤（后端在 order 子对象内取值） */
export interface PickupDispatchOrderFilter {
  senderName?: string;
  receiverName?: string;
}

/** 取派件分页查询条件（空字段不参与过滤） */
export interface PickupDispatchPageQuery {
  page: number;
  pageSize: number;
  taskType?: number;
  status?: number;
  courier?: { name: string };
  order?: PickupDispatchOrderFilter;
}

const PickupDispatchAPI = {
  /** 取派件任务分页 */
  page(data: PickupDispatchPageQuery) {
    return request<unknown, PageResponse<PickupDispatchRecord>>({
      url: `${PICKUP_DISPATCH_BASE_URL}/page`,
      method: "post",
      data,
    });
  },
};

export default PickupDispatchAPI;
