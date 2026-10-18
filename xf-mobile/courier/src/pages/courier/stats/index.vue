<template>
  <view class="page">
    <!-- 日期选择：后端按 actual_end_time 当日过滤（yyyy-MM-dd） -->
    <view class="card date-card">
      <text class="date-label">统计日期</text>
      <picker mode="date" :value="date" :end="today" @change="onDateChange">
        <view class="date-cell">
          <text class="date-value">{{ date }}</text>
          <u-icon name="calendar" color="var(--c-text-3)" size="18" />
        </view>
      </picker>
    </view>

    <!-- 完成量统计：取件 / 派件 / 合计 -->
    <view class="card stat-card">
      <view class="stat-item">
        <text class="stat-num">{{ pickupCount }}</text>
        <text class="stat-label">取件完成</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ dispatchCount }}</text>
        <text class="stat-label">派件完成</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ totalCount }}</text>
        <text class="stat-label">合计</text>
      </view>
    </view>

    <!-- 当日已完成任务明细 -->
    <view class="section-title">当日已完成任务</view>
    <view class="list-wrap">
      <u-empty v-if="!loading && list.length === 0" mode="list" text="当日暂无已完成任务"></u-empty>
      <view
        v-for="item in list"
        :key="item.id"
        class="card task-card"
        @click="goDetail(item.id)"
      >
        <view class="task-head">
          <text class="task-no">{{ item.orderNumber || item.id }}</text>
          <u-tag :text="typeView(item.taskType).text" type="primary" plain size="mini" />
        </view>
        <view class="task-body">
          <text class="task-line">寄件人：{{ maskName(item.sender) }}</text>
          <text class="task-line">收件人：{{ maskName(item.receiver) }}</text>
        </view>
      </view>
      <u-loadmore v-if="list.length > 0" :status="loadStatus"></u-loadmore>
    </view>
  </view>
</template>

<script setup lang="ts">
// DIS-06 快递员绩效统计：复用 courierCount / pickupDispatchPage（status=已完成 + date 过滤）
import { ref, computed } from 'vue'
import { courierCount, pickupDispatchPage } from '@/common/api/courier'
import { taskTypeView, TASK_TYPE_PICKUP, TASK_TYPE_DISPATCH, TASK_STATUS_COMPLETED } from '@/common/constants'
import { maskName } from '@/common/utils/desensitive'
import { onShow, onReachBottom } from '@dcloudio/uni-app'

function fmtDate(d: Date): string {
  const m = d.getMonth() + 1
  const day = d.getDate()
  return `${d.getFullYear()}-${m < 10 ? '0' + m : m}-${day < 10 ? '0' + day : day}`
}

const today = fmtDate(new Date())
const date = ref(today)
const pickupCount = ref(0)
const dispatchCount = ref(0)
const list = ref<any[]>([])
const loading = ref(false)
const loadStatus = ref('loadmore')
const page = ref(1)
const pageSize = 10
const total = ref(0)

const totalCount = computed(() => pickupCount.value + dispatchCount.value)

function typeView(t?: number) {
  return taskTypeView(t)
}

// 三个完成量计数并行取（后端按 token 隔离到当前快递员）
async function loadStats() {
  const params = { status: TASK_STATUS_COMPLETED, date: date.value }
  const [pickupRes, dispatchRes] = await Promise.all([
    courierCount({ ...params, taskType: TASK_TYPE_PICKUP }).catch(() => null),
    courierCount({ ...params, taskType: TASK_TYPE_DISPATCH }).catch(() => null),
  ])
  pickupCount.value = (pickupRes && pickupRes.count) || 0
  dispatchCount.value = (dispatchRes && dispatchRes.count) || 0
}

// 明细分页：pagesize 全小写，读 res.items / res.counts
async function loadList(refresh = true) {
  if (refresh) {
    page.value = 1
    list.value = []
  }
  loading.value = true
  try {
    const res = await pickupDispatchPage({
      page: page.value,
      pagesize: pageSize,
      status: TASK_STATUS_COMPLETED,
      date: date.value,
    })
    const items = (res && res.items) || []
    list.value = refresh ? items : list.value.concat(items)
    total.value = (res && res.counts) || 0
    loadStatus.value = list.value.length >= total.value ? 'nomore' : 'loadmore'
  } catch (e) {
  } finally {
    loading.value = false
  }
}

function onDateChange(e: any) {
  date.value = (e.detail && e.detail.value) || today
  loadStats()
  loadList()
}

function goDetail(id: string) {
  uni.navigateTo({ url: '/pages/courier/task/detail?id=' + id })
}

onShow(() => {
  loadStats()
  loadList()
})
onReachBottom(() => {
  if (list.value.length < total.value) {
    page.value += 1
    loadList(false)
  }
})
</script>

<style lang="scss">
.date-card {
  display: flex;
  align-items: center;
  padding: var(--s-4);
}
.date-label {
  font-size: var(--f-body);
  color: var(--c-text-2);
  margin-right: var(--s-4);
}
.date-cell {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--s-2) var(--s-3);
  background: var(--c-bg);
  border-radius: var(--r-sm);
  min-width: 320rpx;
}
.date-value {
  font-size: var(--f-body);
  color: var(--c-text-1);
}
.stat-card {
  display: flex;
  padding: var(--s-4) 0;
}
.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stat-num {
  font-size: 48rpx;
  font-weight: 700;
  color: var(--c-primary);
}
.stat-label {
  margin-top: var(--s-1);
  font-size: var(--f-tip);
  color: var(--c-text-3);
}
.section-title {
  margin: var(--s-4) var(--s-4) 0;
  font-size: var(--f-sub);
  font-weight: 600;
  color: var(--c-text-1);
}
.list-wrap {
  margin-top: var(--s-3);
}
.task-card {
  padding: var(--s-4);
}
.task-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.task-no {
  font-size: var(--f-body);
  color: var(--c-text-1);
  font-weight: bold;
}
.task-body {
  margin-top: var(--s-2);
}
.task-line {
  display: block;
  font-size: var(--f-aux);
  color: var(--c-text-2);
  margin-top: var(--s-1);
}
</style>
