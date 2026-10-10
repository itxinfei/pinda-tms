<template>
  <view class="page">
    <u-tabs
      :list="tabs"
      :current="tabIndex"
      @click="onTab"
      lineColor="#2B6CFF"
      :activeStyle="{ color: '#2B6CFF' }"
      :inactiveStyle="{ color: '#4E5969' }"
    ></u-tabs>

    <view class="list-wrap">
      <u-empty v-if="!loading && list.length === 0" mode="list" text="暂无运单"></u-empty>
      <view
        v-for="item in list"
        :key="item.id"
        class="card order-card"
        @click="goDetail(item.id)"
      >
        <view class="order-head">
          <text class="order-no">{{ item.orderNumber || item.id }}</text>
          <u-tag
            :text="statusView(item.status).text"
            :type="statusView(item.status).type"
            size="mini"
          />
        </view>
        <view class="order-body">
          <text class="order-line">
            收件人：{{ maskName(item.receiverName || item.name) }}
            {{ maskPhone(item.receiverPhone || item.mobile) }}
          </text>
          <text class="order-line">目的地：{{ item.receiverAddress || item.address || '—' }}</text>
        </view>
        <view class="order-foot">
          <text class="order-time">{{ item.createTime || '' }}</text>
          <text class="order-amount" v-if="item.amount">¥{{ item.amount }}</text>
        </view>
      </view>
      <u-loadmore v-if="list.length > 0" :status="loadStatus"></u-loadmore>
    </view>

    <view class="action-bar">
      <u-button type="primary" text="去寄件" shape="circle" @click="goCreate"></u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { mailingPage } from '@/common/api/customer'
import { orderStatusView } from '@/common/constants'
import { maskName, maskPhone } from '@/common/utils/desensitive'
import { onShow } from '@dcloudio/uni-app'

const tabs = [{ name: '全部' }, { name: '寄出' }, { name: '寄入' }]
const tabIndex = ref(0)
const list = ref<any[]>([])
const loading = ref(false)
const loadStatus = ref('loadmore')
const page = ref(1)
const pageSize = 10
const total = ref(0)

function statusView(s?: number) {
  return orderStatusView(s)
}

async function loadData(refresh = true) {
  if (refresh) {
    page.value = 1
    list.value = []
  }
  loading.value = true
  try {
    const res = await mailingPage({
      page: page.value,
      pagesize: pageSize,
      mailType: tabIndex.value === 0 ? '' : tabIndex.value === 1 ? 0 : 1,
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
  uni.navigateTo({ url: '/pages/customer/mailing/detail?id=' + id })
}
function goCreate() {
  uni.navigateTo({ url: '/pages/customer/mailing/create' })
}

onShow(() => loadData())
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--s-3);
}
.order-time {
  font-size: var(--f-tip);
  color: var(--c-text-3);
}
.order-amount {
  font-size: var(--f-body);
  color: var(--c-error);
  font-weight: bold;
}
</style>
