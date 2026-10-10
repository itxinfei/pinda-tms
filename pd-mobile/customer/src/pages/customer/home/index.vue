<template>
  <view class="page">
    <view class="hero">
      <view class="hero-title">您好，寄件更省心</view>
      <view class="hero-sub">实时追踪 · 上门取件 · 电子签收</view>
    </view>

    <view class="card entry-grid">
      <view class="entry" @click="go('pages/customer/mailing/create')">
        <u-icon name="car" size="44" color="#2B6CFF"></u-icon>
        <text class="entry-text">我要寄件</text>
      </view>
      <view class="entry" @click="goTab('pages/customer/mailing/list')">
        <u-icon name="order" size="44" color="#2B6CFF"></u-icon>
        <text class="entry-text">查快递</text>
      </view>
      <view class="entry" @click="go('pages/customer/address/list')">
        <u-icon name="map" size="44" color="#2B6CFF"></u-icon>
        <text class="entry-text">我的地址</text>
      </view>
      <view class="entry" @click="goTab('pages/customer/profile/index')">
        <u-icon name="account" size="44" color="#2B6CFF"></u-icon>
        <text class="entry-text">个人中心</text>
      </view>
    </view>

    <view class="card stat-card" v-if="stat">
      <view class="stat-item">
        <text class="stat-num">{{ inProgress }}</text>
        <text class="stat-label">进行中</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ done }}</text>
        <text class="stat-label">已完成</text>
      </view>
    </view>

    <u-gap height="20"></u-gap>
    <u-button
      type="primary"
      text="立即寄件"
      shape="circle"
      @click="go('pages/customer/mailing/create')"
    ></u-button>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { mailingCount } from '@/common/api/customer'
import { onShow } from '@dcloudio/uni-app'

const stat = ref<any>(null)
const inProgress = computed(() => stat.value?.inProgress ?? stat.value?.progress ?? 0)
const done = computed(() => stat.value?.done ?? stat.value?.finished ?? 0)

function go(path: string) {
  uni.navigateTo({ url: '/' + path })
}
function goTab(path: string) {
  uni.switchTab({ url: '/' + path })
}

async function loadCount() {
  try {
    stat.value = await mailingCount()
  } catch (e) {
    stat.value = null
  }
}
onShow(() => loadCount())
</script>

<style lang="scss">
.hero {
  background: var(--c-primary);
  margin: calc(var(--s-4) * -1) calc(var(--s-4) * -1) var(--s-4);
  padding: var(--s-8) var(--s-6);
}
.hero-title {
  color: var(--c-surface);
  font-size: var(--f-title);
  font-weight: bold;
}
.hero-sub {
  color: rgba(255, 255, 255, 0.85);
  font-size: var(--f-aux);
  margin-top: var(--s-2);
}
.entry-grid {
  display: flex;
  justify-content: space-around;
}
.entry {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.entry-text {
  font-size: var(--f-aux);
  color: var(--c-text-2);
  margin-top: var(--s-2);
}
.stat-card {
  display: flex;
}
.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stat-num {
  font-size: 40rpx;
  color: var(--c-primary);
  font-weight: bold;
}
.stat-label {
  font-size: var(--f-tip);
  color: var(--c-text-3);
  margin-top: var(--s-1);
}
</style>
