// 订单状态：客户端 7 档视图（D-48，2026-10-09 拍板），集中映射，禁止页面硬编码数字
// 状态码 =《07》§一 紧凑口径 1~12（旧 23xxx 已全线作废，勿回填）。
// 修改点：courier / driver 复制本文件后替换为本端状态映射（取派任务 / 运输任务枚举不同）。
export type CustomerStatusLevel = 1 | 2 | 3 | 4 | 5 | 6 | 7

export const ORDER_STATUS_MAP: Record<
  number,
  { level: CustomerStatusLevel; text: string; type: string }
> = {
  1: { level: 1, text: '待取件', type: 'warning' },
  2: { level: 2, text: '已取件', type: 'primary' },
  3: { level: 2, text: '网点自寄', type: 'primary' },
  4: { level: 2, text: '网点入库', type: 'primary' },
  5: { level: 2, text: '待装车', type: 'primary' },
  6: { level: 3, text: '运输中', type: 'primary' },
  7: { level: 3, text: '运输中', type: 'primary' },
  8: { level: 4, text: '待派送', type: 'primary' },
  9: { level: 4, text: '派送中', type: 'primary' },
  10: { level: 5, text: '已签收', type: 'success' },
  11: { level: 6, text: '已拒收', type: 'error' },
  12: { level: 7, text: '已取消', type: 'info' },
}

export function orderStatusView(status?: number) {
  if (status == null) return { level: 0, text: '未知', type: 'info' }
  return ORDER_STATUS_MAP[status] || { level: 0, text: '未知', type: 'info' }
}
