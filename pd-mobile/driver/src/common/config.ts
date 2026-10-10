// 全局配置：网关地址、三端前缀、token 头、角色
// 修改点：三端前缀 / 角色 各不相同，courier、driver 复制本文件后改这两处即可。
// 修改点：GATEWAY 用条件编译（构建期静态处理）。运行时读 process.env.UNI_PLATFORM
// 会被 vite define 漏掉（as any 转型破坏替换匹配），导致 H5 产物直连绝对网关触发 CORS。
// #ifdef H5
const GATEWAY = '/prod-api' // 容器 nginx 将 /prod-api/* 重写为网关 /api/*
// #endif
// #ifndef H5
const GATEWAY = 'http://192.168.20.130:8760/api'
// #endif

// 修改点：司机端前缀改为 /web-driver
export const BASE_URL = GATEWAY + '/web-driver'
// 鉴权前缀（登录 / 验证码 / 登出），三端共用（实测网关为 /auth，非 /authority）
export const AUTH_BASE = GATEWAY + '/auth'
// 轨迹 / 定位前缀（免鉴权）
export const NETTY_BASE = GATEWAY + '/netty-service'

export const TOKEN_KEY = 'pd_token'
export const TOKEN_HEADER = 'token'
// 企业编码（登录必填，后端 LoginParamDTO 校验 @NotBlank；种子租户见 pd_auth_多租户重建_20261009.sql）
export const TENANT_CODE = 'pinda'
// 修改点：司机端角色改为 DRIVER
export const ROLE = 'DRIVER'

export const CAPTCHA_URL = AUTH_BASE + '/anno/captcha'
export const LOGIN_URL = AUTH_BASE + '/anno/login'
export const LOGOUT_URL = AUTH_BASE + '/anno/logout'
export const LOGIN_PAGE = '/pages/driver/login/index'
