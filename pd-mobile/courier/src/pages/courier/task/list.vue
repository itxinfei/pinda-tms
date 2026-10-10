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

    <!-- 任务列表 -->
    <view class="list-wrap">
      <u-empty v-if="!loading && list.length === 0" mode="list" text="暂无任务"></u-empty>
      <view
        v-for="item in list"
        :key="item.id"
        class="card task-card"
        @click="goDetail(item.id)"
      >
        <view class="task-head">
          <text class="task-no">{{ item.orderNumber || item.id }}</text>
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

    <!-- 底部操作条：扫一扫接单 -->
    <view class="action-bar">
      <u-button type="primary" text="扫一扫接单" shape="circle" @click="onScan"></u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
// 取派任务列表：u-tabs 取件/派件双标签 + 分页 + 状态/类型 tag + 收寄件人脱敏
import { ref } from 'vue'
import { pickupDispatchPage } from '@/common/api/courier'
import { taskStatusView, taskTypeView } from '@/common/constants'
import { maskName } from '@/common/utils/desensitive'
import { onShow, onReachBottom } from '@dcloudio/uni-app'

const tabs = [{ name: '取件' }, { name: '派件' }]
const tabIndex = ref(0)
const list = ref<any[]>([])
const loading = ref(false)
const loadStatus = ref('loadmore')
const page = ref(1)
const pageSize = 10
const total = ref(0)

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

function onTab(e: any) {
  tabIndex.value = e.index
  loadData()
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
</script>

<style lang="scss">
.list-wrap {
  margin-top: var(--s-3);
  padding-bottom: calc(var(--s-8) * 2.5);
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
