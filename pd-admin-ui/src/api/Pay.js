import axiosApi from './AxiosApi.js'

/**
 * 统一支付 API
 * 对接 pd-oms 的 PayController(/pay 前缀, 经网关 /api/pay)
 */
const apiList = {
  create: `/pay/create`,
  query: `/pay/query`,
  refund: `/pay/refund`
}

export default {
  /**
   * 创建支付
   * @param {String} orderId 订单ID
   */
  createPayment (orderId) {
    return axiosApi({
      method: 'POST',
      url: `${apiList.create}/${orderId}`
    })
  },
  /**
   * 查询支付状态
   * @param {String} orderId 订单ID
   */
  queryPayment (orderId) {
    return axiosApi({
      method: 'GET',
      url: `${apiList.query}/${orderId}`
    })
  },
  /**
   * 退款
   * @param {String} orderId 订单ID
   */
  refund (orderId) {
    return axiosApi({
      method: 'POST',
      url: `${apiList.refund}/${orderId}`
    })
  }
}
