<template>
  <view class="page">
    <!-- 顶部 hero：欢迎 + 角色标识 -->
    <view class="hero">
      <view class="hero-title">快递员工作台</view>
      <view class="hero-sub">取件派件 · 扫码接单 · 实时追踪</view>
    </view>

    <!-- 待执行 / 进行中 计数（容错，失败不阻塞） -->
    <view class="card stat-card" v-if="stat">
      <view class="stat-item">
        <text class="stat-num">{{ pending }}</text>
        <text class="stat-label">待执行</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ processing }}</text>
        <text class="stat-label">进行中</text>
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

    <!-- 扫码接单入口 -->
    <view class="card scan-card" @click="onScan">
      <u-icon name="scan" size="44" color="var(--c-primary)"></u-icon>
      <text class="scan-text">扫码接单</text>
      <text class="scan-tip">扫描运单条码快速接单</text>
    </view>

    <u-gap height="20"></u-gap>
    <u-button
      type="primary"
      text="我的任务"
      shape="circle"
      @click="goTab('pages/courier/task/list')"
    ></u-button>
  </view>
</template>

<script setup lang="ts">
// 任务大厅：hero + 待执行/进行中计数 + 取派双标签 + 扫码接单 + 我的任务入口
import { ref, computed } from 'vue'
import { courierCount } from '@/common/api/courier'
import { onShow } from '@dcloudio/uni-app'

const tabs = [{ name: '取件' }, { name: '派件' }]
const tabIndex = ref(0)
const stat = ref<any>(null)
// 计数容错：字段名不确定，统一 ?.xxx ?? 0
const pending = computed(() => stat.value?.pending ?? stat.value?.waitExecute ?? stat.value?.todo ?? 0)
const processing = computed(() => stat.value?.processing ?? stat.value?.progress ?? stat.value?.doing ?? 0)

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

// 首页计数（可选，失败不阻塞）
async function loadCount() {
  try {
    stat.value = await courierCount({})
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
.scan-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--s-8) var(--s-4);
}
.scan-text {
  font-size: var(--f-sub);
  color: var(--c-text-1);
  font-weight: bold;
  margin-top: var(--s-2);
}
.scan-tip {
  font-size: var(--f-tip);
  color: var(--c-text-3);
  margin-top: var(--s-1);
}
</style>
