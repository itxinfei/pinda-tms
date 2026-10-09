import request from "@/utils/request";
import type {
  CaptchaInfo,
  LoginRequest,
  LoginResult,
} from "./types";

const AUTH_BASE_URL = "/api/authority/anno";

const AuthAPI = {
  /**
   * 登录
   */
  async login(data: LoginRequest) {
    const payload = {
      account: data.username,
      password: data.password,
      key: data.captchaId,
      code: data.captchaCode,
    };

    const result = await request<any, any>({
      url: `${AUTH_BASE_URL}/login`,
      method: "post",
      data: payload,
      anonymous: true,
    });

    // 品达返回格式转换：{ user, token: { token, expire }, permissionsList }
    // 转成前端期望格式：{ accessToken, refreshToken, tokenType, expiresIn }
    return {
      accessToken: result.token?.token || "",
      refreshToken: result.token?.token || "", // 品达没有 refreshToken，先用 accessToken 代替
      tokenType: "Bearer",
      expiresIn: result.token?.expire || 7200,
    };
  },

  /**
   * 获取验证码图片
   */
  async getCaptcha() {
    const key = Date.now() + Math.random().toString(36).substring(2);
    
    const response = await request({
      url: `${AUTH_BASE_URL}/captcha?key=${key}`,
      method: "get",
      responseType: "arraybuffer",
      anonymous: true,
    });
    
    // 响应拦截器对 arraybuffer 直接返回整个 response，从 data 里取二进制数据
    const data = (response as any).data || response;
    const base64 = btoa(
      new Uint8Array(data as ArrayBuffer).reduce(
        (data, byte) => data + String.fromCharCode(byte),
        ""
      )
    );
    
    return {
      captchaId: key,
      captchaBase64: `data:image/png;base64,${base64}`,
    } as CaptchaInfo;
  },

  /**
   * 退出登录
   */
  logout() {
    return request({
      url: `${AUTH_BASE_URL}/logout`,
      method: "post",
    });
  },
};

export default AuthAPI;

export * from "./types";
