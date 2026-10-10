import request from "@/utils/request";

const STATION_BASE_URL = "/api/auth/station";

/** 岗位（网点作业岗位） */
export interface Station {
  id: number;
  tenantId?: number;
  name: string;
  orgId: number;
  status: boolean;
  describe?: string;
  createTime?: string;
  createUser?: number;
  updateTime?: string;
  updateUser?: number;
}

/** 岗位分页查询参数 */
export interface StationQuery {
  pageNo: number;
  size: number;
  name?: string;
  orgId?: number;
}

/** 岗位表单提交体 */
export type StationForm = Partial<Station> & {
  name: string;
  orgId: number;
};

/** 分页结果 */
export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

const StationAPI = {
  /** 岗位分页 */
  page(params: StationQuery) {
    return request<unknown, PageResult<Station>>({
      url: `${STATION_BASE_URL}/page`,
      method: "get",
      params,
    });
  },
  /** 岗位详情 */
  getDetail(id: number) {
    return request<unknown, Station>({
      url: `${STATION_BASE_URL}/detail`,
      method: "get",
      params: { id },
    });
  },
  /** 新增岗位，返回新岗位ID */
  create(data: StationForm) {
    return request<unknown, number>({ url: `${STATION_BASE_URL}`, method: "post", data });
  },
  /** 修改岗位 */
  update(data: StationForm) {
    return request({ url: `${STATION_BASE_URL}`, method: "put", data });
  },
  /** 删除岗位 */
  remove(id: number) {
    return request({ url: `${STATION_BASE_URL}`, method: "delete", params: { id } });
  },
};

export default StationAPI;
