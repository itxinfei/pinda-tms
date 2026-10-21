import request from "@/utils/request";

const CARGO_BASE_URL = "/api/manager/order-manager/cargo";

/** 货物类型引用（新增/更新时仅传 id） */
export interface GoodsTypeRef {
  id: string;
  name?: string;
}

/** 订单货物项（对应后端 OrderCargoVo 前端态） */
export interface OrderCargoRecord {
  id?: string;
  /** 货物类型 */
  goodsType?: GoodsTypeRef | null;
  /** 货物名称 */
  name?: string;
  /** 货物单位 */
  unit?: string | null;
  /** 货值（元） */
  cargoValue?: number | null;
  /** 货品条码 */
  cargoBarcode?: string | null;
  /** 数量 */
  quantity?: number | null;
  /** 体积（方） */
  volume?: number | null;
  /** 重量（kg） */
  weight?: number | null;
  /** 备注 */
  remark?: string | null;
  /** 总体积（后端回显） */
  totalVolume?: number | null;
  /** 总重量（后端回显） */
  totalWeight?: number | null;
}

const CargoAPI = {
  /** 按订单查询货物列表 */
  list(orderId: string) {
    return request<unknown, OrderCargoRecord[]>({
      url: CARGO_BASE_URL,
      method: "get",
      params: { orderId },
    });
  },
  /** 新增货物（orderId 通过 order.id 关联） */
  save(orderId: string, data: Partial<OrderCargoRecord>) {
    return request<unknown, OrderCargoRecord>({
      url: CARGO_BASE_URL,
      method: "post",
      data: { ...data, order: { id: orderId } },
    });
  },
  /** 更新货物 */
  update(id: string, data: Partial<OrderCargoRecord>) {
    return request<unknown, OrderCargoRecord>({
      url: `${CARGO_BASE_URL}/${id}`,
      method: "put",
      data,
    });
  },
  /** 删除货物 */
  remove(id: string) {
    return request<unknown, unknown>({
      url: `${CARGO_BASE_URL}/${id}`,
      method: "delete",
    });
  },
};

export default CargoAPI;
