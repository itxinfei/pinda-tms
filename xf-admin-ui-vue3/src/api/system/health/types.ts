/** 服务健康查询参数（接口为全量实时探测，无分页参数） */
export interface HealthQueryParams {
  /** 关键字：服务名 */
  keywords?: string;
}

/** 服务健康对象 */
export interface HealthItem {
  /** Nacos 服务名（如 xf-auth） */
  serviceName: string;
  /** UP / DOWN */
  status: string;
  instances: number;
  /** 实例地址列表 ip:port（逗号分隔） */
  instanceList: string;
  /** 探测耗时 ms */
  latency: number | null;
}

/** 健康概览 */
export interface HealthOverview {
  total: number;
  upCount: number;
  downCount: number;
}
