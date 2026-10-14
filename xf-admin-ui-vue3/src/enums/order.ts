// 订单域枚举
// 码值口径：《07-数据字典与枚举口径》§2，与后端 enums/Order*.java 一致

/**
 * 订单类型
 */
export enum OrderType {
  /** 同城订单 */
  INCITY = 1,
  /** 城际订单 */
  OUTCITY = 2,
}

/**
 * 支付方式
 */
export enum OrderPaymentMethod {
  /** 预结（寄付） */
  PRE_PAY = 1,
  /** 到付 */
  END_PAY = 2,
}

/**
 * 支付状态
 */
export enum OrderPaymentStatus {
  /** 未支付 */
  UNPAID = 1,
  /** 已支付 */
  PAID = 2,
  /** 已退款 */
  REFUNDED = 3,
}

/**
 * 取件方式
 */
export enum OrderPickupType {
  /** 网点自寄 */
  NO_PICKUP = 1,
  /** 上门取件 */
  PICKUP = 2,
}

/** 订单类型中文 */
export const ORDER_TYPE_LABELS: Record<OrderType, string> = {
  [OrderType.INCITY]: "同城",
  [OrderType.OUTCITY]: "城际",
};

/** 支付方式中文 */
export const ORDER_PAYMENT_METHOD_LABELS: Record<OrderPaymentMethod, string> = {
  [OrderPaymentMethod.PRE_PAY]: "预结",
  [OrderPaymentMethod.END_PAY]: "到付",
};

/** 支付状态中文（含已退款） */
export const ORDER_PAYMENT_STATUS_LABELS: Record<OrderPaymentStatus, string> = {
  [OrderPaymentStatus.UNPAID]: "未付",
  [OrderPaymentStatus.PAID]: "已付",
  [OrderPaymentStatus.REFUNDED]: "已退款",
};

/** 取件方式中文 */
export const ORDER_PICKUP_TYPE_LABELS: Record<OrderPickupType, string> = {
  [OrderPickupType.NO_PICKUP]: "网点自寄",
  [OrderPickupType.PICKUP]: "上门取件",
};
