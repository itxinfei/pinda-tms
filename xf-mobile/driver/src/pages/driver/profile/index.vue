<template>
  <view class="page">
    <view class="hero">
      <u-avatar :text="maskName(userInfo?.name) || '司'" fontSize="36"></u-avatar>
      <view class="hero-info">
        <text class="hero-name">{{ maskName(userInfo?.name) || '司机' }}</text>
        <text class="hero-phone">{{ maskPhone(userInfo?.phone) }}</text>
      </view>
    </view>

    <view class="card">
      <view class="row">
        <text class="label">账号</text>
        <text class="value">{{ profile.account || userInfo?.account || '—' }}</text>
      </view>
      <u-line></u-line>
      <view class="row">
        <text class="label">姓名</text>
        <text class="value">{{ maskName(profile.name || userInfo?.name) || '—' }}</text>
      </view>
      <u-line></u-line>
      <view class="row">
        <text class="label">手机号</text>
        <text class="value">{{ maskPhone(profile.phone || userInfo?.phone) }}</text>
      </view>
      <u-line></u-line>
      <view class="row">
        <text class="label">所属车队</text>
        <text class="value">{{ profile.team || userInfo?.team || '—' }}</text>
      </view>
    </view>

    <u-gap height="20"></u-gap>
    <view class="card">
      <view class="row menu-row" hover-class="row-hover" @click="goCarInfo">
        <text class="label">我的车辆</text>
        <view class="menu-right">
          <u-icon name="arrow-right" color="var(--c-text-4)" size="16" />
        </view>
      </view>
    </view>

    <u-gap height="20"></u-gap>
    <u-button
      type="error"
      text="退出登录"
      shape="circle"
      plain
      @click="onLogout"
    ></u-button>

    <view class="version">心飞司机宝 v{{ appVersion }}</view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { userProfile } from '@/common/api/driver'
import { useUserStore } from '@/common/store/user'
import { LOGIN_PAGE } from '@/common/config'
import { maskName, maskPhone } from '@/common/utils/desensitive'
import { onShow } from '@dcloudio/uni-app'
import { version as appVersion } from '../../../../package.json'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const profile = ref<any>({})

async function loadProfile() {
  try {
    profile.value = await userProfile()
  } catch (e) {
    profile.value = {}
  }
}

function goCarInfo() {
  uni.navigateTo({ url: '/pages/driver/car/info' })
}

function onLogout() {
  uni.showModal({
    title: '提示',
    content: '确认退出登录？',
    success: (res) => {
      if (res.confirm) {
        // 修改点：清除登录态后跳登录页
        userStore.logout()
        uni.reLaunch({ url: LOGIN_PAGE })
      }
    },
  })
}

onShow(() => loadProfile())
</script>

<style lang="scss">
.hero {
  background: linear-gradient(135deg, var(--c-primary) 0%, var(--c-primary-dark) 100%);
  margin: calc(var(--s-4) * -1) calc(var(--s-4) * -1) var(--s-4);
  padding: var(--s-8) var(--s-6);
  display: flex;
  align-items: center;
}
.hero-info {
  margin-left: var(--s-4);
  display: flex;
  flex-direction: column;
}
.hero-name {
  color: var(--c-surface);
  font-size: var(--f-title);
  font-weight: bold;
}
.hero-phone {
  color: rgba(255, 255, 255, 0.85);
  font-size: var(--f-aux);
  margin-top: var(--s-1);
}
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--s-3) 0;
}
.label {
  font-size: var(--f-body);
  color: var(--c-text-3);
}
.value {
  font-size: var(--f-body);
  color: var(--c-text-1);
}
.menu-right {
  display: flex;
  align-items: center;
}
.row-hover {
  background-color: rgba(255, 0, 51, 0.04);
}
.version {
  margin-top: var(--s-4);
  text-align: center;
  font-size: var(--f-tip);
  color: var(--c-text-3);
}
</style>
