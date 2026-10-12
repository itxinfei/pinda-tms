<template>
  <ECharts :options="option" width="100%" height="100%" />
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { EChartsOption } from "echarts";
import ECharts from "@/components/ECharts/index.vue";
import type { NameValue } from "@/api/dashboard";

defineOptions({ name: "TaskStatusBar" });

const props = defineProps<{
  items: NameValue[];
}>();

const COLORS = ["#00eaff", "#2f6bff", "#00ff7f", "#ffc14d", "#ff5b6b"];

const option = computed<EChartsOption>(() => {
  const data = props.items ?? [];
  // 横向柱状：yAxis 取类目，反转使首项在顶部
  const names = data.map((d) => d.name);
  const values = data.map((d) => d.value);
  return {
    grid: { top: 14, right: 36, bottom: 8, left: 12, containLabel: true },
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "shadow" },
      backgroundColor: "rgba(2,16,54,0.92)",
      borderColor: "rgba(0,234,255,0.4)",
      textStyle: { color: "#d6ecff", fontSize: 12 },
      formatter: "{b}：{c} 单",
    },
    xAxis: {
      type: "value",
      minInterval: 1,
      splitLine: { lineStyle: { color: "rgba(0,150,255,0.12)" } },
      axisLabel: { color: "rgba(148,197,255,0.6)", fontSize: 11 },
      axisLine: { show: false },
      axisTick: { show: false },
    },
    yAxis: {
      type: "category",
      inverse: true,
      data: names,
      axisLine: { lineStyle: { color: "rgba(0,234,255,0.2)" } },
      axisTick: { show: false },
      axisLabel: { color: "#cfe8ff", fontSize: 13 },
    },
    series: [
      {
        type: "bar",
        data: values.map((v, i) => ({
          value: v,
          itemStyle: {
            borderRadius: [0, 4, 4, 0],
            color: COLORS[i % COLORS.length],
            shadowColor: COLORS[i % COLORS.length],
            shadowBlur: 8,
          },
        })),
        barWidth: "46%",
        label: {
          show: true,
          position: "right",
          color: "#d6ecff",
          fontSize: 12,
          fontFamily: "var(--bs-font-num)",
        },
      },
    ],
  };
});
</script>
