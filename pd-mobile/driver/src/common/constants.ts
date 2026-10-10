// 司机端枚举集中映射：司机作业单状态 / 运输任务状态 / 关联订单状态 / 司机侧异常类型
// 修改点：由 courier 端 constants.ts 重写为本端枚举。
// 禁止页面硬编码数字，一律走下方映射。
//
// ⚠️ 同名 CONFIRM 不同义：
//   - DriverJobStatus.CONFIRM = 3 → 改派（司机作业单）
//   - TransportTaskStatus.CONFIRM = 3 → 待确认（运输任务）
// 两者按枚举类区分，切勿混用。

// ============ 司机作业单状态 DriverJobStatus ============
export type DriverJobStatus = 1 | 2 | 3 | 4 | 5
export const DRIVER_JOB_STATUS_MAP: Record<
  number,
  { text: string; type: string }
> = {
  1: { text: '待执行', type: 'warning' },
  2: { text: '进行中', type: 'primary' },
  3: { text: '改派', type: 'info' },
  4: { text: '已完成', type: 'success' },
  5: { text: '已作废', type: 'info' },
}
export function driverJobStatusView(status?: number) {
  if (status == null) return { text: '未知', type: 'info' }
  return DRIVER_JOB_STATUS_MAP[status] || { text: '未知', type: 'info' }
}

// ============ 运输任务状态 TransportTaskStatus ============
export type TransportTaskStatus = 1 | 2 | 3 | 4 | 5
export const TRANSPORT_TASK_STATUS_MAP: Record<
  number,
  { text: string; type: string }
> = {
  1: { text: '待执行', type: 'warning' },
  2: { text: '进行中', type: 'primary' },
  3: { text: '待确认', type: 'info' },
  4: { text: '已完成', type: 'success' },
  5: { text: '已取消', type: 'info' },
}
export function transportTaskStatusView(status?: number) {
  if (status == null) return { text: '未知', type: 'info' }
  return TRANSPORT_TASK_STATUS_MAP[status] || { text: '未知', type: 'info' }
}

// ============ 关联订单状态 OrderStatus ============
export type OrderStatus =
  | 23000
  | 23005
  | 23006
  | 23009
  | 23010
  | 23011
export const ORDER_STATUS_MAP: Record<number, { text: string; type: string }> = {
  23000: { text: '待取件', type: 'warning' },
  23005: { text: '运输中', type: 'primary' },
  23006: { text: '网点出库', type: 'info' },
  23009: { text: '已签收', type: 'success' },
  23010: { text: '拒收', type: 'error' },
  23011: { text: '已取消', type: 'info' },
}
export function orderStatusView(status?: number) {
  if (status == null) return { text: '未知', type: 'info' }
  return ORDER_STATUS_MAP[status] || { text: '未知', type: 'info' }
}

// ============ 司机侧异常类型（仅 4 类）============
export const DRIVER_EXCEPTION_TYPES = [
  { value: 'VEHICLE_BREAKDOWN', text: '车辆故障' },
  { value: 'CARGO_LOSS', text: '货物损失' },
  { value: 'DELAY', text: '延误' },
  { value: 'OTHER', text: '其他' },
]
