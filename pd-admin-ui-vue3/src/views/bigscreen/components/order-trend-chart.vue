<template>
  <ECharts :options="option" width="100%" height="100%" />
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { EChartsOption } from "echarts";
import ECharts from "@/components/ECharts/index.vue";
import type { OrderTrendPoint } from "@/api/dashboard";

defineOptions({ name: "OrderTrendChart" });

const props = defineProps<{
  points: OrderTrendPoint[];
}>();

const option = computed<EChartsOption>(() => {
  const dates = props.points.map((p) => p.date.slice(5));
  const orders = props.points.map((p) => p.orderCount ?? 0);
  const completed = props.points.map((p) => p.completedCount ?? 0);

  return {
    grid: { top: 42, right: 16, bottom: 28, left: 40 },
    tooltip: {
      trigger: "axis",
      backgroundColor: "rgba(2,16,54,0.92)",
      borderColor: "rgba(0,234,255,0.4)",
      textStyle: { color: "#d6ecff", fontSize: 12 },
    },
    legend: {
      top: 6,
      right: 8,
      itemWidth: 14,
      itemHeight: 8,
      textStyle: { color: "#7fa8c9", fontSize: 12 },
      data: ["下单量", "签收量"],
    },
    xAxis: {
      type: "category",
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: "rgba(0,234,255,0.25)" } },
      axisTick: { show: false },
      axisLabel: { color: "rgba(148,197,255,0.65)", fontSize: 11 },
    },
    yAxis: {
      type: "value",
      minInterval: 1,
      splitLine: { lineStyle: { color: "rgba(0,150,255,0.12)" } },
      axisLabel: { color: "rgba(148,197,255,0.65)", fontSize: 11 },
    },
    series: [
      {
        name: "下单量",
        type: "line",
        smooth: true,
        symbol: "circle",
        symbolSize: 6,
        data: orders,
        lineStyle: { width: 2, color: "#00eaff" },
        itemStyle: { color: "#00eaff" },
        areaStyle: {
          color: {
            type: "linear",
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: "rgba(0,234,255,0.35)" },
              { offset: 1, color: "rgba(0,234,255,0.02)" },
            ],
          },
        },
      },
      {
        name: "签收量",
        type: "line",
        smooth: true,
        symbol: "circle",
        symbolSize: 6,
        data: completed,
        lineStyle: { width: 2, color: "#00ff7f" },
        itemStyle: { color: "#00ff7f" },
        areaStyle: {
          color: {
            type: "linear",
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: "rgba(0,255,127,0.28)" },
              { offset: 1, color: "rgba(0,255,127,0.02)" },
            ],
          },
        },
      },
    ],
  };
});
</script>
