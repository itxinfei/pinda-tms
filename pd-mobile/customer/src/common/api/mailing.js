/* 品达TMS 客户端 · 寄件/运单 API（/web-customer/mailing/**） */
import { request } from '../request/index.js';

/** 运费试算 POST /mailing/totalPrice → { amount: "800.00" } */
export const totalPrice = (data) => request({ url: '/web-customer/mailing/totalPrice', method: 'POST', data });

/** 创建寄件 POST /mailing */
export const createOrder = (data) => request({ url: '/web-customer/mailing', method: 'POST', data });

/** 改单 PUT /mailing/{id} */
export const updateOrder = (id, data) => request({ url: `/web-customer/mailing/${id}`, method: 'PUT', data });

/** 支付 PUT /mailing/pay/{id} */
export const pay = (id) => request({ url: `/web-customer/mailing/pay/${id}`, method: 'PUT' });

/** 取消 PUT /mailing/cancel/{id} */
export const cancel = (id) => request({ url: `/web-customer/mailing/cancel/${id}`, method: 'PUT' });

/** 分页 GET /mailing/page 参数 page/pagesize/keyword/mailType(0我寄的1我收的) */
export const page = (params) => request({ url: '/web-customer/mailing/page', data: params });

/** 详情 GET /mailing/detail?id= */
export const detail = (id) => request({ url: '/web-customer/mailing/detail', data: { id } });

/** 时间轴 GET /mailing/route?id= */
export const route = (id) => request({ url: '/web-customer/mailing/route', data: { id } });

/** 计数 GET /mailing/count */
export const count = () => request({ url: '/web-customer/mailing/count' });
