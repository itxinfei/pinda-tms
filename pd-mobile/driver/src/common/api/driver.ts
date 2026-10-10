// 司机端接口封装：路径常量化，页面禁止硬编码接口路径
// 修改点：由 courier.ts 镜像改写为本端（/web-driver）业务接口。
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
import { enqueueOffline } from '../utils/offline'

// ============ 鉴权（三端共用 pd-auth-server）============
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

// ============ 货物 / 司机作业单 ============
// 待提货分页：pagesize 全小写；返回 data.{items,counts}
export function cargoWait(params: any) {
  return get(BASE_URL + '/business/cargo/wait', params)
}
// 在途（单对象 CargoTranTaskDTO，非列表）
export function cargoOnTheWay() {
  return get(BASE_URL + '/business/cargo/onTheWay')
}
// 历史分页
export function cargoHistory(params: any) {
  return get(BASE_URL + '/business/cargo/history', params)
}
// 司机作业单详情（id 是 DriverJobId）
export function cargoDetail(id: string) {
  return get(BASE_URL + '/business/cargo/detail?id=' + id)
}
// 关联订单列表（id 是 DriverJobId）
export function cargoOrders(id: string, keyword?: string) {
  let url = BASE_URL + '/business/cargo/orders?id=' + id
  if (keyword) url += '&keyword=' + keyword
  return get(url)
}

// ============ 附件上传（multipart，业务标识 businessType=driver）============
// 返回 Object（附件 id/url），照片禁 base64 直塞业务请求体
export function attachmentUpload(
  filePath: string,
  businessType = 'driver'
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

// ============ 提货 / 送达（不传 status，经纬度由定位取得）============
// 提货：body 仅含 id（DriverJobId）+ 提货照片
export function cargoPickUp(data: { id: string; cargoPickUpPicture?: string }) {
  return put(BASE_URL + '/business/cargo/pickUp', data)
}
// 送达：body 含 id + 经纬度 + 签收照片
export function cargoFinish(data: {
  id: string
  deliverLatitude: number
  deliverLongitude: number
  deliverPicture?: string
}) {
  return put(BASE_URL + '/business/cargo/finish', data)
}

// ============ 车辆 / 用户 ============
// 车辆信息（id 是车辆 id）
export function carInfo(id: string) {
  return get(BASE_URL + '/business/car/info?id=' + id)
}
// 个人资料
export function userProfile() {
  return get(BASE_URL + '/user/profile')
}

// ============ 异常上报（三要素：exceptionType + attachmentIds(>=1) + remark）============
// reporterId 由 token 取，前端不传；transportTaskId 可选
export function exceptionReport(data: {
  exceptionType: string
  attachmentIds: string[]
  remark: string
  transportTaskId?: string
}) {
  return post(BASE_URL + '/business/cargo/exception/report', data)
}

// ============ 轨迹（免鉴权 /netty-service，只认 businessId+type，无 orderId）============
export function traceLatest(businessId: string, type = 'truck') {
  return get(NETTY_BASE + '/trace/latest?businessId=' + businessId + '&type=' + type)
}
export function traceReplay(businessId: string, type = 'truck') {
  return get(NETTY_BASE + '/trace/replay?businessId=' + businessId + '&type=' + type)
}

// ============ 后台定位上报（免鉴权，无网入离线队列）============
// body：businessId,name,phone,licensePlate,type,lng,lat,currentTime,team,transportTaskId,coordSystem,source
// source 统一 'MOBILE'，coordSystem 统一 'WGS84'
export function locationPush(data: any) {
  const token = uni.getStorageSync(TOKEN_KEY)
  const header: Record<string, string> = { 'content-type': 'application/json' }
  if (token) header[TOKEN_HEADER] = token
  return new Promise<any>((resolve, reject) => {
    uni.request({
      url: NETTY_BASE + '/netty/push',
      method: 'POST',
      data,
      header,
      // 定位上报失败静默入离线队列，避免频繁弹窗
      success: (res: any) => resolve(res.data),
      fail: () => {
        enqueueOffline({
          id: 'loc_' + Date.now() + '_' + Math.floor(Math.random() * 10000),
          url: NETTY_BASE + '/netty/push',
          method: 'POST',
          data,
        })
        reject(new Error('定位上报失败，已加入离线队列'))
      },
    })
  })
}

// ============ 基础数据 ============
export function areaSimple(parentId: number) {
  return get(BASE_URL + '/common/area/simple?parentId=' + parentId)
}
