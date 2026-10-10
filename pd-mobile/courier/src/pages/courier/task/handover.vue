<template>
  <view class="page">
    <view class="card">
      <u-form :model="form" ref="uForm">
        <u-form-item label="交接网点" borderBottom>
          <u--input v-model="form.agency" placeholder="请输入交接网点名称" clearable />
        </u-form-item>
        <u-form-item label="交接人" borderBottom>
          <u--input v-model="form.person" placeholder="请输入交接人（选填）" clearable />
        </u-form-item>
      </u-form>
      <view class="order-no">运单号：{{ tranOrderId }}</view>
    </view>

    <view class="action-bar">
      <u-button
        type="primary"
        text="确认交接"
        shape="circle"
        :loading="loading"
        @click="onConfirm"
      ></u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
// 交接：接 tranOrderId，选填交接网点/交接人，确认调 handover
import { ref, reactive } from 'vue'
import { handover } from '@/common/api/courier'
import { onLoad } from '@dcloudio/uni-app'

const tranOrderId = ref('')
const loading = ref(false)
const form = reactive({ agency: '', person: '' })

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
    // 交接网点为选填，前端仅透传备注，核心接口只认 tranOrderId
    await handover(tranOrderId.value)
    uni.showToast({ title: '交接成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (e) {
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss">
.order-no {
  display: block;
  font-size: var(--f-body);
  color: var(--c-text-1);
  margin-top: var(--s-3);
  font-weight: bold;
}
</style>
