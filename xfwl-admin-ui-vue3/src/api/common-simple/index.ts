import request from "@/utils/request";

const COMMON_BASE_URL = "/api/manager/common";

/** 车队简要信息（下拉用） */
export interface FleetSimple {
  id: string;
  name: string;
  fleetNumber?: string;
}

/** 车辆类型简要信息（下拉用） */
export interface TruckTypeSimple {
  id: string;
  name: string;
  allowableLoad?: number;
  allowableVolume?: number;
}

/** 线路类型简要信息（下拉用） */
export interface TransportLineTypeSimple {
  id: string;
  name: string;
  typeNumber?: string;
}

/** 货物类型简要信息（下拉用） */
export interface GoodsTypeSimple {
  id: string;
  name: string;
  defaultWeight?: number;
  defaultVolume?: number;
  status?: number;
}

/** 系统用户简要信息（负责人下拉用） */
export interface SysUserSimple {
  userId: string;
  username?: string;
  name?: string;
  workNumber?: string;
  mobile?: string;
}

const CommonSimpleAPI = {
  /** 车队下拉列表 */
  fleetOptions() {
    return request<unknown, FleetSimple[]>({
      url: `${COMMON_BASE_URL}/fleet/simple`,
      method: "get",
    });
  },
  /** 车辆类型下拉列表 */
  truckTypeOptions() {
    return request<unknown, TruckTypeSimple[]>({
      url: `${COMMON_BASE_URL}/truckType/simple`,
      method: "get",
    });
  },
  /** 线路类型下拉列表 */
  transportLineTypeOptions() {
    return request<unknown, TransportLineTypeSimple[]>({
      url: `${COMMON_BASE_URL}/transportLineType/simple`,
      method: "get",
    });
  },
  /** 货物类型下拉列表（后端 findAll 未过滤停用，前端统一只取启用项） */
  goodsTypeOptions() {
    return request<unknown, GoodsTypeSimple[]>({
      url: `${COMMON_BASE_URL}/goodsType/simple`,
      method: "get",
    });
  },
  /** 系统用户下拉（车队负责人选择；name 传空时返回全部，输入姓名走后端模糊查询） */
  userOptions(name?: string) {
    return request<unknown, SysUserSimple[]>({
      url: `${COMMON_BASE_URL}/user/simple`,
      method: "get",
      params: name ? { name } : undefined,
    });
  },
};

export default CommonSimpleAPI;
