<template>
  <view class="page">
    <view class="card">
      <text class="title">入库确认</text>
      <text class="desc">确认后将运单标记为已入库，请核对运单信息无误。</text>
      <view class="order-no">运单号：{{ tranOrderId }}</view>
    </view>

    <view class="action-bar">
      <u-button
        type="primary"
        text="确认入库"
        shape="circle"
        :loading="loading"
        @click="onConfirm"
      ></u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
// 入库：接 tranOrderId，确认调 warehousing，成功后 toast 并返回
import { ref } from 'vue'
import { warehousing } from '@/common/api/courier'

const tranOrderId = ref('')
const loading = ref(false)

onLoad((options: any) => {
  tranOrderId.value = options?.tranOrderId || ''
})

async function onConfirm() {
  if (!tranOrderId.value) {
    uni.showToast({ title: '缺少运单标识', icon: 'none' })
    return
  }
  if (loading.value) return
  loading.value = true
  try {
    await warehousing(tranOrderId.value)
    uni.showToast({ title: '入库成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (e) {
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss">
.title {
  display: block;
  font-size: var(--f-sub);
  color: var(--c-text-1);
  font-weight: bold;
}
.desc {
  display: block;
  font-size: var(--f-aux);
  color: var(--c-text-2);
  margin-top: var(--s-2);
}
.order-no {
  display: block;
  font-size: var(--f-body);
  color: var(--c-text-1);
  margin-top: var(--s-3);
  font-weight: bold;
}
</style>
