<template>
  <div class="bs-pie">
    <ECharts :options="option" width="100%" height="100%" />
    <div class="bs-pie__center">
      <div class="bs-pie__total">{{ total }}</div>
      <div class="bs-pie__hint">订单总数</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { EChartsOption } from "echarts";
import ECharts from "@/components/ECharts/index.vue";

defineOptions({ name: "OrderStatusPie" });

const props = defineProps<{
  /** 待处理订单数（状态 1-5） */
  pending: number;
  /** 运输中订单数（状态 6-9） */
  inTransit: number;
  /** 已签收订单数（状态 10） */
  completed: number;
}>();

const total = computed(() => props.pending + props.inTransit + props.completed);

const option = computed<EChartsOption>(() => ({
  tooltip: {
    trigger: "item",
    backgroundColor: "rgba(2,16,54,0.92)",
    borderColor: "rgba(0,234,255,0.4)",
    textStyle: { color: "#d6ecff", fontSize: 12 },
    formatter: "{b}：{c} 单（{d}%）",
  },
  legend: {
    bottom: 4,
    itemWidth: 13,
    itemHeight: 13,
    itemGap: 16,
    icon: "circle",
    textStyle: { color: "#cfe8ff", fontSize: 14 },
  },
  series: [
    {
      type: "pie",
      radius: ["38%", "50%"],
      center: ["50%", "49%"],
      avoidLabelOverlap: true,
      padAngle: 2,
      itemStyle: {
        borderColor: "rgba(5,12,30,0.65)",
        borderWidth: 2,
        borderRadius: 5,
      },
      label: {
        show: true,
        position: "inside",
        formatter: "{d}%",
        color: "#ffffff",
        fontSize: 15,
        fontWeight: 700,
        textBorderColor: "rgba(0,0,0,0.55)",
        textBorderWidth: 2,
      },
      labelLine: { show: false },
      emphasis: {
        scale: true,
        scaleSize: 6,
        label: { show: true },
      },
      data: [
        { value: props.pending, name: "待处理", itemStyle: { color: "#ffc14d" } },
        { value: props.inTransit, name: "运输中", itemStyle: { color: "#00eaff" } },
        { value: props.completed, name: "已签收", itemStyle: { color: "#00ff7f" } },
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
    top: 49%;
    left: 50%;
    transform: translate(-50%, -50%);
    text-align: center;
    pointer-events: none;
  }

  &__total {
    font-family: var(--bs-font-title);
    font-size: 34px;
    color: #fff;
    text-shadow: 0 0 14px rgba(0, 234, 255, 0.6);
  }

  &__hint {
    margin-top: 2px;
    font-size: 12px;
    color: var(--bs-text-dim);
    letter-spacing: 2px;
  }
}
</style>
