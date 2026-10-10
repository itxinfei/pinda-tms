<template>
  <view class="page">
    <view class="hero">
      <view class="hero-title">您好，{{ maskName(userInfo?.name) || '司机' }}</view>
      <view class="hero-sub">运输任务 · 实时定位</view>
    </view>

    <!-- 状态计数 -->
    <view class="card stat-card">
      <view class="stat-item" @click="goWait">
        <text class="stat-num">{{ waitCount }}</text>
        <text class="stat-label">待提货</text>
      </view>
      <view class="stat-item" @click="goOnTheWay">
        <text class="stat-num">{{ onTheWayCount }}</text>
        <text class="stat-label">在途任务</text>
      </view>
    </view>

    <!-- 定位上报开关 -->
    <view class="card location-card">
      <view class="location-left">
        <text class="location-title">定位上报</text>
        <text class="location-desc">开启后每 10 秒上报当前车辆位置</text>
      </view>
      <u-switch
        :value="reporting"
        activeColor="#2B6CFF"
        @change="toggleReport"
      ></u-switch>
    </view>

    <!-- 功能入口 -->
    <view class="card entry-grid">
      <view class="entry" @click="goWait">
        <u-icon name="list" size="44" color="#2B6CFF"></u-icon>
        <text class="entry-text">待提货</text>
      </view>
      <view class="entry" @click="goOnTheWay">
        <u-icon name="car" size="44" color="#2B6CFF"></u-icon>
        <text class="entry-text">在途</text>
      </view>
      <view class="entry" @click="goCar">
        <u-icon name="grid" size="44" color="#2B6CFF"></u-icon>
        <text class="entry-text">车辆信息</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import {
  cargoWait,
  cargoOnTheWay,
  locationPush,
} from '@/common/api/driver'
import { useUserStore } from '@/common/store/user'
import { maskName } from '@/common/utils/desensitive'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const waitCount = ref(0)
const onTheWayCount = ref(0)

// ============ 定位上报 ============
const reporting = ref(false)
let timer: any = null

// 取一次定位（WGS84，与后端存储坐标系一致）
function getLocation(): Promise<{ lng: number; lat: number }> {
  return new Promise((resolve, reject) => {
    // 修改点：坐标系固定 WGS84，禁止手填
    uni.getLocation({
      type: 'wgs84',
      success: (res) =>
        resolve({ lng: res.longitude, lat: res.latitude }),
      fail: () => reject(new Error('定位失败')),
    })
  })
}

async function pushOnce() {
  try {
    const loc = await getLocation()
    await locationPush({
      // 修改点：businessId 用司机 id 标识，后端据此关联车辆/任务
      businessId: userInfo.value?.id || '',
      name: userInfo.value?.name || '',
      phone: userInfo.value?.phone || '',
      licensePlate: userInfo.value?.licensePlate || '',
      type: 'truck',
      lng: loc.lng,
      lat: loc.lat,
      currentTime: new Date().toISOString(),
      team: userInfo.value?.team || '',
      transportTaskId: '',
      coordSystem: 'WGS84',
      source: 'MOBILE',
    })
  } catch (e) {
    // 失败已在 locationPush 内入离线队列，这里静默
  }
}

// 显式开关：开 -> 立即上报一次并起定时器；关 -> 清定时器
function toggleReport(val: boolean) {
  reporting.value = val
  if (val) {
    pushOnce()
    timer = setInterval(pushOnce, 10000)
  } else if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function goWait() {
  uni.switchTab({ url: '/pages/driver/task/wait' })
}
function goOnTheWay() {
  uni.navigateTo({ url: '/pages/driver/task/onTheWay' })
}
function goCar() {
  uni.navigateTo({ url: '/pages/driver/car/info' })
}

// ============ 计数加载 ============
async function loadCount() {
  try {
    // 修改点：待提货计数用 counts，pagesize=1 仅取计数
    const waitRes = await cargoWait({ page: 1, pagesize: 1 })
    waitCount.value = (waitRes && waitRes.counts) || 0
  } catch (e) {
    waitCount.value = 0
  }
  try {
    // 修改点：在途是单对象，存在即计 1，勿当列表/计数用
    const onRes = await cargoOnTheWay()
    onTheWayCount.value =
      onRes && typeof onRes === 'object' && Object.keys(onRes).length > 0 ? 1 : 0
  } catch (e) {
    onTheWayCount.value = 0
  }
}

onShow(() => loadCount())
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
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
.location-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.location-left {
  display: flex;
  flex-direction: column;
}
.location-title {
  font-size: var(--f-body);
  color: var(--c-text-1);
  font-weight: bold;
}
.location-desc {
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
.entry-text {
  font-size: var(--f-aux);
  color: var(--c-text-2);
  margin-top: var(--s-2);
}
</style>
