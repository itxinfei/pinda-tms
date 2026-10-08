<template>
  <div class="build-badge" title="构建版本 / 时间，用于确认线上跑的是哪份代码">构建: {{ sha }} @ {{ time }}</div>
</template>

<script>
export default {
  name: 'BuildBadge',
  data() {
    // 构建时由 scripts/build-with-version.js 注入的 VUE_APP_* 变量
    return {
      sha: process.env.VUE_APP_GIT_SHA || 'unknown',
      time: (process.env.VUE_APP_BUILD_TIME || 'unknown').replace('T', ' ').slice(0, 19)
    }
  }
}
</script>

<style scoped>
.build-badge {
  position: fixed;
  right: 6px;
  bottom: 4px;
  z-index: 9999;
  font-size: 11px;
  line-height: 16px;
  color: #909399;
  background: rgba(255, 255, 255, 0.75);
  padding: 1px 6px;
  border-radius: 4px;
  pointer-events: none;
  user-select: none;
}
</style>
