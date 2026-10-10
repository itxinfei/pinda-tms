/* ============================================
   品达TMS 移动端 · 网络层统一封装
   规范来源：docs/需求文档/19-客户小程序开发任务卡.md P0-1
   - 统一走网关（H5: /prod-api 经容器 nginx 反代；小程序/App 后续换完整域名）
   - 请求头 token（非 Authorization）
   - code===0 成功；401 清 token 跳登录（无 refreshToken，禁止静默刷新）
   ============================================ */

const BASE_URL = (import.meta.env && import.meta.env.VITE_API_BASE) || '/prod-api';

/**
 * 统一请求
 * @param {Object} options { url, method, data, header, loading }
 * @returns {Promise<any>} 成功返回 res.data（业务数据）
 */
export function request(options) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token') || '';
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      timeout: 15000,
      header: {
        'token': token,
        'Content-Type': 'application/json',
        ...(options.header || {})
      },
      success: (res) => {
        const body = res.data;
        // 401：清登录态跳登录
        if (res.statusCode === 401 || (body && body.code === 401)) {
          uni.removeStorageSync('token');
          uni.removeStorageSync('userInfo');
          uni.reLaunch({ url: '/pages/customer/login/index' });
          reject(body || { code: 401, msg: '登录已过期' });
          return;
        }
        if (body && body.code === 0) {
          resolve(body.data !== undefined ? body.data : body);
        } else {
          const msg = (body && body.msg) || '请求失败，请稍后重试';
          uni.showToast({ title: msg, icon: 'none', duration: 2500 });
          reject(body || { code: -1, msg });
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常，请重试', icon: 'none' });
        reject(err);
      }
    });
  });
}

export { BASE_URL };
