// 快递员端枚举集中映射：取派任务状态 / 任务类型 / 签收状态 / 异常类型
// 修改点：driver 复制本文件后替换为本端枚举（运输任务 / 司机作业单状态不同）。
// 禁止页面硬编码数字，一律走下方映射。

// 取派任务状态 PickupDispatchTaskStatus（列表/详情 status 用此）
export type TaskStatus = 1 | 2 | 3 | 4 | 5
export const TASK_STATUS_MAP: Record<
  number,
  { text: string; type: string }
> = {
  1: { text: '待执行', type: 'warning' },
  2: { text: '进行中', type: 'primary' },
  3: { text: '待确认', type: 'info' },
  4: { text: '已完成', type: 'success' },
  5: { text: '已取消', type: 'info' },
}
export function taskStatusView(status?: number) {
  if (status == null) return { text: '未知', type: 'info' }
  return TASK_STATUS_MAP[status] || { text: '未知', type: 'info' }
}

// 任务类型 PickupDispatchTaskType：1 取件 / 2 派件
export const TASK_TYPE_MAP: Record<number, { text: string }> = {
  1: { text: '取件' },
  2: { text: '派件' },
}
export function taskTypeView(type?: number) {
  return (type != null && TASK_TYPE_MAP[type]) || { text: '未知' }
}

// 签收状态 PickupDispatchTaskSignStatus：1 已签收 / 2 拒收
export const SIGN_STATUS_MAP: Record<number, { text: string }> = {
  1: { text: '已签收' },
  2: { text: '已拒收' },
}

// 派送签收二态（delivered 接口 status 仅接受字符串 "1"/"0"）
export const DELIVERED_SIGN = '1' // 签收
export const DELIVERED_REJECT = '0' // 拒收

// 快递员可上报异常类型（仅三类，其余为司机侧，禁报）
export const COURIER_EXCEPTION_TYPES = [
  { value: 'GOODS_DAMAGED', text: '货物破损' },
  { value: 'REJECTED', text: '客户拒收' },
  { value: 'ADDRESS_ERROR', text: '地址错误' },
]
