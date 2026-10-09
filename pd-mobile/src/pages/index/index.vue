<template>
  <view class="container">
    <view class="header">
      <image class="logo" src="/static/logo.png"></image>
      <view class="app-name">品达物流</view>
      <view class="app-desc">运输管理系统</view>
    </view>

    <view class="features">
      <view class="feature-item">
        <view class="feature-icon">📦</view>
        <view class="feature-text">运输调度</view>
      </view>
      <view class="feature-item">
        <view class="feature-icon">📍</view>
        <view class="feature-text">在途跟踪</view>
      </view>
      <view class="feature-item">
        <view class="feature-icon">⚠️</view>
        <view class="feature-text">异常告警</view>
      </view>
    </view>

    <view class="version">v1.0.0</view>
  </view>
</template>

<script>
export default {
  data() {
    return {}
  },
  onLoad() {},
  methods: {},
}
</script>

<style>
.container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 40rpx;
}

.header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 100rpx;
}

.logo {
  height: 160rpx;
  width: 160rpx;
  margin-bottom: 30rpx;
}

.app-name {
  font-size: 48rpx;
  font-weight: bold;
  color: #2B6CFF;
  margin-bottom: 10rpx;
}

.app-desc {
  font-size: 28rpx;
  color: #666;
}

.features {
  display: flex;
  justify-content: space-around;
  width: 100%;
  margin-bottom: 100rpx;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.feature-icon {
  font-size: 60rpx;
  margin-bottom: 16rpx;
}

.feature-text {
  font-size: 26rpx;
  color: #333;
}

.version {
  font-size: 24rpx;
  color: #999;
}
</style>
