<template>
  <div class="bs-screen">
    <div ref="stageRef" class="bs-stage" :style="stageStyle">
      <ScreenHeader />

      <KpiRow
        :today-order-count="summary?.todayOrderCount ?? 0"
        :today-transport-order-count="summary?.todayTransportOrderCount ?? 0"
        :today-task-count="summary?.todayTaskCount ?? 0"
        :active-truck-count="summary?.activeTruckCount ?? 0"
        :pending-truck-count="summary?.pendingTruckCount ?? 0"
        :idle-truck-count="summary?.idleTruckCount ?? 0"
        :pending-alarm-count="pendingAlarmCount"
        :on-time-rate="null"
      />

      <main class="bs-main">
        <!-- 左列：趋势 / 订单状态 / 线路运量 -->
        <div class="bs-col">
          <PanelFrame title="订单下单 / 签收趋势">
            <OrderTrendChart v-if="trend.length" :points="trend" />
            <div v-else class="bs-empty">暂无数据</div>
          </PanelFrame>
          <PanelFrame title="订单状态构成" class="bs-panel--scan">
            <OrderStatusPie
              :pending="overview?.pendingCount ?? 0"
              :in-transit="overview?.inTransitCount ?? 0"
              :completed="overview?.completedCount ?? 0"
            />
          </PanelFrame>
          <PanelFrame title="近 7 日各线路运量 TOP10">
            <LineVolumeChart v-if="lineVolumes.length" :items="lineVolumes" />
            <div v-else class="bs-empty">暂无数据</div>
          </PanelFrame>
          <PanelFrame title="重点车辆轨迹回放" class="bs-panel--scan bs-track-panel">
            <template #extra>
              <select
                v-model="selectedVehicle"
                class="bs-track-select"
                :disabled="!vehicleOptions.length"
              >
                <option v-for="o in vehicleOptions" :key="o.id" :value="o.id">
                  {{ o.label }}（{{ o.count }}）
                </option>
              </select>
            </template>
            <TrajectoryReplay :trail="selectedTrail" :vehicle-name="selectedVehicleName" />
          </PanelFrame>
        </div>

        <!-- 中列：全国地图（车辆+网点+流向）/ 在险预警 / 运输任务五态 -->
        <div class="bs-center">
          <PanelFrame class="bs-map-panel" title="全国车辆与机构实时分布">
            <VehicleMap :locations="locations" :orgs="orgPoints" />
          </PanelFrame>
          <PanelFrame class="bs-risk-panel" title="在险事件预警">
            <RiskWarning :items="riskItems" />
          </PanelFrame>
          <PanelFrame class="bs-task-panel" title="运输任务五态">
            <TaskStatusBar v-if="taskStatusCounts.length" :items="taskStatusCounts" />
            <div v-else class="bs-empty">暂无数据</div>
          </PanelFrame>
        </div>

        <!-- 右列：实时告警 / 待处理订单 / 车辆状态 -->
        <div class="bs-col">
          <PanelFrame title="实时告警" class="bs-panel--scan">
            <AlertScroll :alarms="alarms" />
          </PanelFrame>
          <PanelFrame title="待处理订单">
            <TodoList :items="todoList" />
          </PanelFrame>
          <PanelFrame title="车辆状态构成">
            <TruckStatusPie
              :active="summary?.activeTruckCount ?? 0"
              :pending="summary?.pendingTruckCount ?? 0"
              :idle="summary?.idleTruckCount ?? 0"
            />
          </PanelFrame>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import DashboardAPI, {
  type BigScreenSummary,
  type DashboardOverview,
  type LineVolume,
  type OrderTrendPoint,
  type PendingOrder,
  type NameValue,
} from "@/api/dashboard";
import AlarmAPI, { alarmTypeText, type AlarmRecord } from "@/api/alarm";
import TraceAPI, { type LocationRecordItem } from "@/api/trace";
import OrgAPI, { type OrgNode } from "@/api/org";
import { useKeepTicking } from "./composables/useKeepTicking";
import type { OrgPoint } from "./components/vehicle-map.vue";
import ScreenHeader from "./components/screen-header.vue";
import KpiRow from "./components/kpi-row.vue";
import PanelFrame from "./components/panel-frame.vue";
import OrderTrendChart from "./components/order-trend-chart.vue";
import OrderStatusPie from "./components/order-status-pie.vue";
import LineVolumeChart from "./components/line-volume-chart.vue";
import VehicleMap from "./components/vehicle-map.vue";
import TaskStatusBar from "./components/task-status-bar.vue";
import AlertScroll from "./components/alert-scroll.vue";
import RiskWarning, { type RiskItem } from "./components/risk-warning.vue";
import TrajectoryReplay, { type TrailPoint } from "./components/trajectory-replay.vue";
import TodoList from "./components/todo-list.vue";
import TruckStatusPie from "./components/truck-status-pie.vue";

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
const overview = ref<DashboardOverview | null>(null);
const trend = ref<OrderTrendPoint[]>([]);
const lineVolumes = ref<LineVolume[]>([]);
const taskStatusCounts = ref<NameValue[]>([]);
const alarms = ref<AlarmRecord[]>([]);
const todoList = ref<PendingOrder[]>([]);
const locations = ref<LocationRecordItem[]>([]);
const orgPoints = ref<OrgPoint[]>([]);
const pendingAlarmCount = ref(0);

/**
 * 重点车辆轨迹回放（吸收中交兴路轨迹回放时间轴）。
 * 后端暂无历史轨迹端点，这里采用"跨轮询累积"方案：每次轮询拿到的在途车辆最新定位点，
 * 按 businessId 去重（同时间戳不重复）累积成轨迹，模拟回放所需的连续路径。
 * 待真实历史轨迹端点就绪，只需把 trailHistory 的来源替换为该端点即可（组件接口不变）。
 */
const trailHistory = ref<Record<string, TrailPoint[]>>({});
const vehicleMeta = ref<Record<string, { name?: string; licensePlate?: string }>>({});
const selectedVehicle = ref("");

/** 单辆车轨迹最多保留点数，防止长时间运行内存膨胀 */
const TRAIL_MAX_POINTS = 120;

/** 把本轮定位点累积进历史轨迹（同一车辆跨轮询连成路径） */
const accumulateTrails = (items: LocationRecordItem[]) => {
  const history: Record<string, TrailPoint[]> = { ...trailHistory.value };
  const meta: Record<string, { name?: string; licensePlate?: string }> = { ...vehicleMeta.value };
  for (const it of items) {
    const lng = Number(it.longitude);
    const lat = Number(it.latitude);
    if (!it.businessId || !Number.isFinite(lng) || !Number.isFinite(lat)) continue;
    const t = it.reportTime
      ? new Date(it.reportTime).getTime()
      : it.createTime
        ? new Date(it.createTime).getTime()
        : Date.now();
    if (!Number.isFinite(t)) continue;
    const arr = history[it.businessId] ? [...history[it.businessId]] : [];
    if (!arr.some((p) => p.t === t)) {
      arr.push({ lng, lat, t });
      arr.sort((a, b) => a.t - b.t);
      if (arr.length > TRAIL_MAX_POINTS) arr.splice(0, arr.length - TRAIL_MAX_POINTS);
      history[it.businessId] = arr;
    }
    meta[it.businessId] = { name: it.name, licensePlate: it.licensePlate };
  }
  trailHistory.value = history;
  vehicleMeta.value = meta;
};

/** 可选车辆列表：仅保留轨迹点数 ≥ 2 的车辆，按点数降序 */
const vehicleOptions = computed(() =>
  Object.entries(trailHistory.value)
    .filter(([, pts]) => pts.length >= 2)
    .map(([id, pts]) => {
      const m = vehicleMeta.value[id];
      return {
        id,
        label: m?.licensePlate || m?.name || `车辆${id}`,
        count: pts.length,
      };
    })
    .sort((a, b) => b.count - a.count),
);

const selectedTrail = computed<TrailPoint[]>(() => trailHistory.value[selectedVehicle.value] ?? []);
const selectedVehicleName = computed(() => {
  const m = vehicleMeta.value[selectedVehicle.value];
  return m?.licensePlate || m?.name || (selectedVehicle.value ? `车辆${selectedVehicle.value}` : "");
});

/** 无选中或选中车辆已无轨迹时，自动切到列表首项 */
watch(vehicleOptions, (opts) => {
  if (!selectedVehicle.value && opts.length) {
    selectedVehicle.value = opts[0].id;
  } else if (selectedVehicle.value && !opts.some((o) => o.id === selectedVehicle.value)) {
    selectedVehicle.value = opts[0]?.id ?? "";
  }
});

/**
 * 风险预警卡数据源（吸收 Shipsy At-Risk 控制塔范式）。
 * 当前由 netty 实时告警派生：GPS 类（超速/偏离=高危，停留/离线=中危）映射为风险项。
 * 后续「时效超时预警」后端端点就绪后，把超时运单也转成 RiskItem 追加进来即可。
 */
const riskItems = computed<RiskItem[]>(() =>
  (alarms.value ?? []).map((a: AlarmRecord) => {
    const gps = a.alarmType !== undefined && a.alarmType >= 1 && a.alarmType <= 9;
    const level: RiskItem["level"] =
      gps ? (a.alarmType === 1 || a.alarmType === 3 ? "high" : "mid") : "low";
    const title = a.truckId
      ? `车辆 ${a.truckId}`
      : a.transportTaskId
        ? `任务 ${a.transportTaskId}`
        : "运输过程";
    return {
      id: String(a.id),
      level,
      category: alarmTypeText(a.alarmType),
      title,
      detail: a.alarmContent || "运输过程异常，请及时关注",
      time: a.alarmTime || a.createTime,
    };
  }),
);

const updateScale = () => {
  scale.value = Math.min(
    window.innerWidth / DESIGN_WIDTH,
    window.innerHeight / DESIGN_HEIGHT,
  );
};

/** 组织树扁平化为带父坐标的点（供地图机构网络线使用） */
const flattenOrgs = (nodes: OrgNode[], parent?: OrgNode): OrgPoint[] => {
  const out: OrgPoint[] = [];
  for (const n of nodes) {
    const hasCoord = Number.isFinite(n.longitude) && Number.isFinite(n.latitude);
    if (hasCoord) {
      out.push({
        name: n.name,
        value: [n.longitude as number, n.latitude as number],
        orgType: n.orgType,
        parent:
          parent && Number.isFinite(parent.longitude) && Number.isFinite(parent.latitude)
            ? [parent.longitude as number, parent.latitude as number]
            : undefined,
      });
    }
    if (n.children?.length) {
      out.push(...flattenOrgs(n.children, n));
    }
  }
  return out;
};

/** 单项失败不影响其他面板：各自 settle，保留上一次成功数据 */
const loadAll = async () => {
  const results = await Promise.allSettled([
    DashboardAPI.getBigScreenSummary(),
    DashboardAPI.getTrend(7),
    DashboardAPI.getOverview(),
    DashboardAPI.getTodo(8),
    AlarmAPI.page({ page: 1, pageSize: 20 }),
    AlarmAPI.page({ page: 1, pageSize: 1, status: 0 }),
    TraceAPI.page({ page: 1, pageSize: 200, type: "truck" }),
    OrgAPI.getTree(),
  ]);

  if (results[0].status === "fulfilled" && results[0].value) {
    summary.value = results[0].value;
    lineVolumes.value = results[0].value.lineVolumes ?? [];
    taskStatusCounts.value = results[0].value.taskStatusCounts ?? [];
  }
  if (results[1].status === "fulfilled" && results[1].value) {
    trend.value = results[1].value;
  }
  if (results[2].status === "fulfilled" && results[2].value) {
    overview.value = results[2].value;
  }
  if (results[3].status === "fulfilled" && results[3].value) {
    todoList.value = results[3].value ?? [];
  }
  if (results[4].status === "fulfilled" && results[4].value) {
    alarms.value = results[4].value.items ?? [];
  }
  if (results[5].status === "fulfilled" && results[5].value) {
    pendingAlarmCount.value = results[5].value.total ?? 0;
  }
  if (results[6].status === "fulfilled" && results[6].value) {
    locations.value = results[6].value.items ?? [];
    accumulateTrails(locations.value);
  }
  if (results[7].status === "fulfilled" && results[7].value) {
    orgPoints.value = flattenOrgs(results[7].value ?? []);
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
  gap: 12px;
  padding: 12px 20px 16px;
  transform-origin: center center;
  /* 居中由 translate 配合 scale 完成（-50% 基于自身尺寸，缩放后仍居中） */
  margin-left: -960px;
  margin-top: -540px;
}

.bs-main {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 460px 1fr 460px;
  gap: 12px;
}

.bs-col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;

  > :nth-child(1) {
    flex: 1;
    min-height: 0;
  }

  > :nth-child(2) {
    flex: 1.15;
    min-height: 0;
  }

  > :nth-child(3) {
    flex: 0.85;
    min-height: 0;
  }

  > :nth-child(4) {
    flex: 1.3;
    min-height: 0;
  }
}

.bs-center {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.bs-map-panel {
  flex: 1;
  min-height: 0;
}

.bs-task-panel {
  flex: none;
  height: 200px;
}

.bs-risk-panel {
  flex: none;
  height: 156px;
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

.bs-track-select {
  margin-left: auto;
  max-width: 168px;
  padding: 2px 6px;
  font-size: 12px;
  color: var(--bs-cyan);
  background: rgba(0, 234, 255, 0.08);
  border: 1px solid var(--bs-panel-border);
  border-radius: 2px;
  outline: none;
  cursor: pointer;

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}
</style>
