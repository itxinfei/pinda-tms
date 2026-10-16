import request from "@/utils/request";

const ORDER_EXCEPTION_BASE_URL = "/api/manager/order-exception";

/**
 * 异常类型（ORD-04）：1滞留 2破损 3退回 4其他
 */
export enum OrderExceptionType {
  STALLED = 1,
  DAMAGED = 2,
  RETURNED = 3,
  OTHER = 4,
}

/**
 * 处理状态：1待处理 2已处理
 */
export enum HandleStatus {
  PENDING = 1,
  HANDLED = 2,
}

/** 订单异常记录 */
export interface OrderExceptionRecord {
  id: string;
  orderId: string;
  exceptionType: OrderExceptionType;
  description: string;
  handleStatus: HandleStatus;
  handleResult?: string;
  handlerUser?: string;
  handleTime?: string;
  createTime?: string;
}

/** 后端 IPage 分页结构（Result.data） */
export interface IPageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

/** 异常分页查询条件 */
export interface OrderExceptionPageQuery {
  page: number;
  pageSize: number;
  orderId?: string;
  exceptionType?: OrderExceptionType;
  handleStatus?: HandleStatus;
}

const OrderExceptionAPI = {
  /** 登记订单异常（不改订单状态） */
  register(orderId: string, exceptionType: OrderExceptionType, description: string) {
    return request<unknown, unknown>({
      url: `${ORDER_EXCEPTION_BASE_URL}/register`,
      method: "post",
      params: { orderId, exceptionType, description },
    });
  },
  /** 处理异常（待处理→已处理，留痕） */
  handle(id: string, handleResult: string) {
    return request<unknown, unknown>({
      url: `${ORDER_EXCEPTION_BASE_URL}/${id}/handle`,
      method: "post",
      params: { handleResult },
    });
  },
  /** 分页查询 */
  page(params: OrderExceptionPageQuery) {
    return request<unknown, IPageResult<OrderExceptionRecord>>({
      url: `${ORDER_EXCEPTION_BASE_URL}/page`,
      method: "get",
      params,
    });
  },
};

export default OrderExceptionAPI;
