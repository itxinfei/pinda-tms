import request from "@/utils/request";

/**
 * 车辆轨迹/定位（xfwl-netty）
 *
 * 经网关 /api/netty-service 前缀访问；分页壳与 BFF 不同：
 * Result.data = { items, total, pages, page, pageSize }。
 */
const TRACE_BASE_URL = "/api/netty-service/trace";

/** GPS 定位记录（iot_truck_location） */
export interface LocationRecordItem {
  /** 雪花 ID */
  id: string;
  /** 业务 ID：车辆 ID 或快递员 ID */
  businessId: string;
  /** 业务类型：1 车辆 / 2 快递员 */
  businessType: number;
  /** 司机/快递员名称 */
  name?: string;
  /** 司机/快递员电话 */
  phone?: string;
  /** 车牌号 */
  licensePlate?: string;
  /** 经度（出口归一恒 WGS84，批2 2026-10-17；BigDecimal 可能序列化为字符串，消费时 Number() 转换） */
  longitude?: string | number;
  /** 纬度（出口归一恒 WGS84） */
  latitude?: string | number;
  /** 该行数据落库时所用坐标系（1=WGS84/3=BD09，出口已按行归一，仅溯源用） */
  coordSystem?: number;
  /** 数据来源 MOBILE/PDA/JT808/NETTY_TCP/HTTP */
  source?: string;
  /** 所属车队 */
  team?: string;
  /** 关联运输任务 ID */
  transportTaskId?: string;
  /** 设备上报时间 */
  reportTime?: string;
  /** 入库时间 */
  createTime?: string;
  /** BD09 经度（出参按 coordSystem 动态转换，管理端百度地图直接用） */
  bd09Lng?: string | number;
  /** BD09 纬度（出参按 coordSystem 动态转换） */
  bd09Lat?: string | number;
}

/** 轨迹分页查询条件（POST body，空字段不参与过滤） */
export interface TracePageQuery {
  page: number;
  pageSize: number;
  /** 业务 ID（车辆/快递员） */
  businessId?: string;
  /** 业务类型别名：truck / courier */
  type?: "truck" | "courier";
  /** 运输任务 ID */
  transportTaskId?: string;
  /** 车牌号（模糊匹配） */
  licensePlate?: string;
}

/** netty 分页结果（total 非 counts、pageSize 驼峰） */
export interface TracePageResult {
  items: LocationRecordItem[];
  total: number;
  pages: number;
  page: number;
  pageSize: number;
}

const TraceAPI = {
  /** 轨迹分页查询（按入库时间倒序） */
  page(data: TracePageQuery) {
    return request<unknown, TracePageResult>({
      url: `${TRACE_BASE_URL}/page`,
      method: "post",
      data,
    });
  },
  /** 批量最新位置（每个业务对象最新一条，供在途监控轮询；出参含 bd09Lng/bd09Lat） */
  latestAll(params?: { type?: "truck" | "courier"; licensePlate?: string }) {
    return request<unknown, LocationRecordItem[]>({
      url: `${TRACE_BASE_URL}/latestAll`,
      method: "get",
      params,
    });
  },
  /** 轨迹回放（按上报时间升序全量轨迹，出参含 bd09Lng/bd09Lat） */
  replay(params: { businessId: string; type?: "truck" | "courier" }) {
    return request<unknown, LocationRecordItem[]>({
      url: `${TRACE_BASE_URL}/replay`,
      method: "get",
      params,
    });
  },
};

export default TraceAPI;
