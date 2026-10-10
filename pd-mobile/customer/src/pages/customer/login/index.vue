<template>
  <view class="page login-page">
    <u-navbar :title="'客户登录'" :back="false" :borderBottom="false" bgColor="#2B6CFF" placeholder>
      <template #left><view /></template>
    </u-navbar>

    <!-- 品牌区 -->
    <view class="login-hero">
      <image class="login-logo" src="/static/logo-full.png" mode="widthFix" />
      <view class="login-slogan">品质速递 · 使命必达</view>
    </view>

    <!-- 表单卡片 -->
    <view class="login-card">
      <u-form :model="form" :rules="rules" ref="formRef">
        <u-form-item label="账号" prop="account" borderBottom>
          <u-input v-model="form.account" placeholder="请输入账号" border="none" :clearable="true" />
        </u-form-item>
        <u-form-item label="密码" prop="password" borderBottom>
          <u-input v-model="form.password" placeholder="请输入密码" border="none" type="password" :clearable="true" />
        </u-form-item>
        <u-form-item label="验证码" prop="code" borderBottom>
          <view class="captcha-row">
            <u-input v-model="form.code" placeholder="输入验证码" border="none" class="captcha-input" />
            <image class="captcha-img" :src="captchaSrc" mode="aspectFit" @click="refreshCaptcha" />
          </view>
        </u-form-item>
      </u-form>

      <u-button type="primary" shape="circle" :loading="loading" class="login-btn" @click="handleLogin">
        登 录
      </u-button>
      <view class="login-tip">账号登录 · 微信免密登录开发中</view>
    </view>

    <view class="login-footer">品达物流 TMS · 客户端</view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useUserStore } from '../../../stores/user.js';
import { login, captchaUrl } from '../../../common/api/auth.js';

const userStore = useUserStore();
const formRef = ref(null);
const loading = ref(false);
const key = ref('');
const captchaSrc = ref('');

const form = reactive({ account: '', password: '', code: '' });
const rules = {
  account: [{ required: true, message: '请输入账号', type: 'string' }],
  password: [{ required: true, message: '请输入密码', type: 'string' }],
  code: [{ required: true, message: '请输入验证码', type: 'string' }],
};

const refreshCaptcha = () => {
  key.value = 'k' + Date.now() + Math.floor(Math.random() * 1000);
  captchaSrc.value = captchaUrl(key.value);
};

const showPrivacy = () => {
  const agreed = uni.getStorageSync('cst_privacy_agreed');
  if (agreed) return;
  uni.showModal({
    title: '隐私政策提示',
    content: '为向您提供寄件、查件、轨迹等服务，我们需要收集您的手机号、地址、订单及位置信息（最小必要）。完整隐私政策见《用户协议》。拒绝授权将无法使用寄件功能。',
    confirmText: '同意',
    cancelText: '暂不使用',
    success: (res) => {
      if (res.confirm) {
        uni.setStorageSync('cst_privacy_agreed', '1');
      }
    }
  });
};

const handleLogin = async () => {
  try {
    await formRef.value.validate();
  } catch (e) {
    return;
  }
  loading.value = true;
  try {
    const data = await login({
      account: form.account,
      password: form.password,
      key: key.value,
      code: form.code,
    });
    // JWT 在 data.token.token（两层）
    const jwt = data && data.token && data.token.token;
    if (!jwt) {
      uni.showToast({ title: '登录失败：未返回凭证', icon: 'none' });
      return;
    }
    userStore.setToken(jwt);
    userStore.setUserInfo(data.user || null);
    uni.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/customer/home/index' });
    }, 500);
  } catch (err) {
    refreshCaptcha();
    form.code = '';
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  showPrivacy();
  if (userStore.isLogin) {
    uni.reLaunch({ url: '/pages/customer/home/index' });
    return;
  }
  refreshCaptcha();
});
</script>

<style lang="scss" scoped>
.login-page {
  background: var(--c-bg);
  padding: 0;
  min-height: 100vh;
}
.login-hero {
  background: linear-gradient(160deg, #2B6CFF 0%, #1A4FD6 100%);
  padding: 60rpx 48rpx 80rpx;
  text-align: center;
}
.login-logo {
  width: 360rpx;
  height: auto;
  margin: 0 auto;
}
.login-slogan {
  margin-top: 12rpx;
  color: rgba(255, 255, 255, 0.85);
  font-size: var(--f-body);
}
.login-card {
  margin: -40rpx 32rpx 0;
  background: var(--c-surface);
  border-radius: var(--r-lg);
  padding: 40rpx 32rpx;
  box-shadow: var(--shadow-card);
}
.captcha-row {
  display: flex;
  align-items: center;
  flex: 1;
}
.captcha-input {
  flex: 1;
}
.captcha-img {
  width: 180rpx;
  height: 64rpx;
  margin-left: 16rpx;
  border-radius: var(--r-sm);
}
.login-btn {
  margin-top: 40rpx;
  height: 88rpx;
}
.login-tip {
  margin-top: 24rpx;
  text-align: center;
  color: var(--c-text-3);
  font-size: var(--f-tip);
}
.login-footer {
  margin-top: 60rpx;
  text-align: center;
  color: var(--c-text-4);
  font-size: var(--f-tip);
}
</style>
