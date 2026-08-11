import axiosApi from './AxiosApi.js'

/**
 * GPS 车辆轨迹查询 API
 * 对接 pd-netty 的 GpsTraceController（/trace 前缀）
 */
const apiList = {
  replay: `/trace/replay`,
  latest: `/trace/latest`,
  page: `/trace/page`
}

export default {
  /**
   * 轨迹回放：按业务ID + 类型查询完整轨迹
   * @param {Object} data { businessId, type }
   */
  replay (data) {
    return axiosApi({
      method: 'GET',
      url: apiList.replay,
      params: data
    })
  },
  /**
   * 最近位置：按业务ID + 类型查询最新一条
   * @param {Object} data { businessId, type }
   */
  latest (data) {
    return axiosApi({
      method: 'GET',
      url: apiList.latest,
      params: data
    })
  },
  /**
   * 轨迹分页查询
   * @param {Object} data { page, pageSize, businessId, type, transportTaskId, licensePlate }
   */
  page (data) {
    return axiosApi({
      method: 'POST',
      url: apiList.page,
      data
    })
  }
}
