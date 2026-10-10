import request from "@/utils/request";
import type { PageResponse } from "@/api/common";

const TASK_BASE_URL = "/api/manager/transport-task-manager";

/**
 * 运输任务状态（口径见 TaskTransportVo 注释）
 */
export enum TransportTaskStatus {
  /** 待执行（对应待提货） */
  PENDING = 1,
  /** 进行中（对应在途） */
  PROCESSING = 2,
  /** 待确认（保留状态） */
  TO_CONFIRM = 3,
  /** 已完成（对应已交付） */
  FINISHED = 4,
  /** 已取消 */
  CANCELLED = 5,
}

/** 任务分配状态 */
export enum AssignedStatus {
  /** 未分配 */
  UNASSIGNED = 1,
  /** 已分配 */
  ASSIGNED = 2,
  /** 待人工分配 */
  MANUAL = 3,
}

/** 满载状态 */
export enum LoadingStatus {
  /** 半载 */
  HALF = 1,
  /** 满载 */
  FULL = 2,
  /** 空载 */
  EMPTY = 3,
}

/** 机构简要信息 */
export interface AgencySimple {
  id: string;
  name: string;
  subAgencies?: AgencySimple[];
}

/** 用户（司机/快递员）信息 */
export interface SysUser {
  userId: string;
  username?: string;
  name?: string;
  mobile?: string;
  /** 岗位：1员工 2快递员 3司机 */
  station?: number;
  stationName?: string;
  avatar?: string;
  agency?: AgencySimple;
}

/** 车次简要信息 */
export interface TransportTrips {
  id: string;
  name?: string;
  departureTime?: string;
  arrivalTime?: string;
  /** 周期：1天 2周 3月 */
  period?: number;
  periodName?: string;
}

/** 车辆信息 */
export interface Truck {
  id: string;
  licensePlate?: string;
  brand?: string;
  allowableLoad?: number;
  allowableVolume?: number;
  deviceGpsId?: string;
}

/** 运输任务列表项字段 */
export interface TaskTransportRecord {
  id: string;
  transportTrips?: TransportTrips;
  startAgency?: AgencySimple;
  endAgency?: AgencySimple;
  status: number;
  assignedStatus?: number;
  loadingStatus?: number;
  truck?: Truck;
  drivers?: SysUser[];
  planDepartureTime?: string;
  actualDepartureTime?: string;
  createTime?: string;
  transportOrderCount?: number;
}

/** 运输任务详情：含完整时间节点与关联运单 */
export interface TaskTransportDetail extends TaskTransportRecord {
  cargoPickUpPicture?: string;
  cargoPicture?: string;
  transportCertificate?: string;
  planArrivalTime?: string;
  actualArrivalTime?: string;
  planPickUpGoodsTime?: string;
  actualPickUpGoodsTime?: string;
  planDeliveryTime?: string;
  actualDeliveryTime?: string;
  deliverPicture?: string;
  deliveryLatitude?: string;
  deliveryLongitude?: string;
  deliverLatitude?: string;
  deliverLongitude?: string;
  transportOrders?: { id: string; status?: number }[];
}

/** 运输任务分页查询条件（空字段不参与过滤） */
export interface TaskTransportPageQuery {
  page: number;
  pageSize: number;
  id?: string;
  status?: number;
  driverName?: string;
}

const TransportTaskAPI = {
  /** 运输任务分页 */
  page(data: TaskTransportPageQuery) {
    return request<unknown, PageResponse<TaskTransportRecord>>({
      url: `${TASK_BASE_URL}/page`,
      method: "post",
      data,
    });
  },
  /** 运输任务详情 */
  getDetail(id: string) {
    return request<unknown, TaskTransportDetail>({
      url: `${TASK_BASE_URL}/${id}`,
      method: "get",
    });
  },
};

export default TransportTaskAPI;
