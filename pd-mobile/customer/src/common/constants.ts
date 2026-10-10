// 订单状态：客户端 7 档视图（D-48，2026-10-09 拍板），集中映射，禁止页面硬编码数字
// 修改点：courier / driver 复制本文件后替换为本端状态映射（取派任务 / 运输任务枚举不同）。
export type CustomerStatusLevel = 1 | 2 | 3 | 4 | 5 | 6 | 7

export const ORDER_STATUS_MAP: Record<
  number,
  { level: CustomerStatusLevel; text: string; type: string }
> = {
  23000: { level: 1, text: '待取件', type: 'warning' },
  23001: { level: 2, text: '已取件', type: 'primary' },
  23002: { level: 2, text: '网点自寄', type: 'primary' },
  23003: { level: 2, text: '网点入库', type: 'primary' },
  23004: { level: 2, text: '待装车', type: 'primary' },
  23005: { level: 3, text: '运输中', type: 'primary' },
  23006: { level: 3, text: '运输中', type: 'primary' },
  23007: { level: 4, text: '待派送', type: 'primary' },
  23008: { level: 4, text: '派送中', type: 'primary' },
  23009: { level: 5, text: '已签收', type: 'success' },
  23010: { level: 6, text: '已拒收', type: 'error' },
  23011: { level: 7, text: '已取消', type: 'info' },
}

export function orderStatusView(status?: number) {
  if (status == null) return { level: 0, text: '未知', type: 'info' }
  return ORDER_STATUS_MAP[status] || { level: 0, text: '未知', type: 'info' }
}
