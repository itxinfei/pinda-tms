<template>
  <view class="page">
    <u-navbar title="运单详情" autoBack></u-navbar>
    <u-loading-page :loading="loading"></u-loading-page>

    <block v-if="!loading && order">
      <view class="card">
        <view class="detail-head">
          <text class="detail-no">{{ order.orderNumber || order.id }}</text>
          <u-tag :text="statusView(order.status).text" :type="statusView(order.status).type" />
        </view>
        <u-cell-group>
          <u-cell title="收件人" :label="maskName(order.name)" />
          <u-cell title="联系电话" :label="maskPhone(order.mobile)" />
          <u-cell title="计划取件" :label="order.planPickUpTime || '—'" />
          <u-cell title="实际取件" :label="order.actualDispathedTime || '—'" />
          <u-cell title="运单号" :label="order.tranOrderId || '—'" />
        </u-cell-group>
      </view>

      <view class="card">
        <text class="section-title">物流轨迹</text>
        <u-time-line v-if="routeList.length">
          <u-time-line-item v-for="(r, i) in routeList" :key="i">
            <template #content>
              <view class="tl-item">
                <text class="tl-msg">{{ r.msg }}</text>
                <text class="tl-time">{{ r.time || '' }}</text>
              </view>
            </template>
          </u-time-line-item>
        </u-time-line>
        <u-empty v-else mode="list" text="暂无轨迹"></u-empty>
      </view>

      <view class="action-bar" v-if="order && (order.status === 23000 || canPay)">
        <u-button
          v-if="order.status === 23000"
          text="取消订单"
          type="error"
          plain
          shape="circle"
          @click="onCancel"
        />
        <u-button
          v-if="canPay"
          text="立即支付"
          type="primary"
          shape="circle"
          @click="onPay"
        />
      </view>
    </block>

    <u-empty v-if="!loading && !order" mode="page" text="订单不存在"></u-empty>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  mailingDetail,
  mailingRoute,
  cancelMailing,
  payMailing,
} from '@/common/api/customer'
import { orderStatusView } from '@/common/constants'
import { maskName, maskPhone } from '@/common/utils/desensitive'

const order = ref<any>(null)
const routeList = ref<any[]>([])
const loading = ref(true)
const id = ref('')

function statusView(s?: number) {
  return orderStatusView(s)
}
const canPay = computed(
  () => order.value && order.value.paymentStatus !== undefined && order.value.paymentStatus === 0
)

async function load() {
  loading.value = true
  try {
    order.value = await mailingDetail(id.value)
    routeList.value = (await mailingRoute(id.value)) || []
  } catch (e) {
  } finally {
    loading.value = false
  }
}

async function onCancel() {
  uni.showModal({
    title: '提示',
    content: '仅发货前可取消并原路退款，确定取消？',
    success: async (r) => {
      if (r.confirm) {
        await cancelMailing(id.value)
        uni.showToast({ title: '已取消', icon: 'success' })
        load()
      }
    },
  })
}
async function onPay() {
  await payMailing(id.value)
  uni.showToast({ title: '支付成功', icon: 'success' })
  load()
}

onLoad((opt: any) => {
  id.value = opt.id
  load()
})
</script>

<style lang="scss">
.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--s-3);
}
.detail-no {
  font-size: var(--f-title);
  color: var(--c-text-1);
  font-weight: bold;
}
.section-title {
  display: block;
  font-size: var(--f-sub);
  color: var(--c-text-1);
  font-weight: bold;
  margin-bottom: var(--s-3);
}
.tl-item {
  display: flex;
  flex-direction: column;
}
.tl-msg {
  font-size: var(--f-body);
  color: var(--c-text-2);
}
.tl-time {
  font-size: var(--f-tip);
  color: var(--c-text-3);
  margin-top: var(--s-1);
}
</style>
