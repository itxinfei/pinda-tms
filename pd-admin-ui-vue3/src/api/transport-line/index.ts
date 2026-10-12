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

/** 线路保存入参：机构/类型均为选填嵌套对象，清空时传 null（pd-base 实体配 FieldStrategy.ALWAYS 落 null） */
export interface TransportLineSavePayload {
  id?: string;
  /** 线路名称（必填） */
  name: string;
  /** 线路编号（选填） */
  lineNumber?: string | null;
  /** 所属机构（必填，编辑时必须原样回传，否则新建会被 BFF 回退为当前用户机构/缺省 1） */
  agency?: { id: string } | null;
  /** 起始机构（选填） */
  startAgency?: { id: string } | null;
  /** 目的机构（选填） */
  endAgency?: { id: string } | null;
  /** 线路类型（选填） */
  transportLineType?: { id: string } | null;
  /** 距离 km（选填） */
  distance?: number | null;
  /** 成本 元（选填） */
  cost?: number | null;
  /** 预计时间 分钟（选填，整数） */
  estimatedTime?: number | null;
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
  /** 新增线路 */
  save(data: TransportLineSavePayload) {
    return request<unknown, TransportLineRecord>({
      url: LINE_BASE_URL,
      method: "post",
      data,
    });
  },
  /** 更新线路 */
  update(id: string, data: TransportLineSavePayload) {
    return request<unknown, TransportLineRecord>({
      url: `${LINE_BASE_URL}/${id}`,
      method: "put",
      data,
    });
  },
  /** 删除线路（BFF 实为软停用；线路下存在关联车次时后端 400 拒绝） */
  remove(id: string) {
    return request<unknown, unknown>({
      url: `${LINE_BASE_URL}/${id}`,
      method: "delete",
    });
  },
};

export default TransportLineAPI;
