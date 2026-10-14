<template>
  <div class="card dash-chart__trend">
    <div class="card__head">
      <h3 class="card__title">订单量趋势</h3>
      <div class="card__head-right">
        <el-tooltip content="查看全部订单" placement="top" :show-after="300">
          <el-button link type="primary" size="small" @click="emit('view-all')">全部订单</el-button>
        </el-tooltip>
        <el-radio-group :model-value="dateRange" size="small" @update:model-value="emit('update:dateRange', $event as number)">
          <el-radio-button :value="7">近7天</el-radio-button>
          <el-radio-button :value="30">近30天</el-radio-button>
        </el-radio-group>
      </div>
    </div>
    <div class="card__body card__body--chart">
      <el-skeleton v-if="loading" :rows="4" animated class="dash-chart__skeleton" />
      <template v-else>
        <el-result v-if="error" icon="error" title="趋势数据加载失败" class="dash-result">
          <template #extra>
            <el-button size="small" type="primary" @click="emit('retry')">重试</el-button>
          </template>
        </el-result>
        <el-empty v-else-if="empty" description="所选时段暂无订单数据" :image-size="64" />
        <ECharts v-else :options="options" height="190px" @click="onChartClick" />
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
  dateRange: number;
}>();

const emit = defineEmits<{
  retry: [];
  "view-all": [];
  "update:dateRange": [value: number];
  "click-date": [date: string];
}>();

/** 仅接受 "yyyy-MM-dd" 格式日期（避免误触图例/坐标轴空白区），否则忽略 */
function onChartClick(params: unknown) {
  const name = (params as { name?: string } | null)?.name;
  if (!name || !/^\d{4}-\d{2}-\d{2}$/.test(String(name))) return;
  emit("click-date", String(name));
}
</script>

<style lang="scss">
@use "./dashboard-card.scss";
</style>
