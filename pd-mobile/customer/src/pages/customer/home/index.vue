<template>
  <view class="page home-page">
    <u-navbar :title="'品达物流'" :back="false" :borderBottom="false" bgColor="#2B6CFF" placeholder>
      <template #left><view /></template>
    </u-navbar>

    <!-- 欢迎区 -->
    <view class="home-hero">
      <view class="home-title">您好，欢迎使用品达物流</view>
      <view class="home-count">
        <view class="count-num">{{ countNum }}</view>
        <view class="count-label">当前进行中的订单</view>
      </view>
    </view>

    <!-- 功能入口 -->
    <view class="home-entries">
      <view class="entry-card" @click="goMailing">
        <view class="entry-icon icon-mail">寄</view>
        <view class="entry-name">我要寄件</view>
        <view class="entry-desc">在线下单 · 运费即时试算</view>
      </view>
      <view class="entry-card" @click="goList">
        <view class="entry-icon icon-list">查</view>
        <view class="entry-name">查快递</view>
        <view class="entry-desc">运单列表 · 轨迹查询</view>
      </view>
      <view class="entry-card" @click="goAddress">
        <view class="entry-icon icon-addr">址</view>
        <view class="entry-name">地址簿</view>
        <view class="entry-desc">常用地址管理</view>
      </view>
      <view class="entry-card" @click="goAgency">
        <view class="entry-icon icon-ag">网</view>
        <view class="entry-name">网点查询</view>
        <view class="entry-desc">附近网点自寄参考</view>
      </view>
    </view>

    <u-tabbar :value="0" :fixed="true" :placeholder="true" :safeAreaInsetBottom="true">
      <u-tabbar-item text="首页" icon="home" @click="goHome" />
      <u-tabbar-item text="我的运单" icon="list" @click="goList" />
      <u-tabbar-item text="我的" icon="account" @click="goProfile" />
    </u-tabbar>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { count } from '../../../common/api/mailing.js';
import { useUserStore } from '../../../stores/user.js';

const userStore = useUserStore();
const countNum = ref('-');

const loadCount = async () => {
  try {
    const data = await count();
    countNum.value = data && data.count !== undefined ? data.count : 0;
  } catch (e) {
    countNum.value = '-';
  }
};

const goMailing = () => uni.navigateTo({ url: '/pages/customer/mailing/create' });
const goList = () => uni.navigateTo({ url: '/pages/customer/mailing/list' });
const goAddress = () => uni.navigateTo({ url: '/pages/customer/address/list' });
const goAgency = () => uni.navigateTo({ url: '/pages/customer/agency/list' });
const goHome = () => uni.navigateTo({ url: '/pages/customer/home/index' });
const goProfile = () => uni.navigateTo({ url: '/pages/customer/profile/index' });

onShow(() => {
  if (!userStore.isLogin) {
    uni.reLaunch({ url: '/pages/customer/login/index' });
    return;
  }
  loadCount();
});
</script>

<style lang="scss" scoped>
.home-page {
  padding: 0;
}
.home-hero {
  background: linear-gradient(160deg, #2B6CFF 0%, #1A4FD6 100%);
  padding: 40rpx 48rpx 64rpx;
  color: #fff;
}
.home-title {
  font-size: var(--f-title);
  font-weight: 600;
}
.home-count {
  margin-top: 32rpx;
  display: flex;
  align-items: baseline;
}
.count-num {
  font-size: 72rpx;
  font-weight: 700;
  margin-right: 16rpx;
}
.count-label {
  color: rgba(255, 255, 255, 0.85);
  font-size: var(--f-aux);
}
.home-entries {
  display: flex;
  flex-wrap: wrap;
  margin: -24rpx 24rpx 0;
}
.entry-card {
  width: calc(50% - 12rpx);
  margin: 12rpx 6rpx;
  background: var(--c-surface);
  border-radius: var(--r-md);
  padding: 28rpx 24rpx;
  box-shadow: var(--shadow-card);
  box-sizing: border-box;
}
.entry-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: var(--r-md);
  background: var(--c-primary-light);
  color: var(--c-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  font-weight: 600;
}
.entry-name {
  margin-top: 16rpx;
  font-size: var(--f-sub);
  color: var(--c-text-1);
  font-weight: 600;
}
.entry-desc {
  margin-top: 6rpx;
  font-size: var(--f-tip);
  color: var(--c-text-3);
}
</style>
