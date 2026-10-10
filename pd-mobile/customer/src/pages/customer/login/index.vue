<template>
  <view class="page login-page">
    <!-- 全屏背景插画（神领橙系） -->
    <image class="login-bg" src="/static/login-bg.png" mode="aspectFill" />

    <!-- 品牌区：放大 logo + 端名 -->
    <view class="login-brand">
      <image class="login-logo" src="/static/logo-full.png" mode="widthFix" />
      <view class="login-end">品达物流 · 客户端</view>
    </view>

    <!-- 表单卡 -->
    <view class="login-card">
      <view class="login-title">欢迎回来</view>
      <view class="login-sub">请登录账号，开启高效物流服务</view>

      <view class="field">
        <u-input v-model="form.account" placeholder="请输入账号" border="none" :clearable="true" prefixIcon="account" height="88rpx" />
      </view>
      <view class="field">
        <u-input v-model="form.password" placeholder="请输入密码" border="none" type="password" :clearable="true" prefixIcon="lock" height="88rpx" />
      </view>
      <view class="field field-captcha">
        <u-input v-model="form.code" placeholder="输入验证码" border="none" class="captcha-input" height="88rpx" />
        <image class="captcha-img" :src="captchaSrc" mode="aspectFill" @click="refreshCaptcha" />
      </view>

      <view class="login-btn" :class="{ 'is-loading': loading }" @click="handleLogin">
        {{ loading ? '登录中...' : '登 录' }}
      </view>

      <view class="login-agree">登录即代表同意《用户协议》和《隐私政策》</view>
    </view>

    <view class="login-footer">品达物流 TMS · 客户端</view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useUserStore } from '../../../stores/user.js';
import { login, captchaUrl } from '../../../common/api/auth.js';
import { TENANT_CODE } from '../../../common/config';

const userStore = useUserStore();
const loading = ref(false);
const key = ref('');
const captchaSrc = ref('');

const form = reactive({ account: '', password: '', code: '' });

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
  if (!form.account || !form.password || !form.code) {
    uni.showToast({ title: '请填写账号、密码和验证码', icon: 'none' });
    return;
  }
  loading.value = true;
  try {
    const data = await login({
      account: form.account,
      password: form.password,
      key: key.value,
      code: form.code,
      tenantCode: TENANT_CODE,
    });
    // JWT 在 data.token（后端返回字符串；兼容历史两层写法）
    const jwt = (data && data.token && data.token.token) || (data && data.token);
    if (!jwt) {
      uni.showToast({ title: '登录失败：未返回凭证', icon: 'none' });
      refreshCaptcha();
      form.code = '';
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
  position: relative;
  min-height: 100vh;
  background: #1a1c41;
}
.login-bg {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}
.login-brand {
  position: relative;
  z-index: 1;
  padding-top: 130rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.login-logo {
  width: 280rpx;
  height: auto;
  filter: drop-shadow(0 6rpx 18rpx rgba(0, 0, 0, 0.35));
}
.login-end {
  margin-top: 16rpx;
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 600;
  letter-spacing: 2rpx;
  text-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.4);
}
.login-card {
  position: relative;
  z-index: 1;
  margin: 56rpx var(--s-5) 0;
  background: rgba(255, 255, 255, 0.97);
  border-radius: 32rpx;
  padding: 44rpx 36rpx 36rpx;
  box-shadow: 0 24rpx 60rpx rgba(0, 0, 0, 0.3);
}
.login-title {
  font-size: 38rpx;
  font-weight: 700;
  color: var(--c-text-1);
}
.login-sub {
  margin-top: 6rpx;
  font-size: var(--f-aux);
  color: var(--c-text-3);
}
.field {
  margin-top: 28rpx;
  height: 92rpx;
  background: #f7f8fa;
  border: 2rpx solid #e7e9ee;
  border-radius: 20rpx;
  padding: 0 26rpx;
  display: flex;
  align-items: center;
}
.field-captcha {
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
  margin-top: 44rpx;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 24rpx;
  background: #e15536;
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 600;
  text-align: center;
  letter-spacing: 4rpx;
  transition: opacity 0.2s;
}
.login-btn:active {
  opacity: 0.85;
}
.login-btn.is-loading {
  opacity: 0.7;
}
.login-agree {
  margin-top: 32rpx;
  text-align: center;
  color: var(--c-text-4);
  font-size: var(--f-tip);
}
.login-footer {
  position: relative;
  z-index: 1;
  margin-top: 40rpx;
  padding-bottom: 48rpx;
  text-align: center;
  color: rgba(255, 255, 255, 0.85);
  font-size: var(--f-tip);
  text-shadow: 0 1rpx 8rpx rgba(0, 0, 0, 0.35);
}
</style>
