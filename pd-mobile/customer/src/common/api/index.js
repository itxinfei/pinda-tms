/* 品达TMS 客户端 · 网点/物品类型/区域/用户/轨迹 API */
import { request } from '../request/index.js';

/** 网点分页 GET /agency/page?page=&pagesize=&keyword=&cityId= */
export const agencyPage = (params) => request({ url: '/web-customer/agency/page', data: params });

/** 物品类型全量 GET /goodsType/all */
export const goodsTypeAll = () => request({ url: '/web-customer/goodsType/all' });

/** 省市区级联 GET /common/area/simple?parentId=（省=0，必填） */
export const areaSimple = (parentId) => request({ url: '/web-customer/common/area/simple', data: { parentId } });

/** 我的资料 GET /user/profile */
export const profile = () => request({ url: '/web-customer/user/profile' });

/** 按订单查轨迹 GET /orderTrace/trace?orderId= */
export const trace = (orderId) => request({ url: '/web-customer/orderTrace/trace', data: { orderId } });
