import request from "@/utils/request";
import type { PageResponse } from "@/api/common";
import type { AgencySimple, SysUser, TaskTransportRecord } from "@/api/transport-task";

const DRIVER_JOB_BASE_URL = "/api/manager/driver-job-manager";

/**
 * 司机作业单状态
 */
export enum DriverJobStatus {
  /** 待执行（对应待提货） */
  PENDING = 1,
  /** 进行中（对应在途） */
  PROCESSING = 2,
  /** 改派 */
  REASSIGNED = 3,
  /** 已完成（对应已交付） */
  FINISHED = 4,
  /** 已作废 */
  CANCELLED = 5,
}

/** 司机作业单列表项字段（列表 VO 已含全部展示字段） */
export interface DriverJobRecord {
  id: string;
  startAgency?: AgencySimple;
  endAgency?: AgencySimple;
  status: number;
  driver?: SysUser;
  taskTransport?: TaskTransportRecord;
  /** 提货对接人 */
  startHandover?: string;
  /** 交付对接人 */
  finishHandover?: string;
  planDepartureTime?: string;
  actualDepartureTime?: string;
  planArrivalTime?: string;
  actualArrivalTime?: string;
  createTime?: string;
}

/** 司机作业单分页查询条件（空字段不参与过滤） */
export interface DriverJobPageQuery {
  page: number;
  pageSize: number;
  id?: string;
  status?: number;
  driver?: { name: string };
}

const DriverJobAPI = {
  /** 司机作业单分页 */
  page(data: DriverJobPageQuery) {
    return request<unknown, PageResponse<DriverJobRecord>>({
      url: `${DRIVER_JOB_BASE_URL}/page`,
      method: "post",
      data,
    });
  },
};

export default DriverJobAPI;
