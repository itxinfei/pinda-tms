import axiosApi from './AxiosApi.js'

const apiList = {
  page: {
    method: 'GET',
    url: `/authority/user/page`
  },
  save: {
    method: 'POST',
    url: `/authority/user`
  },
  update: {
    method: 'PUT',
    url: `/authority/user`
  },
  avatar: {
    method: 'PUT',
    url: `/authority/user/avatar`
  },
  delete: {
    method: 'DELETE',
    url: `/authority/user`
  },
  reset: {
    method: 'POST',
    url: `/authority/user/reset`
  },
  updatePassword: {
    method: 'PUT',
    url: `/authority/user/password`
  }
}

export default {
  page (data) {
    return axiosApi({
      ...apiList.page,
      formData: true,
      data
    })
  },
  save (data) {
    return axiosApi({
      ...apiList.save,
      data
    })
  },
  update (data) {
    return axiosApi({
      ...apiList.update,
      data
    })
  },
  updatePassword (data) {
    return axiosApi({
      ...apiList.updatePassword,
      data
    })
  },
  delete (data) {
    return axiosApi({
      ...apiList.delete,
      data
    })
  },
  reset (data) {
    // 后端是 @PostMapping("/reset") + @RequestParam("ids[]")：方法必须是 POST，
    // 参数名要带方括号且走查询串（调用方传的是 { ids: [...] }）
    const ids = Array.isArray(data) ? data : (data && data.ids) || []
    return axiosApi({
      ...apiList.reset,
      params: { 'ids[]': ids }
    })
  },
  avatar (data) {
    return axiosApi({
      ...apiList.avatar,
      data
    })
  }
}
