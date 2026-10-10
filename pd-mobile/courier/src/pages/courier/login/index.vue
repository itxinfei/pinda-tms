<template>
  <view class="page login-page">
    <!-- 品牌区（联云式顶部渐变头，神领深藏青配色） -->
    <view class="login-hero">
      <image class="login-logo" src="/static/logo-full.png" mode="widthFix" />
      <view class="login-brand">品达物流</view>
      <view class="login-slogan">快递员工作台 · 取派一体</view>
      <image class="hero-deco" src="/static/login-bg.png" mode="widthFix" />
    </view>

    <!-- 登录卡片 -->
    <view class="login-card">
      <view class="login-title">欢迎回来</view>
      <view class="login-sub">请登录账号，开启高效配送服务</view>

      <view class="field">
        <u--input v-model="form.account" placeholder="请输入账号" clearable prefixIcon="account" />
      </view>
      <view class="field">
        <u--input v-model="form.password" placeholder="请输入密码" type="password" clearable prefixIcon="lock" />
      </view>
      <view class="field field-captcha">
        <u--input v-model="form.code" placeholder="输入验证码" class="captcha-input" />
        <image class="captcha-img" :src="captchaUrl" mode="aspectFill" @click="refreshCaptcha" />
      </view>

      <view class="login-btn" :class="{ 'is-loading': loading }" @click="onLogin">
        {{ loading ? '登录中...' : '登 录' }}
      </view>
      <view class="login-tip">账号登录 · 微信免密登录开发中</view>
    </view>

    <view class="login-footer">品达物流 TMS · 快递员端</view>
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
.login-page {
  min-height: 100vh;
  padding: 0;
  background: #f5f6f8;
  position: relative;
}
.login-hero {
  padding: calc(var(--s-8) * 3) var(--s-6) 150rpx;
  background: linear-gradient(160deg, #1a1c41 0%, #283443 60%, #2d3a4b 100%);
  border-radius: 0 0 48rpx 48rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  position: relative;
  overflow: hidden;
}
.login-logo {
  width: 240rpx;
  height: auto;
  filter: drop-shadow(0 8rpx 20rpx rgba(0, 0, 0, 0.35));
}
.login-brand {
  margin-top: 24rpx;
  color: #ffffff;
  font-size: 44rpx;
  font-weight: 700;
  letter-spacing: 6rpx;
}
.login-slogan {
  margin-top: 12rpx;
  color: rgba(255, 255, 255, 0.7);
  font-size: var(--f-aux);
  letter-spacing: 2rpx;
}
.hero-deco {
  position: absolute;
  left: 0;
  right: 0;
  bottom: -10rpx;
  width: 100%;
  opacity: 0.5;
  pointer-events: none;
}
.login-card {
  margin: -80rpx var(--s-6) 0;
  background: #ffffff;
  border-radius: 32rpx;
  padding: 48rpx 40rpx;
  box-shadow: 0 16rpx 48rpx rgba(26, 28, 65, 0.16);
  position: relative;
  z-index: 2;
}
.login-title {
  font-size: 40rpx;
  font-weight: 700;
  color: var(--c-text-1);
}
.login-sub {
  margin-top: 8rpx;
  font-size: var(--f-aux);
  color: var(--c-text-3);
}
.field {
  margin-top: 32rpx;
  background: #f5f6f8;
  border-radius: 20rpx;
  padding: 8rpx 28rpx;
}
.field-captcha {
  display: flex;
  align-items: center;
  padding-right: 12rpx;
}
.captcha-input {
  flex: 1;
}
.captcha-img {
  width: 200rpx;
  height: 72rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
}
.login-btn {
  margin-top: 48rpx;
  height: 96rpx;
  line-height: 96rpx;
  border-radius: 48rpx;
  background: linear-gradient(90deg, #e15536 0%, #f1784f 100%);
  color: #ffffff;
  font-size: 34rpx;
  font-weight: 600;
  text-align: center;
  letter-spacing: 4rpx;
  box-shadow: 0 12rpx 28rpx rgba(225, 85, 54, 0.35);
}
.login-btn.is-loading {
  opacity: 0.7;
}
.login-tip {
  margin-top: 24rpx;
  text-align: center;
  color: var(--c-text-3);
  font-size: var(--f-tip);
}
.login-footer {
  margin-top: 48rpx;
  text-align: center;
  color: var(--c-text-4);
  font-size: var(--f-tip);
}
</style>
