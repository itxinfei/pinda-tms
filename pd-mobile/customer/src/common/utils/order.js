/* 心飞TMS 客户端 · 订单状态/枚举映射（禁硬编码数字）
   状态码 =《07》§一 紧凑口径 1~12（旧 23xxx 已全线作废，勿回填）
   状态色映射（UI规范 §2.1）：待处理=warning / 进行中=primary / 已完成=success / 异常=error */

export const ORDER_STATUS_MAP = {
  1: { label: '待取件', type: 'warning' },
  2: { label: '已取件', type: 'primary' },
  3: { label: '网点自寄', type: 'primary' },
  4: { label: '网点入库', type: 'primary' },
  5: { label: '待装车', type: 'primary' },
  6: { label: '运输中', type: 'primary' },
  7: { label: '网点出库', type: 'primary' },
  8: { label: '待派送', type: 'primary' },
  9: { label: '派送中', type: 'primary' },
  10: { label: '已签收', type: 'success' },
  11: { label: '拒收', type: 'error' },
  12: { label: '已取消', type: 'info' },
};

export const statusInfo = (status) => ORDER_STATUS_MAP[status] || { label: '未知', type: 'info' };

/** 付款方式 1预结 2到付 */
export const payMethodLabel = (m) => (m === 1 ? '预结' : m === 2 ? '到付' : '-');
/** 付款状态 1未付 2已付 */
export const payStatusLabel = (s) => (s === 1 ? '未付' : s === 2 ? '已付' : '-');
/** 取件方式 1网点自寄 2上门取件 */
export const pickupTypeLabel = (t) => (t === 1 ? '网点自寄' : t === 2 ? '上门取件' : '-');
/** 订单类型 1同城 2城际 */
export const orderTypeLabel = (t) => (t === 1 ? '同城' : t === 2 ? '城际' : '-');

/** 格式化 LocalDateTime 字符串为 yyyy-MM-dd HH:mm */
export const fmtTime = (t) => {
  if (!t) return '-';
  const s = String(t).replace('T', ' ');
  return s.length > 16 ? s.slice(0, 16) : s;
};
