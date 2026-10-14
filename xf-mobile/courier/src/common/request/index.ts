// 统一请求封装：JWT 自动附加、401 清 token 跳登录、统一错误提示、返回结构归一化
// 修改点：三端复用，无需改动（token 头字段为 token，非 Authorization）。
import { TOKEN_KEY, TOKEN_HEADER, LOGIN_PAGE } from '../config'

export interface ApiResult<T = any> {
  code: number
  msg: string
  data: T
}

function handle401() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.reLaunch({ url: LOGIN_PAGE })
}

export function request<T = any>(
  url: string,
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' = 'GET',
  data?: any,
  options: { header?: Record<string, string>; needToken?: boolean } = {}
): Promise<T> {
  const needToken = options.needToken !== false
  const header: Record<string, string> = {
    'content-type': 'application/json',
    ...(options.header || {}),
  }
  const token = uni.getStorageSync(TOKEN_KEY)
  if (needToken && token) header[TOKEN_HEADER] = token

  return new Promise<T>((resolve, reject) => {
    uni.request({
      url,
      method,
      data,
      header,
      success: (res) => {
        const status = res.statusCode
        if (status === 200 || status === 201) {
          const body = res.data as ApiResult<T>
          // 归一化：部分接口返回裸 List / 裸 Object（无 code 包裹），直接透传
          if (body && typeof body === 'object' && !Array.isArray(body) && 'code' in body) {
            if (body.code === 401) {
              handle401()
              reject(new Error('登录已过期'))
              return
            }
            if (body.code !== 0 && body.code !== undefined) {
              uni.showToast({ title: body.msg || '请求失败', icon: 'none' })
              reject(new Error(body.msg || '请求失败'))
              return
            }
            resolve(body.data)
            return
          }
          resolve(body as unknown as T)
        } else if (status === 401) {
          handle401()
          reject(new Error('登录已过期'))
        } else {
          uni.showToast({ title: '服务异常(' + status + ')', icon: 'none' })
          reject(new Error('服务异常'))
        }
      },
      fail: () => {
        uni.showToast({ title: '网络连接失败', icon: 'none' })
        reject(new Error('网络连接失败'))
      },
    })
  })
}

export function get<T = any>(url: string, data?: any, options?: any) {
  return request<T>(url, 'GET', data, options)
}
export function post<T = any>(url: string, data?: any, options?: any) {
  return request<T>(url, 'POST', data, options)
}
export function put<T = any>(url: string, data?: any, options?: any) {
  return request<T>(url, 'PUT', data, options)
}
export function del<T = any>(url: string, data?: any, options?: any) {
  return request<T>(url, 'DELETE', data, options)
}
