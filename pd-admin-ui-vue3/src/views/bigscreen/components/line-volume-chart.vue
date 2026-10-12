<template>
  <ECharts :options="option" width="100%" height="100%" />
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { EChartsOption } from "echarts";
import ECharts from "@/components/ECharts/index.vue";
import type { LineVolume } from "@/api/dashboard";

defineOptions({ name: "LineVolumeChart" });

const props = defineProps<{
  items: LineVolume[];
}>();

const option = computed<EChartsOption>(() => {
  // y 轴类目倒序，使任务量第 1 名显示在最上方
  const names = props.items.map((i) => i.name).reverse();
  const values = props.items.map((i) => i.taskCount ?? 0).reverse();

  return {
    grid: { top: 8, right: 36, bottom: 8, left: 8, containLabel: true },
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "shadow" },
      backgroundColor: "rgba(2,16,54,0.92)",
      borderColor: "rgba(0,234,255,0.4)",
      textStyle: { color: "#d6ecff", fontSize: 12 },
      formatter: (params: unknown) => {
        const p = (params as Array<{ name: string; value: number }>)[0];
        return `${p.name}<br/>运输任务：<b>${p.value}</b> 个`;
      },
    },
    xAxis: {
      type: "value",
      minInterval: 1,
      splitLine: { lineStyle: { color: "rgba(0,150,255,0.12)" } },
      axisLabel: { color: "rgba(148,197,255,0.65)", fontSize: 11 },
    },
    yAxis: {
      type: "category",
      data: names,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: "#9fc7e8",
        fontSize: 11,
        width: 110,
        overflow: "truncate",
      },
    },
    series: [
      {
        type: "bar",
        data: values,
        barMaxWidth: 14,
        itemStyle: {
          borderRadius: [0, 7, 7, 0],
          color: {
            type: "linear",
            x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [
              { offset: 0, color: "rgba(4,126,232,0.35)" },
              { offset: 1, color: "#04eef8" },
            ],
          },
        },
        label: {
          show: true,
          position: "right",
          color: "#00eaff",
          fontSize: 11,
          fontFamily: "DIN, sans-serif",
        },
      },
    ],
  };
});
</script>
