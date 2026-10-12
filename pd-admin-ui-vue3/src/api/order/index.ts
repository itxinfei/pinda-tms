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
 * 订单状态（紧凑码 1~12，口径见数据字典）：
 * 1待取件 2已取件 3网点自寄 4网点入库 5待装车 6运输中 7网点出库 8待派送 9派送中 10已签收 11拒收 12已取消
 */
export enum OrderStatus {
  /** 待取件 */
  PENDING_PICKUP = 1,
  /** 已取件 */
  PICKED_UP = 2,
  /** 网点自寄 */
  SELF_SHIPPED = 3,
  /** 网点入库 */
  INBOUND = 4,
  /** 待装车 */
  PENDING_LOADING = 5,
  /** 运输中 */
  TRANSPORTING = 6,
  /** 网点出库 */
  OUTBOUND = 7,
  /** 待派送 */
  PENDING_DELIVERY = 8,
  /** 派送中 */
  DELIVERING = 9,
  /** 已签收 */
  SIGNED = 10,
  /** 拒收 */
  REJECTED = 11,
  /** 已取消 */
  CANCELLED = 12,
}

/** 取派件任务简要信息（详情页） */
export interface PickupDispatchTask {
  id: string;
  /** 任务类型：1取件 2派件 */
  taskType: number;
  status: number;
  signStatus?: number;
  assignedStatus?: number;
  courier?: { id?: string; name?: string; mobile?: string };
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
  /** 运单状态：1新建 2已装车 3到达 4到达终端网点 5已签收 6拒收 */
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
  /** 取消原因（status=12 时有值） */
  cancelReason?: string;
  /** 取消时间（status=12 时有值，yyyy-MM-dd HH:mm:ss） */
  cancelTime?: string;
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
  /** 下单时间范围起点（create_time >= startTime，yyyy-MM-dd HH:mm:ss） */
  startTime?: string;
  /** 下单时间范围终点（create_time <= endTime，yyyy-MM-dd HH:mm:ss） */
  endTime?: string;
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
  /**
   * 取消订单（状态 1~9 可取消）
   * @returns warnings 非空时表示订单已取消，但存在运单/下游任务需人工处理
   */
  cancel(id: string, reason: string) {
    return request<unknown, { warnings: string[] }>({
      url: `${ORDER_BASE_URL}/${id}/cancel`,
      method: "put",
      data: { reason },
    });
  },
};

export default OrderAPI;
