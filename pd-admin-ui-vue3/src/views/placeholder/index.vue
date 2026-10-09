<template>
  <div class="placeholder-page">
    <el-result
      icon="info"
      :title="title"
      :sub-title="description"
    >
      <template #extra>
        <el-button type="primary" @click="goBack">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from "vue-router";

const route = useRoute();
const router = useRouter();

const title = route.meta.title || "页面建设中";
const description = "该功能正在开发中，敬请期待...";

function goBack() {
  router.push("/dashboard");
}
</script>

<style lang="scss" scoped>
.placeholder-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}
</style>
