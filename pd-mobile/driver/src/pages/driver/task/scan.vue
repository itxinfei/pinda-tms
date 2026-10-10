<template>
  <view class="page">
    <view class="card">
      <u-button
        type="primary"
        text="扫车次 / 线路码接单"
        shape="circle"
        @click="onScan"
      ></u-button>
      <u-gap height="20"></u-gap>
      <!-- 修改点：扫码结果展示，业务逻辑待后端开通，仅提示 -->
      <u-empty v-if="!scanResult" mode="scan" text="点击上方按钮扫码接单"></u-empty>
      <view v-else class="scan-result">
        <text class="sr-label">扫码内容</text>
        <text class="sr-value">{{ scanResult }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const scanResult = ref('')

// 扫码接单占位：解析二维码后提示功能待后端开通，不强制实现业务逻辑
function onScan() {
  // 修改点：仅调用系统扫码，解析后提示，不接业务
  uni.scanCode({
    success: (res) => {
      scanResult.value = res.result || ''
      uni.showToast({ title: '功能待后端开通', icon: 'none' })
    },
    fail: () => {
      uni.showToast({ title: '已取消扫码', icon: 'none' })
    },
  })
}
</script>

<style lang="scss">
.scan-result {
  display: flex;
  flex-direction: column;
  padding: var(--s-3);
  background: var(--c-primary-light);
  border-radius: var(--r-md);
}
.sr-label {
  font-size: var(--f-tip);
  color: var(--c-text-3);
}
.sr-value {
  font-size: var(--f-body);
  color: var(--c-text-1);
  margin-top: var(--s-1);
  word-break: break-all;
}
</style>
