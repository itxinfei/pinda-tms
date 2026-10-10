<template>
  <view class="page login-page">
    <!-- 品牌区：极简白底，仅 logo -->
    <view class="login-brand">
      <image class="login-logo" src="/static/logo-full.png" mode="widthFix" />
    </view>

    <!-- 表单区：白底描边输入框 + 橙色实心按钮 -->
    <view class="login-form">
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
      <view class="login-agree">登录即代表同意《用户协议》和《隐私政策》</view>
    </view>

    <view class="login-footer">品达物流 TMS · 司机端</view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { loginApi, captchaSrc } from '@/common/api/driver'
import { useUserStore } from '@/common/store/user'
import { TENANT_CODE } from '@/common/config'

const form = reactive({ account: '', password: '', code: '' })
const key = ref('')
const loading = ref(false)
const userStore = useUserStore()
const captchaUrl = ref('')

// 刷新验证码（每次生成独立 key 防止缓存）
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
    const res = await loginApi({
      account: form.account,
      password: form.password,
      key: key.value,
      code: form.code,
      tenantCode: TENANT_CODE,
    })
    // JWT 在 res.token（后端返回字符串；兼容历史两层写法）
    const token = (res && res.token && res.token.token) || (res && res.token)
    if (!token) {
      uni.showToast({ title: '登录失败，请重试', icon: 'none' })
      refreshCaptcha()
      return
    }
    userStore.setToken(token)
    if (res.user) userStore.setUserInfo(res.user)
    uni.showToast({ title: '登录成功', icon: 'success' })
    // 修改点：司机端登录成功跳首页（tabBar）
    setTimeout(() => {
      uni.switchTab({ url: '/pages/driver/home/index' })
    }, 600)
  } catch (e) {
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss">
.login-page {
  min-height: 100vh;
  padding: 0 var(--s-6);
  background: #ffffff;
}
.login-brand {
  padding-top: 170rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.login-logo {
  width: 200rpx;
  height: auto;
}
.login-form {
  margin-top: 80rpx;
}
.field {
  margin-top: 28rpx;
  background: #f7f8fa;
  border: 2rpx solid #e5e7ec;
  border-radius: 16rpx;
  padding: 0 24rpx;
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
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 20rpx;
  background: #e15536;
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 600;
  text-align: center;
  letter-spacing: 4rpx;
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
.login-agree {
  margin-top: 40rpx;
  text-align: center;
  color: var(--c-text-4);
  font-size: var(--f-tip);
}
.login-footer {
  margin-top: 48rpx;
  text-align: center;
  color: var(--c-text-4);
  font-size: var(--f-tip);
}
</style>
