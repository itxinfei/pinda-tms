// 快递员端接口封装：路径常量化，页面禁止硬编码接口路径
// 修改点：driver 复制本文件后把 BASE_URL 前缀改为 /web-driver 并替换接口。
import {
  BASE_URL,
  NETTY_BASE,
  AUTH_BASE,
  CAPTCHA_URL,
  LOGIN_URL,
  TOKEN_KEY,
  TOKEN_HEADER,
  TENANT_CODE,
} from '../config'
import { get, post, put } from '../request'
// 签收二态 / 异常类型常量（定义在 constants.ts，便于页面从本模块统一引入）
export { DELIVERED_SIGN, DELIVERED_REJECT, COURIER_EXCEPTION_TYPES } from '../constants'

// ============ 鉴权（三端共用 pd-auth）============
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

// ============ 首页 / 个人 ============
// 各状态计数（query: taskType,status,keyword,date）
export function courierCount(params: any) {
  return get(BASE_URL + '/courier/count', params)
}
export function userProfile() {
  return get(BASE_URL + '/user/profile')
}

// ============ 取派任务 ============
// 任务分页：pagesize 全小写；返回 data.{items,counts}
export function pickupDispatchPage(params: any) {
  return get(BASE_URL + '/courier/pickupDispatch', params)
}
export function taskDetail(id: string) {
  return get(BASE_URL + '/courier/detail', { id })
}
export function taskRoute(id: string) {
  return get(BASE_URL + '/courier/route', { id })
}
// 运费试算（courier 版 MailingSaveDTO：必填 orderNumber，无地址字段）
export function totalPrice(data: any) {
  return post(BASE_URL + '/courier/totalPrice', data)
}

// ============ 附件上传（multipart，业务标识 businessType=courier）============
// 返回 Object（附件 id/url），照片/签名禁 base64 直塞业务请求体
export function attachmentUpload(
  filePath: string,
  businessType = 'courier'
): Promise<any> {
  const token = uni.getStorageSync(TOKEN_KEY)
  const header: Record<string, string> = {}
  if (token) header[TOKEN_HEADER] = token
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/attachment/upload',
      filePath,
      name: 'file',
      formData: { businessType },
      header,
      success: (res) => {
        try {
          const body = JSON.parse(res.data)
          if (body.code === 0 || body.code === undefined) resolve(body.data)
          else {
            uni.showToast({ title: body.msg || '上传失败', icon: 'none' })
            reject(new Error(body.msg || '上传失败'))
          }
        } catch (e) {
          reject(new Error('上传响应解析失败'))
        }
      },
      fail: () => {
        uni.showToast({ title: '上传失败', icon: 'none' })
        reject(new Error('上传失败'))
      },
    })
  })
}

// ============ 派送签收 / 拒收（仅 "1" 签收 / "0" 拒收，不收 location）============
export function delivered(tranOrderId: string, status: string) {
  return put(BASE_URL + '/courier/delivered/' + tranOrderId + '/' + status)
}
// 入库
export function warehousing(tranOrderId: string) {
  return put(BASE_URL + '/courier/warehousing/' + tranOrderId)
}
// 交接
export function handover(tranOrderId: string) {
  return put(BASE_URL + '/courier/handover/' + tranOrderId)
}

// ============ 身份证核验（POST 非 GET，form/query：orderNumber,code）============
export function verifyIdCard(params: { orderNumber: string; code: string }) {
  return post(BASE_URL + '/courier/verifyIdCard', params)
}

// ============ 轨迹（免鉴权 /netty-service，只认 businessId+type，无 orderId）============
export function traceLatest(businessId: string, type = 'courier') {
  return get(NETTY_BASE + '/trace/latest', { businessId, type })
}
export function traceReplay(businessId: string, type = 'courier') {
  return get(NETTY_BASE + '/trace/replay', { businessId, type })
}

// ============ 异常上报（三要素：exceptionType + attachmentIds(>=1) + remark）============
export function exceptionReport(data: any) {
  return post(BASE_URL + '/courier/exception/report', data)
}

// ============ 基础数据 ============
export function areaSimple(parentId: number) {
  return get(BASE_URL + '/common/area/simple', { parentId })
}
export function goodsTypeAll() {
  return get(BASE_URL + '/goodsType/all')
}
