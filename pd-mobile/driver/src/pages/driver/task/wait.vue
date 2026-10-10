<template>
  <view class="page">
    <view class="list-wrap">
      <u-empty v-if="!loading && list.length === 0" mode="list" text="暂无待提货任务"></u-empty>
      <view
        v-for="item in list"
        :key="item.id"
        class="card order-card"
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
          <text class="order-line">
            运单号：{{ item.tranOrderNum || '—' }}
          </text>
          <text class="order-line">
            起：{{ (item.startAgency && item.startAgency.name) || '—' }} → 终：{{
              (item.endAgency && item.endAgency.name) || '—'
            }}
          </text>
        </view>
        <view class="order-foot">
          <!-- 修改点：disable 标识是否可提货，置灰提示 -->
          <text class="order-tip" :class="{ off: item.disable }">
            {{ item.disable ? '暂不可提货' : '可提货' }}
          </text>
        </view>
      </view>
      <u-loadmore v-if="list.length > 0" :status="loadStatus"></u-loadmore>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { cargoWait } from '@/common/api/driver'
import { driverJobStatusView } from '@/common/constants'
import { onShow, onReachBottom, onPullDownRefresh } from '@dcloudio/uni-app'

const list = ref<any[]>([])
const loading = ref(false)
const loadStatus = ref('loadmore')
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
  try {
    // 修改点：分页 pagesize 全小写，读 res.items / res.counts
    const res = await cargoWait({ page: page.value, pagesize: pageSize })
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

function goDetail(id: string) {
  // 修改点：id 为 DriverJobId
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
.list-wrap {
  margin-top: var(--s-3);
}
.order-card {
  padding: var(--s-4);
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
.order-foot {
  margin-top: var(--s-3);
}
.order-tip {
  font-size: var(--f-tip);
  color: var(--c-success);
}
.order-tip.off {
  color: var(--c-text-3);
}
</style>
