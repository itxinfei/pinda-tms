import request from "@/utils/request";
import type { PageResponse } from "@/api/common";
import type { AgencySimple } from "@/api/transport-task";
import type { FleetSimple, TruckTypeSimple } from "@/api/common-simple";

const TRUCK_BASE_URL = "/api/manager/transfor-center/bussiness/truck";

/** 车辆信息（列表项） */
export interface TruckRecord {
  id: string;
  brand?: string;
  licensePlate?: string;
  deviceGpsId?: string;
  /** 准载重量（t） */
  allowableLoad?: number;
  /** 准载体积（m³） */
  allowableVolume?: number;
  truckType?: TruckTypeSimple;
  fleet?: FleetSimple;
  /** 所属机构（详情返回） */
  agency?: AgencySimple;
  /** 以下三项仅详情接口返回，且为静态拼装文本 */
  workStatus?: string;
  expireStatus?: string;
  loadStatus?: string;
}

/** 车辆行驶证信息 */
export interface TruckLicenseRecord {
  id?: string;
  engineNumber?: string;
  /** 注册时间 yyyy-MM-dd */
  registrationDate?: string;
  /** 国家强制报废日期 yyyy-MM-dd */
  mandatoryScrap?: string;
  /** 检验有效期 yyyy-MM-dd */
  expirationDate?: string;
  overallQuality?: number;
  allowableWeight?: number;
  outsideDimensions?: string;
  /** 行驶证有效期 yyyy-MM-dd */
  validityPeriod?: string;
  transportCertificateNumber?: string;
  picture?: string;
}

/** 车辆分页查询条件（GET 参数，空字段不参与过滤） */
export interface TruckPageQuery {
  page: number;
  pageSize: number;
  truckTypeId?: string;
  licensePlate?: string;
  fleetId?: string;
}

const TruckAPI = {
  /** 车辆分页 */
  page(params: TruckPageQuery) {
    return request<unknown, PageResponse<TruckRecord>>({
      url: `${TRUCK_BASE_URL}/page`,
      method: "get",
      params,
    });
  },
  /** 车辆详情 */
  getDetail(id: string) {
    return request<unknown, TruckRecord>({
      url: `${TRUCK_BASE_URL}/${id}`,
      method: "get",
    });
  },
  /** 车辆行驶证详情 */
  getLicense(id: string) {
    return request<unknown, TruckLicenseRecord>({
      url: `${TRUCK_BASE_URL}/${id}/license`,
      method: "get",
    });
  },
};

export default TruckAPI;
