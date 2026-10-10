/* 品达TMS 客户端 · 登录/验证码 API */
import { request, BASE_URL } from '../request/index.js';

/** 验证码图片地址（直接用 img src） */
export const captchaUrl = (key) => `${BASE_URL}/auth/anno/captcha?key=${key}`;

/** 账号+验证码登录（POST /auth/anno/login） */
export const login = (data) => request({ url: '/auth/anno/login', method: 'POST', data });
