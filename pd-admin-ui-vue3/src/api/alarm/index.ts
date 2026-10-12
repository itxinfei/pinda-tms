import request from "@/utils/request";

/**
 * 告警中心（TRK-02）
 *
 * 接口由 pd-netty 提供（iot_alarm_record 表），经网关 /api/netty-service 前缀访问，
 * 与 BFF 分页结构不同：netty 返回 total，BFF 返回 counts。
 */
const ALARM_BASE_URL = "/api/netty-service/alarm";

/** 告警处理状态：0 未处理 / 1 已处理 */
export enum AlarmHandleStatus {
  /** 未处理 */
  PENDING = 0,
  /** 已处理 */
  HANDLED = 1,
}

/**
 * 告警类型语义码，与后端 com.itheima.pinda.enums.AlarmTypeEnum 枚举名一致。
 * 查询接口的 alarmType 参数只认语义码字符串（如 "SPEED_OVER"），不传数字码。
 */
export enum AlarmTypeKey {
  /** GPS：超速 */
  SPEED_OVER = "SPEED_OVER",
  /** GPS：长时间停留 */
  STAY_TOO_LONG = "STAY_TOO_LONG",
  /** GPS：偏离路线 */
  DEVIATE_ROUTE = "DEVIATE_ROUTE",
  /** GPS：车辆离线 */
  VEHICLE_OFFLINE = "VEHICLE_OFFLINE",
  /** 人工：货物破损 */
  GOODS_DAMAGED = "GOODS_DAMAGED",
  /** 人工：客户拒收 */
  REJECTED = "REJECTED",
  /** 人工：地址错误 */
  ADDRESS_ERROR = "ADDRESS_ERROR",
  /** 人工：车辆故障 */
  VEHICLE_BREAKDOWN = "VEHICLE_BREAKDOWN",
  /** 人工：货物损失 */
  CARGO_LOSS = "CARGO_LOSS",
  /** 人工：延误 */
  DELAY = "DELAY",
  /** 人工：其他异常 */
  OTHER = "OTHER",
}

/** 告警类型元数据（数字码为 iot_alarm_record.alarm_type 落库值） */
export interface AlarmTypeMeta {
  /** 落库数字码 */
  code: number;
  /** 查询用语义码 */
  key: AlarmTypeKey;
  /** 中文名称 */
  label: string;
}

/** GPS 自动检测告警（1-4） */
export const GPS_ALARM_TYPES: AlarmTypeMeta[] = [
  { code: 1, key: AlarmTypeKey.SPEED_OVER, label: "超速" },
  { code: 2, key: AlarmTypeKey.STAY_TOO_LONG, label: "长时间停留" },
  { code: 3, key: AlarmTypeKey.DEVIATE_ROUTE, label: "偏离路线" },
  { code: 4, key: AlarmTypeKey.VEHICLE_OFFLINE, label: "车辆离线" },
];

/** 一线人员人工上报告警（10-16） */
export const MANUAL_ALARM_TYPES: AlarmTypeMeta[] = [
  { code: 10, key: AlarmTypeKey.GOODS_DAMAGED, label: "货物破损" },
  { code: 11, key: AlarmTypeKey.REJECTED, label: "客户拒收" },
  { code: 12, key: AlarmTypeKey.ADDRESS_ERROR, label: "地址错误" },
  { code: 13, key: AlarmTypeKey.VEHICLE_BREAKDOWN, label: "车辆故障" },
  { code: 14, key: AlarmTypeKey.CARGO_LOSS, label: "货物损失" },
  { code: 15, key: AlarmTypeKey.DELAY, label: "延误" },
  { code: 16, key: AlarmTypeKey.OTHER, label: "其他异常" },
];

/** 数字码 → 元数据，表格列把记录中的数字码转中文 */
const ALARM_TYPE_CODE_MAP: Record<number, AlarmTypeMeta> = [...GPS_ALARM_TYPES, ...MANUAL_ALARM_TYPES].reduce(
  (map, item) => {
    map[item.code] = item;
    return map;
  },
  {} as Record<number, AlarmTypeMeta>,
);

/** 数字码转中文名称，未知码原样附带数字展示 */
export function alarmTypeText(code?: number): string {
  if (code === null || code === undefined) {
    return "-";
  }
  return ALARM_TYPE_CODE_MAP[code]?.label ?? `未知类型(${code})`;
}

/** 数字码转语义码，未知码返回 undefined（tag 配色等按语义判断时使用，禁止裸比数字） */
export function alarmTypeKeyOf(code?: number): AlarmTypeKey | undefined {
  if (code === null || code === undefined) {
    return undefined;
  }
  return ALARM_TYPE_CODE_MAP[code]?.key;
}

/** 是否为 GPS 自动告警（1-9 段） */
export function isGpsAlarm(code?: number): boolean {
  return code !== null && code !== undefined && code >= 1 && code <= 9;
}

/** 告警记录（iot_alarm_record） */
export interface AlarmRecord {
  /** 雪花 ID（项目统一按字符串消费） */
  id: string;
  /** 告警类型数字码（1-4 GPS / 10-16 人工） */
  alarmType: number;
  /** 运输任务 ID */
  transportTaskId?: string;
  /** 车辆 ID（netty 只存 ID，不关联车牌） */
  truckId?: string;
  /** 司机/快递员 ID（人工上报时为上报人 ID） */
  driverId?: string;
  /** 告警点经度（BD09，后端 BigDecimal 序列化为字符串） */
  longitude?: string;
  /** 告警点纬度（BD09） */
  latitude?: string;
  /** 告警内容 */
  alarmContent?: string;
  /** 告警时间（yyyy-MM-dd HH:mm:ss） */
  alarmTime?: string;
  /** 处理状态：0 未处理 / 1 已处理 */
  status: number;
  /** 入库时间 */
  createTime?: string;
}

/** 告警分页查询条件（空字段不参与过滤） */
export interface AlarmPageQuery {
  page: number;
  pageSize: number;
  /** 运输任务 ID（精确匹配） */
  transportTaskId?: string;
  /** 告警类型语义码，传 AlarmTypeKey，禁止传数字 */
  alarmType?: string;
  /** 处理状态：0 未处理 / 1 已处理 */
  status?: number;
}

/** netty 分页结果（字段为 total，区别于 BFF PageResponse.counts） */
export interface AlarmPageResult {
  items: AlarmRecord[];
  total: number;
  pages: number;
  page: number;
  pageSize: number;
}

const AlarmAPI = {
  /** 告警分页查询（按告警时间倒序） */
  page(params: AlarmPageQuery) {
    return request<unknown, AlarmPageResult>({
      url: ALARM_BASE_URL,
      method: "get",
      params,
    });
  },

  /** 标记告警已处理（幂等：重复处理不报错） */
  handle(id: string) {
    return request<unknown, AlarmRecord>({
      url: `${ALARM_BASE_URL}/${id}/handle`,
      method: "put",
    });
  },
};

export default AlarmAPI;
