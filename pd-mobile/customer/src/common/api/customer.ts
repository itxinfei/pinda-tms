// 客户端接口封装：路径常量化，页面禁止硬编码接口路径
// 修改点：courier / driver 复制本文件后，把 BASE_URL 前缀改为对应端即可。
import {
  BASE_URL,
  AUTH_BASE,
  CAPTCHA_URL,
  LOGIN_URL,
  LOGOUT_URL,
  TENANT_CODE,
} from '../config'
import { get, post, put, del } from '../request'

// ============ 鉴权 ============
// 验证码图片地址（直接给 <image> 使用，免鉴权）
export function captchaSrc(key: string): string {
  return CAPTCHA_URL + '?key=' + key
}
export function loginApi(data: {
  account: string
  password: string
  key: string
  code: string
}) {
  // 企业编码为登录必填项（后端 LoginParamDTO 校验），统一在此注入
  return post(LOGIN_URL, { ...data, tenantCode: TENANT_CODE })
}
export function logoutApi() {
  return post(LOGOUT_URL, {}, { needToken: true })
}

// ============ 首页 ============
export function mailingCount() {
  return get(BASE_URL + '/mailing/count')
}

// ============ 寄件 ============
export function goodsTypeAll() {
  return get(BASE_URL + '/goodsType/all')
}
export function agencyPage(params: any) {
  return get(BASE_URL + '/agency/page', params)
}
export function addressPage(params: any) {
  // 注意：地址簿分页用 pageSize（驼峰）
  return get(BASE_URL + '/address/page', params)
}
export function mailingTotalPrice(data: any) {
  return post(BASE_URL + '/mailing/totalPrice', data)
}
export function createMailing(data: any) {
  return post(BASE_URL + '/mailing', data)
}

// ============ 订单 ============
export function mailingPage(params: any) {
  // 注意：订单列表分页用 pagesize（全小写）
  return get(BASE_URL + '/mailing/page', params)
}
export function mailingDetail(id: string) {
  return get(BASE_URL + '/mailing/detail?id=' + id)
}
export function mailingRoute(id: string) {
  return get(BASE_URL + '/mailing/route?id=' + id)
}
export function cancelMailing(id: string) {
  return put(BASE_URL + '/mailing/cancel/' + id)
}
export function payMailing(id: string) {
  return put(BASE_URL + '/mailing/pay/' + id)
}

// ============ 轨迹（只传 orderId，禁止传 transportTaskId）============
export function orderTrace(orderId: string) {
  return get(BASE_URL + '/orderTrace/trace?orderId=' + orderId)
}

// ============ 地址簿 ============
export function addressList(params: any) {
  return get(BASE_URL + '/address/page', params)
}
export function addressDetail(id: string) {
  return get(BASE_URL + '/address/detail/' + id)
}
export function createAddress(data: any) {
  return post(BASE_URL + '/address', data)
}
export function updateAddress(id: string, data: any) {
  return put(BASE_URL + '/address/' + id, data)
}
export function deleteAddress(id: string) {
  return del(BASE_URL + '/address/' + id)
}

// ============ 行政区划级联（parentId 必填，省级传 0）============
export function areaSimple(parentId: number) {
  return get(BASE_URL + '/common/area/simple?parentId=' + parentId)
}

// ============ 个人中心 ============
export function userProfile() {
  return get(BASE_URL + '/user/profile')
}
