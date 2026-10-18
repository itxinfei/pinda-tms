<template>
  <view class="page">
    <!-- 取件 / 派件 双标签：taskType 1=取件 2=派件 -->
    <u-tabs
      :list="tabs"
      :current="tabIndex"
      @click="onTab"
      lineColor="var(--c-primary)"
      :activeStyle="{ color: 'var(--c-primary)' }"
      :inactiveStyle="{ color: 'var(--c-text-2)' }"
    ></u-tabs>

    <!-- 状态筛选（status 走后端契约，非硬编码过滤，《70》§3.3②） -->
    <view class="status-bar">
      <view
        v-for="s in statusOptions"
        :key="s.label"
        class="status-chip"
        :class="{ active: statusFilter === s.value }"
        @click="onStatus(s.value)"
      >{{ s.label }}</view>
    </view>

    <!-- 任务列表 -->
    <view class="list-wrap">
      <u-skeleton v-if="loading && list.length === 0" :rows="4" title animated />
      <u-empty v-else-if="list.length === 0" mode="list" text="暂无任务"></u-empty>
      <view
        v-for="item in list"
        :key="item.id"
        class="card task-card"
        :class="{ picked: pickMode && isPicked(item.id) }"
        hover-class="card-hover"
        @click="onCardClick(item)"
      >
        <view class="task-head">
          <view class="task-no-wrap">
            <view v-if="pickMode" class="pick-dot" :class="{ on: isPicked(item.id) }"></view>
            <text class="task-no">{{ item.orderNumber || item.id }}</text>
          </view>
          <u-tag :text="statusView(item.status).text" :type="statusView(item.status).type" size="mini" />
        </view>
        <view class="task-type">
          <u-tag :text="typeView(item.taskType).text" type="primary" plain size="mini" />
        </view>
        <view class="task-body">
          <text class="task-line">寄件人：{{ maskName(item.sender) }}</text>
          <text class="task-line">收件人：{{ maskName(item.receiver) }}</text>
          <text class="task-line">寄件地址：{{ item.senderAddress || '—' }}</text>
          <text class="task-line">收件地址：{{ item.receiverAddress || '—' }}</text>
        </view>
      </view>
      <u-loadmore v-if="list.length > 0" :status="loadStatus"></u-loadmore>
    </view>

    <!-- 底部操作条：扫一扫接单 / 批量操作占位（依赖后端批量契约，先 UI 壳） -->
    <view class="action-bar bar-fixed">
      <template v-if="!pickMode">
        <u-button type="primary" text="扫一扫接单" shape="circle" class="bar-btn" @click="onScan"></u-button>
        <u-button type="primary" plain text="批量操作" shape="circle" class="bar-btn" @click="enterPick"></u-button>
      </template>
      <template v-else>
        <u-button type="info" plain text="退出选择" shape="circle" class="bar-btn" @click="exitPick"></u-button>
        <u-button
          type="primary"
          text="批量完成（已选 {{ picked.size }}）"
          shape="circle"
          class="bar-btn"
          :disabled="picked.size === 0"
          @click="onBatchDone"
        ></u-button>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
// 取派任务列表：u-tabs 取件/派件双标签 + 分页 + 状态/类型 tag + 收寄件人脱敏
import { ref } from 'vue'
import { pickupDispatchPage } from '@/common/api/courier'
import { taskStatusView, taskTypeView } from '@/common/constants'
import { maskName } from '@/common/utils/desensitive'
import { onShow, onReachBottom, onPullDownRefresh } from '@dcloudio/uni-app'

const tabs = [{ name: '取件' }, { name: '派件' }]
const tabIndex = ref(0)
const list = ref<any[]>([])
const loading = ref(false)
const loadStatus = ref('loadmore')
const page = ref(1)
const pageSize = 10
const total = ref(0)
// 状态筛选（null=全部，走后端 status 参数）
const statusFilter = ref<number | null>(null)
const statusOptions = [
  { label: '全部', value: null as number | null },
  { label: '待执行', value: 1 },
  { label: '执行中', value: 2 },
  { label: '待确认', value: 3 },
  { label: '已完成', value: 4 },
]
// 批量选择（UI 占位，提交待后端契约）
const pickMode = ref(false)
const picked = ref<Set<string>>(new Set())

function statusView(s?: number) {
  return taskStatusView(s)
}
function typeView(t?: number) {
  return taskTypeView(t)
}

// 分页加载：pagesize 全小写，读取 res.items / res.counts
async function loadData(refresh = true) {
  if (refresh) {
    page.value = 1
    list.value = []
  }
  loading.value = true
  try {
    const res = await pickupDispatchPage({
      page: page.value,
      pagesize: pageSize,
      taskType: tabIndex.value + 1, // 1 取件 / 2 派件
      status: statusFilter.value ?? undefined,
    })
    const items = (res && res.items) || []
    list.value = refresh ? items : list.value.concat(items)
    total.value = (res && res.counts) || 0
    loadStatus.value = list.value.length >= total.value ? 'nomore' : 'loadmore'
  } catch (e) {
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

function onTab(e: any) {
  tabIndex.value = e.index
  statusFilter.value = null
  exitPick()
  loadData()
}

function onStatus(v: number | null) {
  statusFilter.value = v
  loadData()
}

function isPicked(id: string) {
  return picked.value.has(String(id))
}

function enterPick() {
  pickMode.value = true
  picked.value = new Set()
}

function exitPick() {
  pickMode.value = false
  picked.value = new Set()
}

function onCardClick(item: any) {
  if (!pickMode.value) {
    goDetail(item.id)
    return
  }
  const next = new Set(picked.value)
  const key = String(item.id)
  if (next.has(key)) next.delete(key)
  else next.add(key)
  picked.value = next
}

function onBatchDone() {
  // 后端批量完成契约未开放，《70》§3.3 先 UI 占位
  uni.showToast({ title: '批量完成待后端契约开放', icon: 'none' })
}

function goDetail(id: string) {
  uni.navigateTo({ url: '/pages/courier/task/detail?id=' + id })
}

// 扫一扫接单：解析运单号跳详情
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

// 上拉加载更多：注册 onReachBottom 页面生命周期
onReachBottom(() => {
  if (list.value.length < total.value) {
    page.value += 1
    loadData(false)
  }
})

onShow(() => loadData())
onPullDownRefresh(() => loadData())
</script>

<style lang="scss">
.list-wrap {
  margin-top: var(--s-3);
  padding-bottom: calc(var(--s-8) * 2.5);
}
.status-bar {
  display: flex;
  flex-wrap: wrap;
  gap: var(--s-2);
  padding: var(--s-3) var(--s-4) 0;
}
.status-chip {
  padding: 6rpx 20rpx;
  border-radius: 999rpx;
  background: var(--c-surface);
  color: var(--c-text-2);
  font-size: var(--f-tip);
}
.status-chip.active {
  background: var(--c-primary);
  color: #fff;
}
.task-card {
  padding: var(--s-4);
}
.task-card.picked {
  background-color: rgba(255, 0, 51, 0.04);
  border-color: var(--c-primary);
}
.task-no-wrap {
  display: flex;
  align-items: center;
  gap: var(--s-2);
}
.pick-dot {
  width: 32rpx;
  height: 32rpx;
  border-radius: 50%;
  border: 2rpx solid var(--c-border);
  box-sizing: border-box;
  flex-shrink: 0;
}
.pick-dot.on {
  background: var(--c-primary);
  border-color: var(--c-primary);
}
.bar-fixed {
  display: flex;
  gap: var(--s-3);
}
.bar-btn {
  flex: 1;
}
.card-hover {
  background-color: rgba(255, 0, 51, 0.04);
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
.task-type {
  margin-top: var(--s-2);
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
