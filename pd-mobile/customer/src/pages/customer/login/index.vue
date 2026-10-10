<template>
  <view class="page login-page">
    <!-- 品牌区（深色科技渐变，广联达风格） -->
    <view class="login-hero">
      <image class="login-logo" src="/static/logo-full.png" mode="widthFix" />
      <view class="login-brand">品达物流</view>
      <view class="login-slogan">品质速递 · 使命必达</view>
    </view>

    <!-- 登录卡片 -->
    <view class="login-card">
      <view class="login-title">欢迎回来</view>
      <view class="login-sub">请登录账号，开启高效物流服务</view>

      <view class="field">
        <u-input v-model="form.account" placeholder="请输入账号" border="none" :clearable="true" />
      </view>
      <view class="field">
        <u-input v-model="form.password" placeholder="请输入密码" border="none" type="password" :clearable="true" />
      </view>
      <view class="field field-captcha">
        <u-input v-model="form.code" placeholder="输入验证码" border="none" class="captcha-input" />
        <image class="captcha-img" :src="captchaSrc" mode="aspectFill" @click="refreshCaptcha" />
      </view>

      <view class="login-btn" :class="{ 'is-loading': loading }" @click="handleLogin">
        {{ loading ? '登录中...' : '登 录' }}
      </view>
      <view class="login-tip">账号登录 · 微信免密登录开发中</view>
    </view>

    <!-- 背景装饰插画 -->
    <view class="login-bg-deco">
      <image src="/static/login-bg.png" mode="widthFix" />
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
  min-height: 100vh;
  padding: 0;
  background: linear-gradient(160deg, #1a1c41 0%, #283443 55%, #2d3a4b 100%);
  position: relative;
  overflow: hidden;
}
.login-hero {
  padding: calc(var(--s-8) * 3) var(--s-6) 120rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}
.login-logo {
  width: 260rpx;
  height: auto;
  filter: drop-shadow(0 8rpx 20rpx rgba(0, 0, 0, 0.35));
}
.login-brand {
  margin-top: 28rpx;
  color: #ffffff;
  font-size: 44rpx;
  font-weight: 700;
  letter-spacing: 6rpx;
}
.login-slogan {
  margin-top: 14rpx;
  color: rgba(255, 255, 255, 0.7);
  font-size: var(--f-aux);
  letter-spacing: 2rpx;
}
.login-card {
  margin: 0 var(--s-6);
  background: rgba(255, 255, 255, 0.97);
  border-radius: 32rpx;
  padding: 48rpx 40rpx;
  box-shadow: 0 24rpx 64rpx rgba(0, 0, 0, 0.28);
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
.login-bg-deco {
  position: absolute;
  left: 0;
  right: 0;
  bottom: -20rpx;
  z-index: 1;
  opacity: 0.55;
  pointer-events: none;
}
.login-bg-deco image {
  width: 100%;
}
.login-footer {
  position: absolute;
  bottom: 40rpx;
  left: 0;
  right: 0;
  z-index: 2;
  text-align: center;
  color: rgba(255, 255, 255, 0.5);
  font-size: var(--f-tip);
}
</style>
