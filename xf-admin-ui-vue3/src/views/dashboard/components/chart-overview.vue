<template>
  <div class="card card--link" @click="emit('click')">
    <div class="card__head">
      <h3 class="card__title">订单状态概览</h3>
      <el-tag type="primary" size="small" effect="plain">{{ headTag }}</el-tag>
    </div>
    <div class="card__body overview-card">
      <el-skeleton v-if="loading" :rows="3" animated class="dash-chart__skeleton" />
      <template v-else>
        <el-result v-if="error" icon="error" title="总览数据加载失败" class="dash-result">
          <template #extra>
            <el-button size="small" type="primary" @click="emit('retry')">重试</el-button>
          </template>
        </el-result>
        <div v-else class="overview-bars">
          <div
            v-for="item in bars"
            :key="item.label"
            class="overview-bars__item"
            :style="{ '--overview-percent': `${item.percent}%` }"
          >
            <div class="overview-bars__meta">
              <span class="overview-bars__label">
                <span class="overview-bars__dot" :style="{ background: item.color }" />
                {{ item.label }}
              </span>
              <span class="overview-bars__value">{{ item.valueText }}</span>
            </div>
            <span class="overview-bars__track">
              <span class="overview-bars__bar" :style="{ background: item.gradient }" />
            </span>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
export interface OverviewBar {
  label: string;
  valueText: string;
  percent: number;
  color: string;
  gradient: string;
}

defineProps<{
  bars: OverviewBar[];
  loading: boolean;
  error: boolean;
  headTag: string;
}>();

const emit = defineEmits<{ retry: []; click: [] }>();
</script>

<style lang="scss">
@use "./dashboard-card.scss";
</style>
