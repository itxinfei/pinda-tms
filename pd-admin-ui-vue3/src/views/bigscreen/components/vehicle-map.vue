<template>
  <div class="bs-map">
    <ECharts :options="option" width="100%" height="100%" />
    <div v-if="!points.length" class="bs-map__empty">暂无车辆定位数据</div>
  </div>
</template>

<script setup lang="ts">
// echarts/core 是模块单例：此处 registerMap 与全局 ECharts 组件共享同一份注册表，
// 必须在组件模块求值阶段（早于子组件 mounted）完成注册。
import * as echarts from "echarts/core";
import chinaGeoJSON from "../assets/chinaGeoJSON.json";
import { computed } from "vue";
import type { EChartsOption } from "echarts";
import ECharts from "@/components/ECharts/index.vue";
import type { LocationRecordItem } from "@/api/trace";

echarts.registerMap("china", chinaGeoJSON as never);

defineOptions({ name: "VehicleMap" });

const props = defineProps<{
  locations: LocationRecordItem[];
}>();

interface VehiclePoint {
  name: string;
  value: [number, number];
  driver?: string;
  time?: string;
}

// 后端按入库时间倒序，同一车辆首次出现即最新一条；坐标为 BD09，城市级展示不做纠偏
const points = computed<VehiclePoint[]>(() => {
  const seen = new Set<string>();
  const result: VehiclePoint[] = [];
  for (const item of props.locations) {
    if (!item.businessId || seen.has(item.businessId)) {
      continue;
    }
    const lng = Number(item.longitude);
    const lat = Number(item.latitude);
    if (!Number.isFinite(lng) || !Number.isFinite(lat)) {
      continue;
    }
    seen.add(item.businessId);
    result.push({
      name: item.licensePlate || `车辆${item.businessId}`,
      value: [lng, lat],
      driver: item.name,
      time: item.reportTime || item.createTime,
    });
  }
  return result;
});

const option = computed<EChartsOption>(() => ({
  tooltip: {
    trigger: "item",
    backgroundColor: "rgba(2,16,54,0.92)",
    borderColor: "rgba(0,234,255,0.4)",
    textStyle: { color: "#d6ecff", fontSize: 12 },
    formatter: (params: unknown) => {
      const p = params as { data?: VehiclePoint };
      const d = p.data;
      if (!d) {
        return "";
      }
      const lines = [
        `<b>${d.name}</b>`,
        d.driver ? `司机：${d.driver}` : "",
        d.time ? `上报时间：${d.time}` : "",
        `坐标：${d.value[0].toFixed(4)}, ${d.value[1].toFixed(4)}`,
      ];
      return lines.filter(Boolean).join("<br/>");
    },
  },
  geo: {
    map: "china",
    aspectScale: 0.85,
    layoutCenter: ["50%", "52%"],
    layoutSize: "98%",
    roam: true,
    label: { show: false },
    itemStyle: {
      areaColor: "rgba(7,21,55,0.8)",
      borderColor: "#0177ff",
      borderWidth: 1.2,
      shadowColor: "rgba(7,21,55,0.8)",
      shadowBlur: 10,
    },
    emphasis: {
      label: { show: false },
      itemStyle: { areaColor: "rgba(0,90,200,0.7)" },
    },
  },
  series: [
    {
      name: "在途车辆",
      type: "effectScatter",
      coordinateSystem: "geo",
      showEffectOn: "render",
      rippleEffect: { brushType: "stroke", scale: 3.2 },
      symbolSize: 9,
      itemStyle: {
        color: "#ff8a3d",
        shadowBlur: 10,
        shadowColor: "rgba(255,138,61,0.9)",
      },
      emphasis: {
        label: {
          show: true,
          position: "right",
          color: "#fff",
          fontSize: 12,
          formatter: "{b}",
        },
      },
      data: points.value,
    },
  ],
}));
</script>

<style scoped lang="scss">
.bs-map {
  position: relative;
  width: 100%;
  height: 100%;

  &__empty {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    color: var(--bs-text-dim);
    font-size: 14px;
    letter-spacing: 3px;
    pointer-events: none;
  }
}
</style>
