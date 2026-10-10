<template>
  <view class="page login">
    <view class="login-hero">
      <image class="login-logo" src="/static/logo-full.png" mode="widthFix" />
      <text class="login-sub">快递员工作台 · 取派一体</text>
    </view>
    <view class="card login-card">
      <u-form :model="form" ref="uForm">
        <u-form-item label="账号" borderBottom>
          <u--input v-model="form.account" placeholder="请输入账号" clearable />
        </u-form-item>
        <u-form-item label="密码" borderBottom>
          <u--input v-model="form.password" type="password" placeholder="请输入密码" clearable />
        </u-form-item>
        <u-form-item label="验证码" borderBottom>
          <u--input v-model="form.code" placeholder="请输入验证码" />
          <template #right>
            <image
              :src="captchaUrl"
              class="captcha-img"
              mode="aspectFill"
              @click="refreshCaptcha"
            />
          </template>
        </u-form-item>
      </u-form>
      <u-gap height="20"></u-gap>
      <u-button
        type="primary"
        text="登 录"
        :loading="loading"
        shape="circle"
        size="large"
        @click="onLogin"
      ></u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
// 登录页：复用 courier 端鉴权接口与用户 store，登录成功后跳快递员首页
import { ref, reactive } from 'vue'
import { loginApi, captchaSrc } from '@/common/api/courier'
import { TENANT_CODE } from '@/common/config'
import { useUserStore } from '@/common/store/user'

const form = reactive({ account: '', password: '', code: '' })
const key = ref('')
const loading = ref(false)
const userStore = useUserStore()
const captchaUrl = ref('')

// 刷新验证码：生成唯一 key 拼到验证码地址
function refreshCaptcha() {
  key.value = 'cap_' + Date.now() + '_' + Math.floor(Math.random() * 10000)
  captchaUrl.value = captchaSrc(key.value)
}
refreshCaptcha()

async function onLogin() {
  if (!form.account || !form.password || !form.code) {
    uni.showToast({ title: '请填写账号、密码和验证码', icon: 'none' })
    return
  }
  loading.value = true
  try {
    // 登录返回归一化后的 data：{ token:{token}, user }
    const res = await loginApi({
      account: form.account,
      password: form.password,
      key: key.value,
      code: form.code,
      tenantCode: TENANT_CODE,
    })
    const token = (res && res.token && res.token.token) || (res && res.token)
    if (!token) {
      uni.showToast({ title: '登录失败，请重试', icon: 'none' })
      refreshCaptcha()
      return
    }
    userStore.setToken(token)
    if (res.user) userStore.setUserInfo(res.user)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      // 快递员首页是 tabBar，用 switchTab
      uni.switchTab({ url: '/pages/courier/home/index' })
    }, 600)
  } catch (e) {
    // 登录失败（含验证码错误）刷新验证码
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss">
.login {
  padding: 0;
}
.login-hero {
  height: 560rpx;
  padding: 60rpx 48rpx 120rpx;
  background: url('/static/login-bg.png') no-repeat center / cover;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.login-logo {
  width: 320rpx;
  height: auto;
  filter: drop-shadow(0 6rpx 16rpx rgba(0, 0, 0, 0.18));
}
.login-sub {
  color: #ffffff;
  font-size: var(--f-aux);
  margin-top: var(--s-2);
  text-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.15);
}
.login-card {
  margin: -60rpx var(--s-5) 0;
  border-radius: 28rpx;
  box-shadow: 0 12rpx 40rpx rgba(31, 54, 122, 0.12);
  padding: 40rpx 32rpx 48rpx;
}
.captcha-img {
  width: 180rpx;
  height: 64rpx;
  border-radius: var(--r-sm);
}
</style>
