<template>
  <view class="page login-page">
    <!-- 品牌区：极简白底，仅 logo -->
    <view class="login-brand">
      <image class="login-logo" src="/static/logo-full.png" mode="widthFix" />
    </view>

    <!-- 表单区：白底描边输入框 + 橙色实心按钮 -->
    <view class="login-form">
      <view class="field">
        <u-input v-model="form.account" placeholder="请输入账号" border="none" :clearable="true" prefixIcon="account" />
      </view>
      <view class="field">
        <u-input v-model="form.password" placeholder="请输入密码" border="none" type="password" :clearable="true" prefixIcon="lock" />
      </view>
      <view class="field field-captcha">
        <u-input v-model="form.code" placeholder="输入验证码" border="none" class="captcha-input" />
        <image class="captcha-img" :src="captchaSrc" mode="aspectFill" @click="refreshCaptcha" />
      </view>

      <view class="login-btn" :class="{ 'is-loading': loading }" @click="handleLogin">
        {{ loading ? '登录中...' : '登 录' }}
      </view>

      <view class="login-tip">账号登录 · 微信免密登录开发中</view>
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
