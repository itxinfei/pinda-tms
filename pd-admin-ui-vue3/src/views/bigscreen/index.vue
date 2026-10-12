<template>
  <div class="bs-screen">
    <div ref="stageRef" class="bs-stage" :style="stageStyle">
      <ScreenHeader />

      <KpiRow
        :today-order-count="summary?.todayOrderCount ?? 0"
        :today-transport-order-count="summary?.todayTransportOrderCount ?? 0"
        :active-truck-count="summary?.activeTruckCount ?? 0"
        :pending-alarm-count="pendingAlarmCount"
      />

      <main class="bs-main">
        <div class="bs-col">
          <PanelFrame title="近 7 日订单趋势">
            <OrderTrendChart v-if="trend.length" :points="trend" />
            <div v-else class="bs-empty">暂无数据</div>
          </PanelFrame>
          <PanelFrame title="近 7 日各线路运量 TOP10">
            <LineVolumeChart v-if="lineVolumes.length" :items="lineVolumes" />
            <div v-else class="bs-empty">暂无数据</div>
          </PanelFrame>
        </div>

        <PanelFrame class="bs-map-panel" title="全国车辆实时分布">
          <VehicleMap :locations="locations" />
        </PanelFrame>

        <div class="bs-col">
          <PanelFrame title="车辆状态构成">
            <TruckStatusPie
              :active="summary?.activeTruckCount ?? 0"
              :pending="summary?.pendingTruckCount ?? 0"
              :idle="summary?.idleTruckCount ?? 0"
            />
          </PanelFrame>
          <PanelFrame title="实时告警">
            <AlertScroll :alarms="alarms" />
          </PanelFrame>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import DashboardAPI, {
  type BigScreenSummary,
  type LineVolume,
  type OrderTrendPoint,
} from "@/api/dashboard";
import AlarmAPI, { type AlarmRecord } from "@/api/alarm";
import TraceAPI, { type LocationRecordItem } from "@/api/trace";
import { useKeepTicking } from "./composables/useKeepTicking";
import ScreenHeader from "./components/screen-header.vue";
import KpiRow from "./components/kpi-row.vue";
import PanelFrame from "./components/panel-frame.vue";
import OrderTrendChart from "./components/order-trend-chart.vue";
import LineVolumeChart from "./components/line-volume-chart.vue";
import TruckStatusPie from "./components/truck-status-pie.vue";
import AlertScroll from "./components/alert-scroll.vue";
import VehicleMap from "./components/vehicle-map.vue";

defineOptions({ name: "BigScreen" });

/** 设计稿固定尺寸，按视口等比缩放（不做 rem/vw 自适应） */
const DESIGN_WIDTH = 1920;
const DESIGN_HEIGHT = 1080;
/** 大屏轮询间隔 30 秒 */
const REFRESH_INTERVAL = 30_000;

const scale = ref(1);
const stageStyle = computed(() => ({
  width: `${DESIGN_WIDTH}px`,
  height: `${DESIGN_HEIGHT}px`,
  transform: `scale(${scale.value})`,
}));

const summary = ref<BigScreenSummary | null>(null);
const trend = ref<OrderTrendPoint[]>([]);
const lineVolumes = ref<LineVolume[]>([]);
const alarms = ref<AlarmRecord[]>([]);
const locations = ref<LocationRecordItem[]>([]);
const pendingAlarmCount = ref(0);

const updateScale = () => {
  scale.value = Math.min(
    window.innerWidth / DESIGN_WIDTH,
    window.innerHeight / DESIGN_HEIGHT,
  );
};

/** 单项失败不影响其他面板：各自 settle，保留上一次成功数据 */
const loadAll = async () => {
  const results = await Promise.allSettled([
    DashboardAPI.getBigScreenSummary(),
    DashboardAPI.getTrend(7),
    AlarmAPI.page({ page: 1, pageSize: 20 }),
    AlarmAPI.page({ page: 1, pageSize: 1, status: 0 }),
    TraceAPI.page({ page: 1, pageSize: 200, type: "truck" }),
  ]);

  if (results[0].status === "fulfilled" && results[0].value) {
    summary.value = results[0].value;
    lineVolumes.value = results[0].value.lineVolumes ?? [];
  }
  if (results[1].status === "fulfilled" && results[1].value) {
    trend.value = results[1].value;
  }
  if (results[2].status === "fulfilled" && results[2].value) {
    alarms.value = results[2].value.items ?? [];
  }
  if (results[3].status === "fulfilled" && results[3].value) {
    pendingAlarmCount.value = results[3].value.total ?? 0;
  }
  if (results[4].status === "fulfilled" && results[4].value) {
    locations.value = results[4].value.items ?? [];
  }
};

useKeepTicking(loadAll, REFRESH_INTERVAL);

onMounted(() => {
  updateScale();
  window.addEventListener("resize", updateScale);
  loadAll();
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", updateScale);
});
</script>

<style lang="scss">
@use "./assets/fonts/font.scss";
@use "./assets/styles/screen.scss";
</style>

<style scoped lang="scss">
.bs-screen {
  position: fixed;
  inset: 0;
  overflow: hidden;
  background: #050b24 url("./assets/images/bg.webp") center / cover no-repeat;
}

.bs-stage {
  position: absolute;
  top: 50%;
  left: 50%;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 0 20px 18px;
  transform-origin: center center;
  /* 居中由 translate 配合 scale 完成（-50% 基于自身尺寸，缩放后仍居中） */
  margin-left: -960px;
  margin-top: -540px;
}

.bs-main {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 450px 1fr 450px;
  gap: 14px;
}

.bs-col {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;

  > :first-child,
  > :last-child {
    flex: 1;
    min-height: 0;
  }
}

.bs-map-panel {
  min-height: 0;
}

.bs-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--bs-text-dim);
  font-size: 13px;
  letter-spacing: 2px;
}
</style>
