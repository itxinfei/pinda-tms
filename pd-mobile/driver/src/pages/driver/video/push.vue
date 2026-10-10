<template>
  <view class="page">
    <view class="card placeholder">
      <u-icon name="video" size="72" color="#C9CDD4"></u-icon>
      <text class="ph-title">车载视频推流</text>
      <text class="ph-desc">后端推流服务待建设，本入口暂未开放</text>
      <!-- 修改点：后端待建，按钮置灰 disabled，不接前端 RTC -->
      <u-button
        type="primary"
        text="开始推流"
        shape="circle"
        :disabled="true"
        customStyle="margin-top: 24rpx;"
      ></u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
// 视频推流占位页：后端服务尚未提供，仅占位，不实现前端 RTC 逻辑。
</script>

<style lang="scss">
.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--s-8) var(--s-4);
}
.ph-title {
  font-size: var(--f-title);
  color: var(--c-text-1);
  font-weight: bold;
  margin-top: var(--s-4);
}
.ph-desc {
  font-size: var(--f-aux);
  color: var(--c-text-3);
  margin-top: var(--s-2);
  text-align: center;
}
</style>
