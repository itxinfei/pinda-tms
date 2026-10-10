// 登录态 Pinia store：token + 角色 + 用户信息，三端复用
import { defineStore } from 'pinia'
import { TOKEN_KEY, ROLE } from '../config'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: (uni.getStorageSync(TOKEN_KEY) as string) || '',
    userInfo: (uni.getStorageSync('pd_user_info') as any) || null,
    role: ROLE,
  }),
  getters: {
    isLogin: (s) => !!s.token,
  },
  actions: {
    setToken(token: string) {
      this.token = token
      uni.setStorageSync(TOKEN_KEY, token)
    },
    setUserInfo(info: any) {
      this.userInfo = info
      uni.setStorageSync('pd_user_info', info)
    },
    logout() {
      this.token = ''
      this.userInfo = null
      uni.removeStorageSync(TOKEN_KEY)
      uni.removeStorageSync('pd_user_info')
    },
  },
})
