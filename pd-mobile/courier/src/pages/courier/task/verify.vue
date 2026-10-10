<template>
  <view class="page">
    <view class="card">
      <view class="order-no">运单号：{{ orderNumber }}</view>
      <u-gap height="16"></u-gap>
      <u-form :model="form" ref="uForm">
        <u-form-item label="身份证号" borderBottom>
          <u--input v-model="form.code" placeholder="请输入收件人身份证号" clearable />
        </u-form-item>
      </u-form>
    </view>

    <!-- 核验结果：仅回显通过/不通过，不展示/缓存身份证号 -->
    <view class="card result-card" v-if="verified">
      <u-icon
        :name="pass ? 'checkmark-circle' : 'close-circle'"
        :color="pass ? 'var(--c-success)' : 'var(--c-error)'"
        size="48"
      ></u-icon>
      <text class="result-text" :class="pass ? 'pass' : 'fail'">
        {{ pass ? '核验通过' : '核验不通过' }}
      </text>
    </view>

    <view class="action-bar">
      <u-button
        type="primary"
        text="核验"
        shape="circle"
        :loading="loading"
        @click="onVerify"
      ></u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
// 身份证核验：接 orderNumber，输入身份证号 code，调 verifyIdCard，仅回显通过/不通过
// 红线：禁止本地缓存或日志打印身份证号，故不输出、不存储原始身份证号
import { ref, reactive } from 'vue'
import { verifyIdCard } from '@/common/api/courier'

const orderNumber = ref('')
const loading = ref(false)
const verified = ref(false)
const pass = ref(false)
const form = reactive({ code: '' })

onLoad((options: any) => {
  orderNumber.value = options?.orderNumber || ''
})

async function onVerify() {
  if (!orderNumber.value || !form.code) {
    uni.showToast({ title: '请填写运单号与身份证号', icon: 'none' })
    return
  }
  if (loading.value) return
  loading.value = true
  try {
    const res = await verifyIdCard({ orderNumber: orderNumber.value, code: form.code })
    // 仅根据返回判定通过与否，不记录身份证号原文
    pass.value = !!(res && (res.pass || res.success || res.verified))
    verified.value = true
  } catch (e) {
    // 接口报错也视为不通过，但保留用户重新输入
    pass.value = false
    verified.value = true
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss">
.order-no {
  font-size: var(--f-body);
  color: var(--c-text-1);
  font-weight: bold;
}
.result-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--s-8) var(--s-4);
}
.result-text {
  font-size: var(--f-sub);
  margin-top: var(--s-2);
  font-weight: bold;
}
.result-text.pass {
  color: var(--c-success);
}
.result-text.fail {
  color: var(--c-error);
}
</style>
