import request from "@/utils/request";

/** 授权状态（真伪一律由后端判定，前端只展示） */
export type LicenseStatus =
  | "VALID"
  | "EMERGENCY"
  | "GRACE"
  | "TRIAL"
  | "TRIAL_EXPIRED"
  | "MISMATCH"
  | "EXPIRED";

/** 授权码来源：ENV 环境变量只读 / FILE 页面激活落盘 / NONE 无授权 */
export type LicenseSource = "ENV" | "FILE" | "NONE";

/** 授权状态响应（对齐后端 com.xinfei.auth.license.LicenseInfo） */
export interface LicenseInfo {
  /** 七态状态机 */
  status: LicenseStatus;
  /** 是否处于已激活态（VALID/EMERGENCY/GRACE） */
  activated: boolean;

  /** 授权对象（未授权为 null） */
  licensee?: string | null;
  /** 许可类型 PERSONAL/ENTERPRISE/OEM/EMERGENCY */
  type?: string | null;
  /** 授权实例数（本期仅记录） */
  instances?: number | null;
  /** 签发时间(ms) */
  issuedAt?: number | null;
  /** 到期时间(ms)，空表示永久 */
  expiresAt?: number | null;

  /** 授权码绑定的机器指纹（应急码为 null） */
  boundMachineId?: string | null;
  /** 本机机器指纹（64 位 hex），用于发给供应商签发 */
  fingerprint?: string | null;
  /** 机器指纹分段展示形式 */
  fingerprintMasked?: string | null;
  /** 授权码来源 */
  source?: LicenseSource | null;

  /** 试用剩余天数（TRIAL） */
  trialDaysLeft?: number | null;
  /** 宽限剩余天数（GRACE） */
  graceDaysLeft?: number | null;
  /** 应急剩余天数（EMERGENCY） */
  emergencyDaysLeft?: number | null;

  /** 后端展示文案 */
  message?: string;
}

const LICENSE_BASE_URL = "/api/v1/license";

const LicenseAPI = {
  /** 查询授权状态（登录态） */
  getStatus() {
    return request<unknown, LicenseInfo>({
      url: `${LICENSE_BASE_URL}/status`,
      method: "get",
    });
  },

  /** 激活授权码（仅超管，后端校验签名+机器绑定后落盘） */
  activate(licenseKey: string) {
    return request<unknown, LicenseInfo>({
      url: `${LICENSE_BASE_URL}/activate`,
      method: "post",
      data: { licenseKey },
    });
  },

  /** 清除落盘授权码（仅超管，ENV 托管时后端拒绝） */
  clear() {
    return request<unknown, LicenseInfo>({
      url: LICENSE_BASE_URL,
      method: "delete",
    });
  },
};

export default LicenseAPI;
