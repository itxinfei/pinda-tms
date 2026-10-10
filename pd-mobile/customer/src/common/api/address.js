/* 品达TMS 客户端 · 地址簿 API（/web-customer/address/**）
   注意：本模块分页参数是 pageSize（驼峰大写S），其余接口是 pagesize（全小写） */
import { request } from '../request/index.js';

/** 分页 GET /address/page?page=&pageSize=&keyword= */
export const page = (params) => request({ url: '/web-customer/address/page', data: params });

/** 新增 POST /address */
export const save = (data) => request({ url: '/web-customer/address', method: 'POST', data });

/** 更新 PUT /address/{id} */
export const update = (id, data) => request({ url: `/web-customer/address/${id}`, method: 'PUT', data });

/** 删除 DELETE /address/{id} */
export const remove = (id) => request({ url: `/web-customer/address/${id}`, method: 'DELETE' });

/** 详情 GET /address/detail/{id} */
export const detail = (id) => request({ url: `/web-customer/address/detail/${id}` });
