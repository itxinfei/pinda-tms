/* ============================================
   品达TMS 移动端 · 用户态（Pinia）
   规范来源：docs/需求文档/19-客户小程序开发任务卡.md P0-2
   - token 持久化 uni.setStorageSync
   - 401 由网络层统一处理；无 refreshToken，不做静默刷新
   ============================================ */
import { defineStore } from 'pinia';
import { profile as fetchProfile } from '../common/api/index.js';

export const useUserStore = defineStore('user', {
  state: () => ({
    token: uni.getStorageSync('pd_token') || '',
    userInfo: uni.getStorageSync('userInfo') || null,
    profile: null,
  }),
  getters: {
    isLogin: (state) => !!state.token,
  },
  actions: {
    setToken(token) {
      this.token = token || '';
      if (token) {
        uni.setStorageSync('pd_token', token);
      } else {
        uni.removeStorageSync('pd_token');
      }
    },
    setUserInfo(info) {
      this.userInfo = info || null;
      if (info) {
        uni.setStorageSync('userInfo', info);
      } else {
        uni.removeStorageSync('userInfo');
      }
    },
    /** 拉取客户资料 GET /user/profile */
    async fetchProfile() {
      const data = await fetchProfile();
      this.profile = data;
      return data;
    },
    logout() {
      this.token = '';
      this.userInfo = null;
      this.profile = null;
      uni.removeStorageSync('pd_token');
      uni.removeStorageSync('userInfo');
      uni.reLaunch({ url: '/pages/customer/login/index' });
    },
  },
});
