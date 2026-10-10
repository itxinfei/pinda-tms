import request from "@/utils/request";
import type { PageResponse } from "@/api/common";
import type { AgencySimple } from "@/api/transport-task";
import type { TransportLineTypeSimple } from "@/api/common-simple";

const LINE_BASE_URL = "/api/manager/transfor-center/bussiness/transportLine";

/** 线路信息 */
export interface TransportLineRecord {
  id: string;
  name: string;
  lineNumber?: string;
  /** 所属机构 */
  agency?: AgencySimple;
  transportLineType?: TransportLineTypeSimple;
  /** 起始地机构 */
  startAgency?: AgencySimple;
  /** 目的地机构 */
  endAgency?: AgencySimple;
  /** 距离（km） */
  distance?: number;
  /** 成本（元） */
  cost?: number;
  /** 预计时间 */
  estimatedTime?: number;
}

/** 线路分页查询条件（GET 参数，空字段不参与过滤） */
export interface TransportLinePageQuery {
  page: number;
  pageSize: number;
  name?: string;
  transportLineTypeId?: string;
  lineNumber?: string;
}

const TransportLineAPI = {
  /** 线路分页 */
  page(params: TransportLinePageQuery) {
    return request<unknown, PageResponse<TransportLineRecord>>({
      url: `${LINE_BASE_URL}/page`,
      method: "get",
      params,
    });
  },
  /** 线路详情 */
  getDetail(id: string) {
    return request<unknown, TransportLineRecord>({
      url: `${LINE_BASE_URL}/${id}`,
      method: "get",
    });
  },
};

export default TransportLineAPI;
