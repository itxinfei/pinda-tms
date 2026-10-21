import type { BaseQueryParams, PageResult } from "@/api/common";

/** 在线会话查询参数 */
export interface OnlineQueryParams extends BaseQueryParams {
  /** 关键字：账号/姓名/租户编码 */
  keywords?: string;
}

/** 在线会话对象 */
export interface OnlineItem {
  /** token 掩码（仅展示） */
  tokenMask: string;
  /** 完整 token（用于踢人提交，勿持久化/外传） */
  token: string;
  userId: number;
  account: string;
  name: string;
  tenantCode: string;
  /** 登录（token 签发）时间 */
  loginTime: string;
}

export type { PageResult };
