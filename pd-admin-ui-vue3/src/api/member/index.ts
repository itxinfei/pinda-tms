import request from "@/utils/request";
import type { PageResponse } from "@/api/common";

/**
 * 会员管理（USR-01）
 *
 * BFF pd-web-manager 薄转发至 pd-user，网关前缀 /api/manager。
 * 分页结构与其他管理端档案一致：PageResponse（counts）。
 */
const MEMBER_BASE_URL = "/api/manager/user-center/member";

/** 会员状态：1 正常 / 0 禁用（禁用后客户端不可下单） */
export enum MemberStatus {
  /** 禁用 */
  DISABLED = 0,
  /** 正常 */
  ENABLED = 1,
}

/** 实名认证状态：0 未认证 / 1 已认证 / 2 认证未通过 */
export enum IdCardVerifyStatus {
  /** 未认证 */
  UNVERIFIED = 0,
  /** 已认证 */
  VERIFIED = 1,
  /** 认证未通过 */
  REJECTED = 2,
}

/** 会员档案（后端实体 member_info，DTO 中 nickname 映射为 name） */
export interface MemberRecord {
  /** 雪花 ID（字符串消费，避免精度丢失） */
  id?: string;
  /** 手机号（租户内唯一，建档后不可修改） */
  phone: string;
  /** 姓名/昵称 */
  name?: string;
  /** 头像 URL */
  avatar?: string;
  /** 身份证号 */
  idCardNo?: string;
  /** 实名认证状态 */
  idCardNoVerify?: number;
  /** 状态：1 正常 0 禁用 */
  status?: number;
}

/** 会员分页查询条件（GET 参数，空字段不参与过滤） */
export interface MemberPageQuery {
  page: number;
  pageSize: number;
  /** 手机号（后端模糊匹配） */
  phone?: string;
  /** 状态筛选：1 正常 / 0 禁用；不传查全部 */
  status?: number;
}

const MemberAPI = {
  /** 会员分页（按注册时间倒序） */
  page(params: MemberPageQuery) {
    return request<unknown, PageResponse<MemberRecord>>({
      url: `${MEMBER_BASE_URL}/page`,
      method: "get",
      params,
    });
  },

  /** 会员详情 */
  getDetail(id: string) {
    return request<unknown, MemberRecord>({
      url: `${MEMBER_BASE_URL}/detail/${id}`,
      method: "get",
    });
  },

  /** 新增会员（后端默认置为正常状态，手机号重复返 400） */
  save(data: MemberRecord) {
    return request<unknown, void>({
      url: MEMBER_BASE_URL,
      method: "post",
      data,
    });
  },

  /** 修改会员档案（手机号/状态不在此接口变更） */
  update(id: string, data: MemberRecord) {
    return request<unknown, void>({
      url: `${MEMBER_BASE_URL}/${id}`,
      method: "put",
      data,
    });
  },

  /** 逻辑删除会员 */
  remove(id: string) {
    return request<unknown, void>({
      url: `${MEMBER_BASE_URL}/${id}`,
      method: "delete",
    });
  },

  /** 禁用会员（禁用后不可下单，历史订单不受影响） */
  disable(id: string) {
    return request<unknown, void>({
      url: `${MEMBER_BASE_URL}/${id}/disable`,
      method: "put",
    });
  },

  /** 启用会员 */
  enable(id: string) {
    return request<unknown, void>({
      url: `${MEMBER_BASE_URL}/${id}/enable`,
      method: "put",
    });
  },
};

export default MemberAPI;
