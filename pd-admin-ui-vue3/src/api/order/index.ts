import request from "@/utils/request";
import type { PageResponse } from "@/api/common";

const ORDER_BASE_URL = "/api/manager/order-manager/order";

/** 行政区域简要信息 */
export interface AreaSimple {
  id: string;
  name: string;
  lng?: string;
  lat?: string;
}

/**
 * 订单状态（23000 段，口径见数据字典）
 */
export enum OrderStatus {
  /** 待取件 */
  PENDING_PICKUP = 23000,
  /** 已取件 */
  PICKED_UP = 23001,
  /** 网点自寄 */
  SELF_SHIPPED = 23002,
  /** 网点入库 */
  INBOUND = 23003,
  /** 待装车 */
  PENDING_LOADING = 23004,
  /** 运输中 */
  TRANSPORTING = 23005,
  /** 网点出库 */
  OUTBOUND = 23006,
  /** 待派送 */
  PENDING_DELIVERY = 23007,
  /** 派送中 */
  DELIVERING = 23008,
  /** 已签收 */
  SIGNED = 23009,
  /** 拒收 */
  REJECTED = 23010,
  /** 已取消 */
  CANCELLED = 23011,
}

/** 取派件任务简要信息（详情页） */
export interface PickupDispatchTask {
  id: string;
  /** 任务类型：1取件 2派件 */
  taskType: number;
  status: number;
  signStatus?: number;
  assignedStatus?: number;
  courier?: { id?: string; name?: string; phone?: string };
  agency?: { id?: string; name?: string };
  estimatedStartTime?: string;
  actualStartTime?: string;
  estimatedEndTime?: string;
  actualEndTime?: string;
  confirmTime?: string;
  cancelTime?: string;
  mark?: string;
}

/** 运单简要信息（详情页） */
export interface TransportOrderBrief {
  id: string;
  /** 运单状态：1新建 2已装车 3到达 4到达终端网点 */
  status: number;
  schedulingStatus?: number;
  createTime?: string;
}

/** 订单列表项字段 */
export interface OrderRecord {
  id: string;
  /** 订单类型：1同城 2城际 */
  orderType?: number;
  /** 取件类型：1网点自寄 2上门取件 */
  pickupType?: number;
  createTime?: string;
  /** 客户ID */
  memberId?: string;
  receiverProvince?: AreaSimple;
  receiverCity?: AreaSimple;
  receiverCounty?: AreaSimple;
  receiverAddress?: string;
  receiverName?: string;
  receiverPhone?: string;
  senderProvince?: AreaSimple;
  senderCity?: AreaSimple;
  senderCounty?: AreaSimple;
  senderAddress?: string;
  senderName?: string;
  senderPhone?: string;
  /** 付款方式：1预结 2到付 */
  paymentMethod?: number;
  /** 付款状态：1未付 2已付 */
  paymentStatus?: number;
  amount?: number;
  estimatedArrivalTime?: string;
  distance?: number;
  status: number;
}

/** 订单详情：含取件/派件任务与运单 */
export interface OrderDetail extends OrderRecord {
  taskPickup?: PickupDispatchTask;
  taskDispatch?: PickupDispatchTask;
  transportOrder?: TransportOrderBrief;
}

/** 订单分页查询条件（POST body，空字段不参与过滤） */
export interface OrderPageQuery extends Partial<OrderRecord> {
  page: number;
  pageSize: number;
}

const OrderAPI = {
  /** 订单分页（POST body 提交查询条件） */
  page(data: OrderPageQuery) {
    return request<unknown, PageResponse<OrderRecord>>({
      url: `${ORDER_BASE_URL}/page`,
      method: "post",
      data,
    });
  },
  /** 订单详情 */
  getDetail(id: string) {
    return request<unknown, OrderDetail>({
      url: `${ORDER_BASE_URL}/${id}`,
      method: "get",
    });
  },
  /** 更新订单（金额/支付状态后端忽略，走专用端点） */
  update(id: string, data: Partial<OrderDetail>) {
    return request<unknown, OrderDetail>({
      url: `${ORDER_BASE_URL}/${id}`,
      method: "post",
      data,
    });
  },
};

export default OrderAPI;
