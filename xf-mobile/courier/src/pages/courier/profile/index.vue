<template>
  <view class="page">
    <!-- 个人信息 -->
    <view class="card profile-card">
      <u-avatar :text="(profile?.name || '快').slice(0, 1)" bgColor="var(--c-primary)"></u-avatar>
      <view class="profile-info">
        <text class="profile-name">{{ profile?.name || '—' }}</text>
        <text class="profile-role">{{ roleText }}</text>
      </view>
    </view>

    <view class="card">
      <view class="row"><text class="row-key">手机号</text><text class="row-val">{{ maskPhone(profile?.phoneNumber || profile?.phone) }}</text></view>
      <view class="row"><text class="row-key">角色</text><text class="row-val">{{ roleText }}</text></view>
      <view class="row"><text class="row-key">账号</text><text class="row-val">{{ profile?.account || '—' }}</text></view>
    </view>

    <u-gap height="20"></u-gap>
    <u-button type="error" text="退出登录" shape="circle" @click="onLogout"></u-button>
  </view>
</template>

<script setup lang="ts">
// 个人中心：调 userProfile 展示资料，退出登录后 reLaunch 到登录页
import { ref, computed } from 'vue'
import { userProfile } from '@/common/api/courier'
import { useUserStore } from '@/common/store/user'
import { maskPhone } from '@/common/utils/desensitive'
import { LOGIN_PAGE } from '@/common/config'
import { onShow } from '@dcloudio/uni-app'

const profile = ref<any>(null)
const userStore = useUserStore()
const roleText = computed(() => '快递员')

async function loadProfile() {
  try {
    profile.value = await userProfile()
  } catch (e) {
    profile.value = null
  }
}

function onLogout() {
  uni.showModal({
    title: '提示',
    content: '确定退出登录？',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
        uni.reLaunch({ url: LOGIN_PAGE })
      }
    },
  })
}

onShow(() => loadProfile())
</script>

<style lang="scss">
.profile-card {
  display: flex;
  align-items: center;
}
.profile-info {
  margin-left: var(--s-4);
  display: flex;
  flex-direction: column;
}
.profile-name {
  font-size: var(--f-sub);
  color: var(--c-text-1);
  font-weight: bold;
}
.profile-role {
  font-size: var(--f-aux);
  color: var(--c-text-3);
  margin-top: var(--s-1);
}
.row {
  display: flex;
  font-size: var(--f-aux);
  margin-top: var(--s-2);
}
.row-key {
  width: 140rpx;
  color: var(--c-text-3);
}
.row-val {
  flex: 1;
  color: var(--c-text-2);
}
</style>
