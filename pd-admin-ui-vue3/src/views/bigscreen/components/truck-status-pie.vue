<template>
  <div class="bs-pie">
    <ECharts :options="option" width="100%" height="100%" />
    <div class="bs-pie__center">
      <div class="bs-pie__total">{{ total }}</div>
      <div class="bs-pie__hint">车辆总数</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { EChartsOption } from "echarts";
import ECharts from "@/components/ECharts/index.vue";

defineOptions({ name: "TruckStatusPie" });

const props = defineProps<{
  active: number;
  pending: number;
  idle: number;
}>();

const total = computed(() => props.active + props.pending + props.idle);

const option = computed<EChartsOption>(() => ({
  tooltip: {
    trigger: "item",
    backgroundColor: "rgba(2,16,54,0.92)",
    borderColor: "rgba(0,234,255,0.4)",
    textStyle: { color: "#d6ecff", fontSize: 12 },
    formatter: "{b}：{c} 辆（{d}%）",
  },
  legend: {
    bottom: 2,
    itemWidth: 10,
    itemHeight: 10,
    icon: "circle",
    textStyle: { color: "#9fc7e8", fontSize: 12 },
  },
  series: [
    {
      type: "pie",
      radius: ["52%", "70%"],
      center: ["50%", "44%"],
      avoidLabelOverlap: true,
      label: { show: false },
      emphasis: {
        scale: true,
        scaleSize: 6,
        label: { show: false },
      },
      data: [
        { value: props.active, name: "在途", itemStyle: { color: "#00eaff" } },
        { value: props.pending, name: "待执行", itemStyle: { color: "#ffc14d" } },
        { value: props.idle, name: "空闲", itemStyle: { color: "#2f6bff" } },
      ],
    },
  ],
}));
</script>

<style scoped lang="scss">
.bs-pie {
  position: relative;
  width: 100%;
  height: 100%;

  &__center {
    position: absolute;
    top: 44%;
    left: 50%;
    transform: translate(-50%, -50%);
    text-align: center;
    pointer-events: none;
  }

  &__total {
    font-family: var(--bs-font-title);
    font-size: 30px;
    color: #fff;
    text-shadow: 0 0 12px rgba(0, 234, 255, 0.5);
  }

  &__hint {
    margin-top: 2px;
    font-size: 12px;
    color: var(--bs-text-dim);
    letter-spacing: 2px;
  }
}
</style>
