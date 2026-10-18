<template>
  <view class="page">
    <!-- 顶部 hero：品牌标题栏 + 欢迎语（页级 navigationStyle:custom） -->
    <view class="hero">
      <view class="hero-bar"><text class="hero-brand">快递员工作台</text></view>
      <view class="hero-title">您好，快递员</view>
      <view class="hero-sub">取件派件 · 扫码接单 · 实时追踪</view>
    </view>

    <!-- 取件 / 派件任务量（count 契约已归一 data 包裹，《70》§3.3①） -->
    <view class="card stat-card" v-if="stat">
      <view class="stat-item">
        <text class="stat-num">{{ pickupCount }}</text>
        <text class="stat-label">取件任务</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ dispatchCount }}</text>
        <text class="stat-label">派件任务</text>
      </view>
    </view>

    <!-- 取件 / 派件 双标签 -->
    <u-tabs
      :list="tabs"
      :current="tabIndex"
      @click="onTab"
      lineColor="var(--c-primary)"
      :activeStyle="{ color: 'var(--c-primary)' }"
      :inactiveStyle="{ color: 'var(--c-text-2)' }"
    ></u-tabs>

    <u-gap height="16"></u-gap>

    <!-- 功能宫格：扫码接单 / 我的任务 / 绩效统计 / 批量入库 -->
    <view class="card entry-grid">
      <view class="entry" @click="onScan">
        <view class="entry-icon"><u-icon name="scan" size="40" color="var(--c-primary)"></u-icon></view>
        <text class="entry-text">扫码接单</text>
      </view>
      <view class="entry" @click="goTab('pages/courier/task/list')">
        <view class="entry-icon"><u-icon name="list" size="40" color="var(--c-primary)"></u-icon></view>
        <text class="entry-text">我的任务</text>
      </view>
      <view class="entry" @click="goTab('pages/courier/stats/index')">
        <view class="entry-icon"><u-icon name="rmb" size="40" color="var(--c-primary)"></u-icon></view>
        <text class="entry-text">绩效统计</text>
      </view>
      <view class="entry" @click="goWarehousing">
        <view class="entry-icon"><u-icon name="box" size="40" color="var(--c-primary)"></u-icon></view>
        <text class="entry-text">批量入库</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
// 任务大厅：hero + 待执行/进行中计数 + 取派双标签 + 扫码接单 + 我的任务入口
import { ref } from 'vue'
import { courierCount } from '@/common/api/courier'
import { onShow } from '@dcloudio/uni-app'

const tabs = [{ name: '取件' }, { name: '派件' }]
const tabIndex = ref(0)
const stat = ref<any>(null)
const pickupCount = ref(0)
const dispatchCount = ref(0)

function goTab(path: string) {
  uni.switchTab({ url: '/' + path })
}

// 扫码接单：解析运单号后跳详情
function onScan() {
  uni.scanCode({
    success: (res) => {
      const id = (res.result || '').trim()
      if (!id) {
        uni.showToast({ title: '未识别到运单号', icon: 'none' })
        return
      }
      uni.navigateTo({ url: '/pages/courier/task/detail?id=' + id })
    },
    fail: () => {},
  })
}

function onTab(e: any) {
  tabIndex.value = e.index
}

// 批量入库：非 tabBar 页，走 navigateTo
function goWarehousing() {
  uni.navigateTo({ url: '/pages/courier/task/warehousing' })
}

// 首页计数：taskType 1=取件 2=派件（可选，失败不阻塞）
async function loadCount() {
  try {
    const [p, d] = await Promise.all([
      courierCount({ taskType: 1 }),
      courierCount({ taskType: 2 }),
    ])
    pickupCount.value = Number(p) || 0
    dispatchCount.value = Number(d) || 0
    stat.value = true
  } catch (e) {
    stat.value = null
  }
}

onShow(() => loadCount())
</script>

<style lang="scss">
.hero {
  background: linear-gradient(135deg, #FF0033 0%, #CC0029 100%);
  margin: calc(var(--s-4) * -1) calc(var(--s-4) * -1) var(--s-4);
  padding: calc(var(--s-8) + env(safe-area-inset-top)) var(--s-6) var(--s-8);
}
.hero-bar {
  display: flex;
  justify-content: center;
  margin-bottom: var(--s-2);
}
.hero-brand {
  font-size: 34rpx;
  font-weight: 600;
  color: #fff;
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
.entry-grid {
  display: flex;
  justify-content: space-around;
}
.entry {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.entry-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: var(--r-md);
  background: var(--c-primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
}
.entry-text {
  font-size: 26rpx;
  color: var(--c-text-2);
  margin-top: var(--s-2);
}
</style>
