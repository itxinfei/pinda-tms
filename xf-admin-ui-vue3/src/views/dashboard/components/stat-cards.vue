<template>
  <section class="dash-stats">
    <el-skeleton v-if="loading" :rows="4" animated class="dash-stats__skeleton" />
    <el-result
      v-else-if="error"
      icon="error"
      title="总览数据加载失败"
      class="dash-result"
    >
      <template #extra>
        <el-button size="small" type="primary" @click="emit('retry')">重试</el-button>
      </template>
    </el-result>
    <template v-else>
      <div v-for="card in cards" :key="card.label" class="stat-card" @click="card.action">
        <div class="stat-card__icon" :class="`stat-card__icon--${card.tone}`">
          <el-icon :size="18"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-card__body">
          <div class="stat-card__top">
            <span class="stat-card__label">{{ card.label }}</span>
            <span class="stat-card__badge" :class="card.badgeClass">{{ card.badge }}</span>
          </div>
          <div class="stat-card__bottom">
            <span class="stat-card__num">{{ card.value }}</span>
            <svg
              v-if="card.spark && sparkPath(card.spark)"
              class="stat-card__spark"
              viewBox="0 0 100 24"
              preserveAspectRatio="none"
              :aria-label="`近 7 日${card.label}趋势`"
            >
              <defs>
                <linearGradient :id="`spark-${card.tone}`" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="var(--el-color-primary)" stop-opacity="0.18" />
                  <stop offset="100%" stop-color="var(--el-color-primary)" stop-opacity="0" />
                </linearGradient>
              </defs>
              <path
                :d="sparkPath(card.spark)!.area"
                :fill="`url(#spark-${card.tone})`"
                stroke="none"
              />
              <polyline
                :points="sparkPath(card.spark)!.line"
                fill="none"
                stroke="var(--el-color-primary)"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import type { Component } from "vue";

export interface StatCardItem {
  label: string;
  value: string;
  icon: Component;
  tone: "order" | "truck" | "alert" | "rate";
  badge: string;
  badgeClass: string;
  action: () => void;
  /** 近 7 日迷你趋势数据（无数据源传 null 不渲染） */
  spark: number[] | null;
}

defineProps<{
  cards: StatCardItem[];
  loading: boolean;
  error: boolean;
}>();

const emit = defineEmits<{ retry: [] }>();

/**
 * 迷你趋势线路径：100×24 视口归一化折线 + 面积
 * 返回 { line: polyline points, area: area path }，数值全 0 时返回 null 不渲染
 */
function sparkPath(values: number[]): { line: string; area: string } | null {
  if (!Array.isArray(values) || values.length < 2) return null;
  const max = Math.max(...values, 1);
  const min = Math.min(...values, 0);
  const span = max - min || 1;
  const pts = values.map((v, i) => {
    const x = (i / (values.length - 1)) * 100;
    const y = 20 - ((v - min) / span) * 16; // 预留上下 2px 边距
    return `${x.toFixed(1)},${y.toFixed(1)}`;
  });
  return {
    line: pts.join(" "),
    area: `${pts[0]} L${pts.slice(1).join(" L")} L100,24 L0,24 Z`,
  };
}
</script>

<style lang="scss">
@use "./dashboard-card.scss";
</style>
