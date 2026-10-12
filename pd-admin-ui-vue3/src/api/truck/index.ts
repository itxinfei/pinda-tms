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
  /** 准载重量（后端落库 kg；列表/详情/表单统一按吨展示，÷1000 回显、×1000 提交） */
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

/** 车辆保存入参：车型/车队为选填嵌套对象，清空时传 null（pd-base 实体配 FieldStrategy.ALWAYS 落 null） */
export interface TruckSavePayload {
  id?: string;
  /** 品牌（选填） */
  brand?: string | null;
  /** 车牌（必填） */
  licensePlate: string;
  /** GPS 设备号（选填） */
  deviceGpsId?: string | null;
  /** 准载重量（kg，表单吨 ×1000；选填清空传 null） */
  allowableLoad?: number | null;
  /** 准载体积（m³，选填清空传 null） */
  allowableVolume?: number | null;
  truckType?: { id: string } | null;
  fleet?: { id: string } | null;
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
  /** 新增车辆 */
  save(data: TruckSavePayload) {
    return request<unknown, TruckRecord>({
      url: TRUCK_BASE_URL,
      method: "post",
      data,
    });
  },
  /** 更新车辆 */
  update(id: string, data: TruckSavePayload) {
    return request<unknown, TruckRecord>({
      url: `${TRUCK_BASE_URL}/${id}`,
      method: "put",
      data,
    });
  },
  /** 删除车辆（BFF 实为软停用；存在未完成运输任务时后端 400 拒绝） */
  remove(id: string) {
    return request<unknown, unknown>({
      url: `${TRUCK_BASE_URL}/${id}`,
      method: "delete",
    });
  },
};

export default TruckAPI;
