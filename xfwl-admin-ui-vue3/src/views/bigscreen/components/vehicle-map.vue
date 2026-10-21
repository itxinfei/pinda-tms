<template>
  <div class="bs-map">
    <ECharts :options="option" width="100%" height="100%" />
    <div v-if="!hasAnyData" class="bs-map__empty">暂无车辆与网点定位数据</div>
  </div>
</template>

<script setup lang="ts">
// echarts/core 是模块单例：此处 registerMap 与全局 ECharts 组件共享同一份注册表，
// 必须在组件模块求值阶段（早于子组件 mounted）完成注册。
import * as echarts from "echarts/core";
import { HeatmapChart, ScatterChart } from "echarts/charts";
import chinaGeoJSON from "../assets/chinaGeoJSON.json";
import { computed } from "vue";
import type { EChartsOption } from "echarts";
import ECharts from "@/components/ECharts/index.vue";
import type { LocationRecordItem } from "@/api/trace";

echarts.registerMap("china", chinaGeoJSON as never);
// 热力图（在途车辆密度）+ 网点散点需按需注册，复用 echarts/core 单例
echarts.use([HeatmapChart, ScatterChart]);

defineOptions({ name: "VehicleMap" });

export interface OrgPoint {
  name: string;
  /** [经度, 纬度] */
  value: [number, number];
  /** 组织类型：1分公司 2一级转运 3二级转运 4网点 */
  orgType: number;
  /** 上级机构坐标（用于绘制机构网络流向线） */
  parent?: [number, number];
}

const props = defineProps<{
  locations: LocationRecordItem[];
  orgs?: OrgPoint[];
}>();

interface VehiclePoint {
  name: string;
  value: [number, number];
  driver?: string;
  time?: string;
}

// 后端按入库时间倒序，同一车辆首次出现即最新一条；坐标为 BD09，城市级展示不做纠偏
const vehiclePoints = computed<VehiclePoint[]>(() => {
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

// 在途车辆密度热力：按 1° 网格分桶计数，形成真实热区（而非孤立点）
const heatPoints = computed<[number, number, number][]>(() => {
  const buckets = new Map<string, { lng: number; lat: number; count: number }>();
  for (const p of vehiclePoints.value) {
    const lng = Math.round(p.value[0]);
    const lat = Math.round(p.value[1]);
    const key = `${lng},${lat}`;
    const bucket = buckets.get(key) ?? { lng, lat, count: 0 };
    bucket.count += 1;
    buckets.set(key, bucket);
  }
  return [...buckets.values()].map((b) => [b.lng, b.lat, b.count]);
});

const heatMax = computed(() => Math.max(1, ...heatPoints.value.map((p) => p[2])));

const ORG_COLORS: Record<number, string> = {
  1: "#00eaff",
  2: "#2f6bff",
  3: "#00ff7f",
  4: "#ffc14d",
};
const ORG_SIZE: Record<number, number> = { 1: 13, 2: 10, 3: 9, 4: 7 };

const orgPoints = computed(() =>
  (props.orgs ?? [])
    .filter((o) => Number.isFinite(o.value[0]) && Number.isFinite(o.value[1]))
    .map((o) => ({
      name: o.name,
      value: o.value,
      symbolSize: ORG_SIZE[o.orgType] ?? 7,
      itemStyle: { color: ORG_COLORS[o.orgType] ?? "#9fdfff" },
    })),
);

const orgLines = computed(() =>
  (props.orgs ?? [])
    .filter(
      (o) =>
        o.parent &&
        Number.isFinite(o.parent[0]) &&
        Number.isFinite(o.parent[1]) &&
        Number.isFinite(o.value[0]) &&
        Number.isFinite(o.value[1]),
    )
    .map((o) => ({ coords: [o.parent as [number, number], o.value] })),
);

const hasAnyData = computed(() => vehiclePoints.value.length > 0 || orgPoints.value.length > 0);

const option = computed<EChartsOption>(() => ({
  tooltip: {
    trigger: "item",
    backgroundColor: "rgba(2,16,54,0.92)",
    borderColor: "rgba(0,234,255,0.4)",
    textStyle: { color: "#d6ecff", fontSize: 12 },
    formatter: (params: unknown) => {
      const p = params as { seriesType?: string; data?: { name?: string; value?: unknown; driver?: string; time?: string } };
      if (p.seriesType === "lines") {
        return "机构网络流向";
      }
      const d = p.data;
      if (!d) return "";
      if (p.seriesType === "effectScatter") {
        const lines = [
          `<b>${d.name ?? ""}</b>`,
          d.driver ? `司机：${d.driver}` : "",
          d.time ? `上报：${d.time}` : "",
          d.value && Array.isArray(d.value)
            ? `坐标：${(d.value as number[])[0].toFixed(4)}, ${(d.value as number[])[1].toFixed(4)}`
            : "",
        ];
        return lines.filter(Boolean).join("<br/>");
      }
      return `<b>${d.name ?? ""}</b>`;
    },
  },
  visualMap: {
    show: false,
    min: 0,
    max: heatMax.value,
    seriesIndex: 0,
    inRange: {
      color: ["rgba(0,90,200,0.12)", "rgba(0,234,255,0.55)", "rgba(255,138,61,0.9)"],
    },
    calculable: false,
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
      name: "在途密度",
      type: "heatmap",
      coordinateSystem: "geo",
      zlevel: 0,
      pointSize: 22,
      blurSize: 30,
      data: heatPoints.value,
    },
    {
      name: "机构网络",
      type: "lines",
      coordinateSystem: "geo",
      zlevel: 1,
      effect: {
        show: true,
        period: 5,
        trailLength: 0.4,
        symbol: "arrow",
        symbolSize: 5,
        color: "#9fe8ff",
      },
      lineStyle: {
        color: "rgba(0,234,255,0.45)",
        width: 1,
        opacity: 0.6,
        curveness: 0.3,
      },
      data: orgLines.value,
    },
    {
      name: "机构网点",
      type: "scatter",
      coordinateSystem: "geo",
      zlevel: 2,
      symbol: "circle",
      itemStyle: { shadowBlur: 8, shadowColor: "rgba(0,234,255,0.6)" },
      label: {
        show: false,
        position: "right",
        color: "#cfe8ff",
        fontSize: 13,
        formatter: "{b}",
      },
      emphasis: { label: { show: true } },
      data: orgPoints.value,
    },
    {
      name: "在途车辆",
      type: "effectScatter",
      coordinateSystem: "geo",
      zlevel: 3,
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
      data: vehiclePoints.value,
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
