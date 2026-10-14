import request from "@/utils/request";
import type { PageResponse } from "@/api/common";
import type { AgencySimple, TransportTrips, Truck } from "@/api/transport-task";
import type { FleetSimple } from "@/api/common-simple";
import type { TransportLineRecord } from "@/api/transport-line";

const DRIVER_BASE_URL = "/api/manager/transfor-center/bussiness/driver";
const DRIVER_LICENSE_BASE_URL = "/api/manager/transfor-center/bussiness/driverLicense";

/** 司机基本信息（列表项） */
export interface DriverRecord {
  userId: string;
  name: string;
  workNumber?: string;
  mobile?: string;
  avatar?: string;
  age?: number;
  /** 所属机构 */
  agency?: AgencySimple;
  fleet?: FleetSimple;
  /** 当前使用车辆 */
  truck?: Truck;
  /** 线路 */
  transportLine?: TransportLineRecord;
  truckTransportLine?: TransportLineRecord;
  truckTransportTrip?: TransportTrips;
  workStatus?: string;
}

/** 司机驾驶证信息 */
export interface DriverLicenseRecord {
  userId?: string;
  allowableType?: string;
  /** 初次领证日期 yyyy-MM-dd */
  initialCertificateDate?: string;
  validPeriod?: string;
  licenseNumber?: string;
  driverAge?: number;
  licenseType?: string;
  qualificationCertificate?: string;
  passCertificate?: string;
  picture?: string;
}

/** 司机分页查询条件（GET 参数，空字段不参与过滤） */
export interface DriverPageQuery {
  page: number;
  pageSize: number;
  name?: string;
  username?: string;
  fleetId?: string;
}

const DriverAPI = {
  /** 司机分页 */
  page(params: DriverPageQuery) {
    return request<unknown, PageResponse<DriverRecord>>({
      url: `${DRIVER_BASE_URL}/page`,
      method: "get",
      params,
    });
  },
  /** 司机基本信息详情 */
  getDetail(id: string) {
    return request<unknown, DriverRecord>({
      url: `${DRIVER_BASE_URL}/${id}`,
      method: "get",
    });
  },
  /** 司机驾驶证信息 */
  getLicense(userId: string) {
    return request<unknown, DriverLicenseRecord>({
      url: `${DRIVER_LICENSE_BASE_URL}/${userId}`,
      method: "get",
    });
  },
};

export default DriverAPI;
