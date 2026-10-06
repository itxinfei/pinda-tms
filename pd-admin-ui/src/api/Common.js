import axiosApi from './AxiosApi.js'
import db from '@/utils/localstorage'

const apiList = {
  // 获取当前系统的所有枚举
  // 2026-10-06 修正：原为 '/gate/dictionary/enums'（重构前的遗留旧契约），
  // 拼上 VUE_APP_BASE_API='/api' 后为 /api/gate/dictionary/enums，
  // 但网关无 /gate/** 路由 → 实测 404。
  // 后端真实端点：AuthorityGeneralController.enums()（无类级 @RequestMapping）
  // 实际路径为 /enums，实测 GET /api/enums 返回 200 + 完整枚举 JSON。
  // 详见 docs/需求文档/后端接口文档.md §8.3
  dictionaryEnums: '/enums'
}

export default {
  uploadFile: `${process.env.VUE_APP_DEV_REQUEST_DOMAIN_PREFIX}${process.env.VUE_APP_BASE_API}/file/attachment/upload`,
  dictionaryEnums () {
    return axiosApi({
      method: 'GET',
      url: apiList.dictionaryEnums
    })
  },
  // 生成id
  generateId (data) {
    return axiosApi({
      url: "/authority/common/generateId",
      method: "GET",
      data
    })
  },
  // 查询附件
  getAttachment (data) {
    return axiosApi({
      url: "/file/attachment",
      method: "get",
      data
    })
  },
  // 删除附件
  deleteAttachment (data) {
    return axiosApi({
      url: "/file/attachment",
      method: "delete",
      data
    })
  },
  // 下载附件
  downloadAttachment (data) {
    return axiosApi({
      url: `/file/attachment/download`,
      method: "get",
      responseType: "blob",
      data
    })
  },
  // 根据业务类型/业务id打包下载
  downloadAttachmentBiz (data) {
    return axiosApi({
      url: `/file/attachment/download/biz`,
      method: "get",
      responseType: "blob",
      data
    })
  }
}
