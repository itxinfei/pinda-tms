<template>
  <view class="page">
    <u-navbar :title="'我的'" :back="false" :borderBottom="false" bgColor="#FFFFFF" title-color="#000000" left-icon-color="var(--c-text-1)" placeholder />

    <!-- 用户信息 -->
    <view class="profile-hero">
      <view class="avatar">
        <image v-if="profile && profile.avatar" :src="profile.avatar" mode="aspectFill" class="avatar-img" />
        <view v-else class="avatar-default">{{ profileName }}</view>
      </view>
      <view class="profile-info">
        <view class="profile-name">{{ profile ? (profile.name || '心飞用户') : '心飞用户' }}</view>
        <view class="profile-phone">{{ profile ? maskPhone(profile.phone) : '加载中...' }}</view>
      </view>
    </view>

    <!-- 功能入口：分组卡片（《70》§3.1） -->
    <view v-for="(group, gi) in menuGroups" :key="group.title" class="menu-group">
      <view class="group-title">{{ group.title }}</view>
      <view class="card menu-card" :class="{ 'menu-card-first': gi === 0 }">
        <view
          v-for="item in group.items"
          :key="item.text"
          class="menu-row"
          hover-class="menu-hover"
          @click="item.click"
        >
          <u-icon :name="item.icon" color="var(--c-primary)" size="20" />
          <text class="menu-text">{{ item.text }}</text>
          <u-icon name="arrow-right" color="var(--c-text-4)" size="18" />
        </view>
      </view>
    </view>

    <view class="action-bar">
      <u-button type="error" shape="circle" plain text="退出登录" class="logout-btn" @click="handleLogout" />
    </view>
    <view style="height: 140rpx"></view>

    <u-tabbar :value="2" :fixed="true" :placeholder="true" :safeAreaInsetBottom="true">
      <u-tabbar-item text="首页" icon="home" @click="goHome" />
      <u-tabbar-item text="我的运单" icon="list" @click="goMailingList" />
      <u-tabbar-item text="我的" icon="account" @click="goProfile" />
    </u-tabbar>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { useUserStore } from '../../../stores/user.js';
import { maskPhone, maskName } from '../../../common/utils/mask.js';

const userStore = useUserStore();
const profile = ref(null);

const profileName = computed(() => {
  const name = profile.value && profile.value.name ? profile.value.name : (userStore.userInfo && userStore.userInfo.name);
  return name ? maskName(name) : '客';
});

const load = async () => {
  if (!userStore.isLogin) {
    uni.reLaunch({ url: '/pages/customer/login/index' });
    return;
  }
  try {
    profile.value = await userStore.fetchProfile();
  } catch (e) {
    profile.value = null;
  }
};

const goAddress = () => uni.navigateTo({ url: '/pages/customer/address/list' });
const goMailingList = () => uni.reLaunch({ url: '/pages/customer/mailing/list' });
const goAgency = () => uni.navigateTo({ url: '/pages/customer/agency/list' });
const goAbout = () => uni.showToast({ title: '心飞物流TMS v1.0', icon: 'none' });
const goService = () => uni.showToast({ title: '客服热线即将开放', icon: 'none' });
const goHome = () => uni.reLaunch({ url: '/pages/customer/home/index' });
const goProfile = () => uni.reLaunch({ url: '/pages/customer/profile/index' });

// 菜单分组（《70》§3.1：寄件服务 / 设置 / 关于）
const menuGroups = [
  {
    title: '寄件服务',
    items: [
      { text: '我的运单', icon: 'order', click: goMailingList },
      { text: '网点查询', icon: 'map', click: goAgency },
    ],
  },
  {
    title: '设置',
    items: [
      { text: '地址簿', icon: 'list-dot', click: goAddress },
      { text: '联系客服', icon: 'server', click: goService },
    ],
  },
  {
    title: '关于',
    items: [{ text: '关于心飞', icon: 'info-circle', click: goAbout }],
  },
];

const handleLogout = () => {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出当前账号吗？',
    success: (res) => {
      if (res.confirm) {
        userStore.logout();
      }
    },
  });
};

onShow(() => {
  load();
});
</script>

<style lang="scss" scoped>
.profile-hero {
  background: linear-gradient(135deg, #FF0033 0%, #CC0029 100%);
  padding: 48rpx 40rpx 64rpx;
  display: flex;
  align-items: center;
}
.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 24rpx;
  background: rgba(255, 255, 255, 0.25);
}
.avatar-img { width: 100%; height: 100%; }
.avatar-default {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 48rpx;
  font-weight: 700;
}
.profile-name {
  color: #fff;
  font-size: var(--f-title);
  font-weight: 600;
}
.profile-phone {
  margin-top: 8rpx;
  color: rgba(255, 255, 255, 0.85);
  font-size: var(--f-aux);
}
.menu-group {
  margin-top: 8rpx;
}
.group-title {
  margin: 0 32rpx 12rpx;
  font-size: var(--f-tip);
  color: var(--c-text-3);
}
.menu-card {
  margin: 0 24rpx 20rpx;
  padding: 8rpx 24rpx;
}
.menu-card-first {
  margin-top: -24rpx;
}
.menu-row {
  display: flex;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1rpx solid var(--c-border);
}
.menu-row:last-child { border-bottom: none; }
.menu-hover {
  background-color: rgba(255, 0, 51, 0.04);
}
.menu-text {
  flex: 1;
  margin-left: 16rpx;
  font-size: var(--f-body);
  color: var(--c-text-1);
}
.logout-btn { height: 88rpx; }
</style>
