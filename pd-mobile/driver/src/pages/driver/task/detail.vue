<template>
  <view class="page">
    <u-empty v-if="!loading && !task" mode="list" text="未获取到任务"></u-empty>

    <block v-if="task">
      <!-- 任务信息 -->
      <view class="card">
        <view class="card-title">任务信息</view>
        <view class="row">
          <text class="label">任务单号</text>
          <text class="value">{{ task.taskNo || task.id }}</text>
        </view>
        <view class="row">
          <text class="label">状态</text>
          <u-tag
            :text="jobStatusView(task.status).text"
            :type="jobStatusView(task.status).type"
            size="mini"
          />
        </view>
        <view class="row">
          <text class="label">运单号</text>
          <text class="value">{{ task.tranOrderNum || '—' }}</text>
        </view>
        <view class="row">
          <text class="label">起始网点</text>
          <text class="value">{{ (task.startAgency && task.startAgency.name) || '—' }}</text>
        </view>
        <view class="row">
          <text class="label">目的网点</text>
          <text class="value">{{ (task.endAgency && task.endAgency.name) || '—' }}</text>
        </view>
      </view>

      <!-- 关联订单 -->
      <view class="card">
        <view class="card-title">关联订单（{{ orders.length }}）</view>
        <u-empty v-if="orders.length === 0" mode="list" text="暂无关联订单"></u-empty>
        <view
          v-for="o in orders"
          :key="o.id"
          class="order-item"
        >
          <text class="order-no">{{ o.orderNumber || o.id }}</text>
          <u-tag
            :text="orderStatusView(o.status).text"
            :type="orderStatusView(o.status).type"
            size="mini"
          />
        </view>
      </view>

      <!-- 最新位置 -->
      <view class="card">
        <view class="card-title">最新位置</view>
        <text class="loc-text" v-if="latest">{{ latest.address || (latest.lng + ',' + latest.lat) }}</text>
        <u-empty v-else mode="address" text="暂无可追踪位置"></u-empty>
      </view>
    </block>

    <!-- 操作按钮 -->
    <view class="action-bar" v-if="task">
      <u-button
        type="primary"
        text="提货"
        shape="circle"
        :disabled="task.disable"
        @click="goPickup"
      ></u-button>
      <u-button
        type="success"
        text="送达"
        shape="circle"
        @click="goDeliver"
      ></u-button>
      <u-button
        type="warning"
        text="轨迹"
        shape="circle"
        plain
        @click="goTrack"
      ></u-button>
      <u-button
        type="error"
        text="异常"
        shape="circle"
        plain
        @click="goException"
      ></u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import {
  cargoDetail,
  cargoOrders,
  traceLatest,
} from '@/common/api/driver'
import { useUserStore } from '@/common/store/user'
import { driverJobStatusView, orderStatusView } from '@/common/constants'

const userStore = useUserStore()
const id = ref('')
const task = ref<any>(null)
const orders = ref<any[]>([])
const latest = ref<any>(null)
const loading = ref(false)

function jobStatusView(s?: number) {
  return driverJobStatusView(s)
}

// 修改点：truckId 从容错取——优先任务 truckId，回退到当前用户 id
function truckId(): string {
  return (task.value && task.value.truckId) || userStore.userInfo?.id || ''
}

async function loadAll() {
  if (!id.value) return
  loading.value = true
  try {
    // 修改点：id 为 DriverJobId
    task.value = await cargoDetail(id.value)
  } catch (e) {
    task.value = null
  }
  try {
    const res = await cargoOrders(id.value)
    // 容错：接口可能返回列表或 {items}
    if (Array.isArray(res)) orders.value = res
    else orders.value = (res && res.items) || []
  } catch (e) {
    orders.value = []
  }
  // 最新位置（免鉴权，按 truck 类型）
  try {
    latest.value = await traceLatest(truckId(), 'truck')
  } catch (e) {
    latest.value = null
  }
  loading.value = false
}

function goPickup() {
  // 修改点：传 taskTransportId（实为 DriverJobId），页面内容错
  uni.navigateTo({ url: '/pages/driver/task/pickup?taskTransportId=' + id.value + '&id=' + id.value })
}
function goDeliver() {
  uni.navigateTo({ url: '/pages/driver/task/deliver?taskTransportId=' + id.value + '&id=' + id.value })
}
function goTrack() {
  uni.navigateTo({ url: '/pages/driver/task/track?truckId=' + truckId() })
}
function goException() {
  uni.navigateTo({
    url:
      '/pages/driver/exception/report?id=' +
      id.value +
      '&transportTaskId=' +
      (task.value?.transportTaskId || ''),
  })
}

onLoad((opt: any) => {
  id.value = opt.id || ''
  loadAll()
})
</script>

<style lang="scss">
.card-title {
  font-size: var(--f-sub);
  color: var(--c-text-1);
  font-weight: bold;
  margin-bottom: var(--s-3);
}
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--s-2) 0;
}
.label {
  font-size: var(--f-body);
  color: var(--c-text-3);
}
.value {
  font-size: var(--f-body);
  color: var(--c-text-1);
}
.order-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--s-2) 0;
  border-top: 1rpx solid var(--c-border);
}
.order-no {
  font-size: var(--f-aux);
  color: var(--c-text-2);
}
.loc-text {
  font-size: var(--f-body);
  color: var(--c-text-1);
}
</style>
