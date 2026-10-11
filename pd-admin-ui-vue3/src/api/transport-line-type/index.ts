import request from "@/utils/request";
import type { PageResponse } from "@/api/common";
import type { SysUserSimple } from "@/api/common-simple";

const TRANSPORT_LINE_TYPE_BASE_URL = "/api/manager/transfor-center/bussiness/transportLineType";

/** 线路类型 */
export interface TransportLineTypeRecord {
  id?: string;
  /** 线路类型名称 */
  name: string;
  /** 线路类型编码 */
  typeNumber: string;
  /** 起始地机构类型：1 分公司 / 2 一级转运中心 / 3 二级转运中心 / 4 网点 */
  startAgencyType?: number;
  /** 起始地机构类型名称（后端回填） */
  startAgencyTypeName?: string;
  /** 目的地机构类型 */
  endAgencyType?: number;
  /** 目的地机构类型名称（后端回填） */
  endAgencyTypeName?: string;
  /** 最后更新时间（后端回填字符串） */
  lastUpdateTime?: string;
  /** 更新人（后端回填） */
  updater?: SysUserSimple;
  /** 状态：1 启用，0 停用（分页接口只返回启用项） */
  status?: number;
}

/** 线路类型分页查询条件（GET 参数，空字段不参与过滤） */
export interface TransportLineTypePageQuery {
  page: number;
  pageSize: number;
  name?: string;
  typeNumber?: string;
}

const TransportLineTypeAPI = {
  /** 线路类型分页 */
  page(params: TransportLineTypePageQuery) {
    return request<unknown, PageResponse<TransportLineTypeRecord>>({
      url: `${TRANSPORT_LINE_TYPE_BASE_URL}/page`,
      method: "get",
      params,
    });
  },
  /** 线路类型详情 */
  getDetail(id: string) {
    return request<unknown, TransportLineTypeRecord>({
      url: `${TRANSPORT_LINE_TYPE_BASE_URL}/${id}`,
      method: "get",
    });
  },
  /** 新增线路类型 */
  save(data: TransportLineTypeRecord) {
    return request<unknown, TransportLineTypeRecord>({
      url: TRANSPORT_LINE_TYPE_BASE_URL,
      method: "post",
      data,
    });
  },
  /** 更新线路类型 */
  update(id: string, data: TransportLineTypeRecord) {
    return request<unknown, TransportLineTypeRecord>({
      url: `${TRANSPORT_LINE_TYPE_BASE_URL}/${id}`,
      method: "put",
      data,
    });
  },
  /** 停用线路类型（软停用） */
  disable(id: string) {
    return request<unknown, unknown>({
      url: `${TRANSPORT_LINE_TYPE_BASE_URL}/${id}`,
      method: "delete",
    });
  },
};

export default TransportLineTypeAPI;
