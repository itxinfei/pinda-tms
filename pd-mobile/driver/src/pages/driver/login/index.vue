<template>
  <view class="page login">
    <view class="login-hero">
      <text class="login-logo">品达物流 · 司机端</text>
      <text class="login-sub">运输任务 · 全程可视</text>
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
import { ref, reactive } from 'vue'
import { loginApi, captchaSrc } from '@/common/api/driver'
import { useUserStore } from '@/common/store/user'

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
    })
    const token = res && res.token && res.token.token
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
.login {
  padding: 0;
}
.login-hero {
  background: var(--c-primary);
  padding: calc(var(--s-8) * 3) var(--s-6) var(--s-8);
  display: flex;
  flex-direction: column;
}
.login-logo {
  color: var(--c-surface);
  font-size: 56rpx;
  font-weight: bold;
}
.login-sub {
  color: rgba(255, 255, 255, 0.85);
  font-size: var(--f-aux);
  margin-top: var(--s-2);
}
.login-card {
  margin: calc(var(--s-8) * -1) var(--s-5) 0;
  border-radius: var(--r-lg);
}
.captcha-img {
  width: 180rpx;
  height: 64rpx;
  border-radius: var(--r-sm);
}
</style>
