import request from "@/utils/request";

/** 授权状态响应 */
export interface LicenseInfo {
  /** VALID 已授权 / TRIAL 试用中 / TRIAL_EXPIRED 试用到期 */
  status: "VALID" | "TRIAL" | "TRIAL_EXPIRED";
  /** 授权对象（未授权为 null） */
  licensee?: string;
  /** 许可类型（未授权为 null） */
  type?: string;
  /** 授权到期时间戳(ms) */
  expiresAt?: number;
  /** 试用剩余天数 */
  trialDaysLeft?: number;
  /** 前端展示文案 */
  message?: string;
}

const LICENSE_BASE_URL = "/api/v1/license";

const LicenseAPI = {
  /**
   * 获取授权状态（匿名，登录前也可调）
   */
  getStatus() {
    return request<unknown, LicenseInfo>({
      url: `${LICENSE_BASE_URL}/status`,
      method: "get",
      anonymous: true,
    });
  },
};

export default LicenseAPI;
