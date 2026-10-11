import request from "@/utils/request";
import type { PageResponse } from "@/api/common";
import type { AgencySimple } from "@/api/transport-task";
import type { SysUserSimple } from "@/api/common-simple";

const FLEET_BASE_URL = "/api/manager/transfor-center/bussiness/fleet";

/** 车队 */
export interface FleetRecord {
  id?: string;
  /** 车队名称 */
  name: string;
  /** 车队编号 */
  fleetNumber?: string;
  /** 所属机构（分页/详情由后端回填） */
  agency?: AgencySimple;
  /** 负责人（分页/详情由后端回填为用户对象） */
  manager?: SysUserSimple;
  /** 车辆总数 */
  truckCount?: number;
  /** 司机总数 */
  driverCount?: number;
  /** 状态：1 启用，0 停用（分页接口只返回启用项） */
  status?: number;
}

/** 车队保存入参：负责人只提交 userId，由 BFF 转为 manager 字段 */
export interface FleetSavePayload {
  id?: string;
  name: string;
  fleetNumber?: string;
  manager?: { userId: string } | null;
}

/** 车队分页查询条件（GET 参数，空字段不参与过滤；manager 为负责人 userId） */
export interface FleetPageQuery {
  page: number;
  pageSize: number;
  name?: string;
  fleetNumber?: string;
  manager?: string;
}

const FleetAPI = {
  /** 车队分页 */
  page(params: FleetPageQuery) {
    return request<unknown, PageResponse<FleetRecord>>({
      url: `${FLEET_BASE_URL}/page`,
      method: "get",
      params,
    });
  },
  /** 车队详情 */
  getDetail(id: string) {
    return request<unknown, FleetRecord>({
      url: `${FLEET_BASE_URL}/${id}`,
      method: "get",
    });
  },
  /** 新增车队 */
  save(data: FleetSavePayload) {
    return request<unknown, FleetRecord>({
      url: FLEET_BASE_URL,
      method: "post",
      data,
    });
  },
  /** 更新车队 */
  update(id: string, data: FleetSavePayload) {
    return request<unknown, FleetRecord>({
      url: `${FLEET_BASE_URL}/${id}`,
      method: "put",
      data,
    });
  },
  /** 停用车队（软停用） */
  disable(id: string) {
    return request<unknown, unknown>({
      url: `${FLEET_BASE_URL}/${id}`,
      method: "delete",
    });
  },
};

export default FleetAPI;
