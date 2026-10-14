<template>
  <div class="card card--link" @click="emit('click')">
    <div class="card__head">
      <h3 class="card__title">近7日线路运量 Top5</h3>
      <el-tag size="small" effect="plain">按运输任务量</el-tag>
    </div>
    <div class="card__body card__body--chart">
      <el-skeleton v-if="loading" :rows="4" animated class="dash-chart__skeleton" />
      <template v-else>
        <el-result v-if="error" icon="error" title="线路运量加载失败" class="dash-result">
          <template #extra>
            <el-button size="small" type="primary" @click="emit('retry')">重试</el-button>
          </template>
        </el-result>
        <el-empty v-else-if="empty" description="暂无线路运量数据" :image-size="56" />
        <ECharts v-else :options="options" height="190px" />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { EChartsCoreOption } from "echarts/core";
import ECharts from "@/components/ECharts/index.vue";

defineProps<{
  options: EChartsCoreOption;
  loading: boolean;
  error: boolean;
  empty: boolean;
}>();

const emit = defineEmits<{ retry: []; click: [] }>();
</script>

<style lang="scss">
@use "./dashboard-card.scss";
</style>
