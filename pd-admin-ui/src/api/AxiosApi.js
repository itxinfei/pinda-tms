import axios from 'axios'
import { Message, MessageBox } from 'element-ui'
import db from '@/utils/localstorage'
// 请求添加条件，如token
axios.interceptors.request.use(
  config => {
    config.headers.token = db.get('TOKEN', '')
    config.headers.tenant = db.get('TENANT', '')
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 接口返回处理
axios.interceptors.response.use(
  response => {
    return response
  },
  error => {
    return Promise.reject(error)
  }
)

function handleError (error, reject) {
  if (error.code === 'ECONNABORTED') {
    Message({
      message: '请求超时'
    })
  } else if (error.response && error.response.data) {
    Message({
      message: error.response.data
    })
  } else if (error.message) {
    Message({
      message: error.message
    })
  }
  reject(error)
}

function isBizError (data) {
  if (!data || typeof data !== 'object') {
    return false
  }
  // 鉴权/网关侧返回的是 R：带 isError / isSuccess 两个布尔
  if (typeof data.isError === 'boolean') {
    return data.isError
  }
  // 业务服务返回的是 pd-common 的 Result：HashMap，只有 code/msg，成功码 0（没有 isError，
  // 之前只判 isError 会把 500/400 当成功，页面表现为"数据为空"而不是报错）
  return typeof data.code === 'number' && data.code !== 0 && data.code !== 200
}

function handleSuccess (res, resolve) {
  if (isBizError(res.data)) {
    // 未登录
    if (res.data.code === 40001) {
      MessageBox.alert(res.data.msg, '提醒', {
        confirmButtonText: '确定',
        callback: () => {
          window.location.hash = '/login'
        }
      })
    } else {
      Message.error(res.data.msg)
    }
  }
  resolve(res)
}

// http请求
const httpServer = (opts) => {
  // 公共参数
  const publicParams = {
    ts: Date.now()
  }

  // http默认配置
  const method = opts.method.toUpperCase()
  // baseURL
  // 开发环境： /api                 // 开发环境在 vue.config.js 中有 devServer.proxy 代理
  // 生产环境： http://IP:PORT/api   // 生产环境中 代理失效， 故需要配置绝对路径
  const httpDefaultOpts = {
    method,
    baseURL: process.env.VUE_APP_PROD_REQUEST_DOMAIN_PREFIX + process.env.VUE_APP_BASE_API,
    url: opts.url,
    responseType: opts.responseType || '',
    timeout: 20000
  }

  const dataRequest = ['PUT', 'POST', 'PATCH']
  if (dataRequest.includes(method)) {
    httpDefaultOpts.data = opts.data || {}
  } else {
    httpDefaultOpts.params = {
      ...publicParams,
      ...(opts.data || {})
    }
  }

  // 后端用 @RequestParam 收的参数（含 ids[] 这类数组）必须走查询串；
  // POST/PUT 时 opts.data 会进 JSON body，绑不上，所以单独给一条 params 通道
  if (opts.params) {
    httpDefaultOpts.params = {
      ...publicParams,
      ...opts.params
    }
  }

  // formData转换
  if (opts.formData) {
    httpDefaultOpts.transformRequest = [data => {
      const formData = new FormData()
      if (data) {
        Object.entries(data).forEach(item => {
          formData.append(item[0], item[1])
        })
      }
      return formData
    }]
  }

  const promise = new Promise((resolve, reject) => {
    axios(httpDefaultOpts).then(response => {
      handleSuccess(response, resolve)
    }).catch(error => {
      handleError(error, reject)
    })
  })
  return promise
}

export default httpServer
