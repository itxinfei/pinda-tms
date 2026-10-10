<template>
  <view class="page">
    <u-navbar title="个人中心" autoBack></u-navbar>
    <view class="card profile-card">
      <u-avatar :text="avatarText" fontSize="36" bgColor="#2B6CFF"></u-avatar>
      <view class="profile-info">
        <text class="profile-name">{{ maskName(userInfo?.name) || '客户' }}</text>
        <text class="profile-phone">{{ maskPhone(userInfo?.mobile) }}</text>
      </view>
    </view>

    <view class="card">
      <u-cell-group>
        <u-cell title="我的地址" isLink @click="go('pages/customer/address/list')" />
        <u-cell title="我的运单" isLink @click="goTab('pages/customer/mailing/list')" />
      </u-cell-group>
    </view>

    <u-gap height="40"></u-gap>
    <u-button text="退出登录" type="error" plain shape="circle" @click="onLogout"></u-button>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { userProfile, logoutApi } from '@/common/api/customer'
import { useUserStore } from '@/common/store/user'
import { maskName, maskPhone } from '@/common/utils/desensitive'

const userStore = useUserStore()
const userInfo = ref<any>(null)
const avatarText = computed(() => (userInfo.value?.name || '客').charAt(0))

onShow(() => load())

async function load() {
  try {
    userInfo.value = await userProfile()
  } catch (e) {}
}
function go(p: string) {
  uni.navigateTo({ url: '/' + p })
}
function goTab(p: string) {
  uni.switchTab({ url: '/' + p })
}
async function onLogout() {
  uni.showModal({
    title: '提示',
    content: '确定退出登录？',
    success: async (r) => {
      if (r.confirm) {
        try {
          await logoutApi()
        } catch (e) {}
        userStore.logout()
        uni.reLaunch({ url: '/pages/customer/login/index' })
      }
    },
  })
}
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
.profile-phone {
  font-size: var(--f-aux);
  color: var(--c-text-2);
  margin-top: var(--s-1);
}
</style>
