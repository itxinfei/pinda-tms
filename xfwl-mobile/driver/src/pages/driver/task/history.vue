<template>
  <view class="page">
    <!-- 搜索：后端按纯数字作业单 id 筛选，非数字按无 id 处理（D-01 口径） -->
    <view class="search-bar">
      <u-search
        v-model="keyword"
        placeholder="输入任务号搜索"
        :showAction="true"
        actionText="搜索"
        @search="loadData"
        @custom="loadData"
        @clear="loadData"
      ></u-search>
    </view>

    <view class="list-wrap">
      <u-skeleton v-if="loading && list.length === 0" :rows="4" title animated />
      <view v-else-if="error" class="state-wrap">
        <u-empty mode="network" text="加载失败，请检查网络后重试"></u-empty>
        <view class="retry-wrap">
          <u-button type="primary" size="small" shape="circle" text="重试" @click="loadData()"></u-button>
        </view>
      </view>
      <u-empty v-else-if="list.length === 0" mode="list" text="暂无历史任务"></u-empty>
      <view
        v-for="item in list"
        :key="item.id"
        class="card order-card"
        hover-class="card-hover"
        @click="goDetail(item.id)"
      >
        <view class="order-head">
          <text class="order-no">{{ item.taskNo || item.id }}</text>
          <u-tag
            :text="statusView(item.status).text"
            :type="statusView(item.status).type"
            size="mini"
          />
        </view>
        <view class="order-body">
          <text class="order-line">完成时间：{{ item.actualArrivalTime || '—' }}</text>
        </view>
      </view>
      <u-loadmore v-if="list.length > 0" :status="loadStatus"></u-loadmore>
    </view>
  </view>
</template>

<script setup lang="ts">
// DRV-05 历史任务：复用 cargoHistory（后端已固定 status=COMPLETED 且按当前司机隔离）
import { ref } from 'vue'
import { cargoHistory } from '@/common/api/driver'
import { driverJobStatusView } from '@/common/constants'
import { onShow, onReachBottom, onPullDownRefresh } from '@dcloudio/uni-app'

const list = ref<any[]>([])
const loading = ref(false)
const error = ref(false)
const loadStatus = ref('loadmore')
const keyword = ref('')
const page = ref(1)
const pageSize = 10
const total = ref(0)

function statusView(s?: number) {
  return driverJobStatusView(s)
}

async function loadData(refresh = true) {
  if (refresh) {
    page.value = 1
    list.value = []
  }
  loading.value = true
  error.value = false
  try {
    // 分页 pagesize 全小写，读 res.items / res.counts
    const res = await cargoHistory({
      page: page.value,
      pagesize: pageSize,
      keyword: keyword.value || undefined,
    })
    const items = (res && res.items) || []
    list.value = refresh ? items : list.value.concat(items)
    total.value = (res && res.counts) || 0
    loadStatus.value = list.value.length >= total.value ? 'nomore' : 'loadmore'
  } catch (e) {
    if (list.value.length === 0) error.value = true
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

function goDetail(id: string) {
  // id 为 DriverJobId，与待提货页同一详情页
  uni.navigateTo({ url: '/pages/driver/task/detail?id=' + id })
}

onShow(() => loadData())
onPullDownRefresh(() => loadData())
onReachBottom(() => {
  if (list.value.length < total.value) {
    page.value += 1
    loadData(false)
  }
})
</script>

<style lang="scss">
.search-bar {
  padding: var(--s-3) var(--s-4) 0;
}
.list-wrap {
  margin-top: var(--s-3);
}
.state-wrap {
  padding-top: 120rpx;
}
.retry-wrap {
  display: flex;
  justify-content: center;
  padding: var(--s-4) 0;
}
.order-card {
  padding: var(--s-4);
}
.card-hover {
  background-color: rgba(255, 0, 51, 0.04);
}
.order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.order-no {
  font-size: var(--f-body);
  color: var(--c-text-1);
  font-weight: bold;
}
.order-body {
  margin-top: var(--s-2);
}
.order-line {
  display: block;
  font-size: var(--f-aux);
  color: var(--c-text-2);
  margin-top: var(--s-1);
}
</style>
