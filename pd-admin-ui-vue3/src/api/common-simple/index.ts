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
};

export default CommonSimpleAPI;
