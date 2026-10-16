<template>
  <div class="page-container page-container--split monitor-page">
    <!-- 左侧：模式切换 + 车辆列表 -->
    <aside class="page-aside">
      <div class="page-aside__inner monitor-aside">
        <div class="monitor-aside__head">
          <el-radio-group v-model="mode" size="small">
            <el-radio-button value="realtime">实时监控</el-radio-button>
            <el-radio-button value="replay">轨迹回放</el-radio-button>
          </el-radio-group>
          <el-input
            v-model="keyword"
            placeholder="搜索车牌 / 司机"
            clearable
            size="small"
            :prefix-icon="Search"
          />
        </div>

        <div class="monitor-aside__list">
          <div
            v-for="item in filteredVehicles"
            :key="`${item.businessId}-${item.businessType}`"
            class="vehicle-item"
            :class="{ 'is-active': isActiveVehicle(item) }"
            @click="onSelectVehicle(item)"
          >
            <div class="vehicle-item__plate">
              {{ item.licensePlate || item.name || `ID ${item.businessId}` }}
            </div>
            <div class="vehicle-item__meta">
              <span v-if="item.name">{{ item.name }}</span>
              <span v-if="item.team">{{ item.team }}</span>
            </div>
            <div class="vehicle-item__time">{{ formatTime(item.reportTime) }}</div>
          </div>
          <el-empty
            v-if="!loading && !filteredVehicles.length"
            :description="mode === 'replay' ? '左侧选择车辆后回放轨迹' : '暂无车辆位置数据'"
            :image-size="60"
          />
        </div>
      </div>
    </aside>

    <!-- 右侧：百度地图 + 回放控制 -->
    <main class="page-main">
      <div class="monitor-map-wrap">
        <div ref="mapRef" class="monitor-map" />
        <div v-if="mapError" class="monitor-map__error">
          <el-empty :description="mapError" :image-size="80" />
        </div>

        <!-- 回放控制浮层：时间轴拖动 + 播放/暂停 -->
        <div v-show="mode === 'replay' && replayPoints.length" class="replay-bar">
          <el-button
            :icon="replayPlaying ? VideoPause : VideoPlay"
            circle
            type="primary"
            :disabled="replayPoints.length < 2"
            @click="togglePlay"
          />
          <div class="replay-bar__slider">
            <el-slider
              v-model="replayIndex"
              :min="0"
              :max="Math.max(replayPoints.length - 1, 0)"
              :disabled="replayPoints.length < 2"
              :format-tooltip="replayTooltip"
              @input="onSliderInput"
            />
          </div>
          <div class="replay-bar__meta">
            <div>{{ currentReplayTime }}</div>
            <div class="replay-bar__count">{{ replayIndex + 1 }}/{{ replayPoints.length }}</div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { computed, nextTick, onActivated, onBeforeUnmount, onDeactivated, onMounted, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { Search, VideoPause, VideoPlay } from "@element-plus/icons-vue";
import TraceAPI, { type LocationRecordItem } from "@/api/trace";
import MapConfigAPI from "@/api/map-config";
import {
  createLabeledMarker,
  fitViewport,
  loadBaiduMap,
  moveMarker,
} from "@/utils/baidu-map";

defineOptions({ name: "Monitor" });

// 轮询与播放节奏外部化（红线：轮询间隔禁硬编码），缺省兜底仅防 env 漏配
const POLL_INTERVAL_MS = Number(import.meta.env.VITE_MAP_POLL_INTERVAL_MS || 10000);
const REPLAY_STEP_MS = Number(import.meta.env.VITE_MAP_REPLAY_STEP_MS || 300);

/** 在途监控监控车辆（type 别名 truck/courier 为接口契约） */
const TRACE_TYPE = "truck" as const;

type MonitorMode = "realtime" | "replay";

const mapRef = ref<HTMLElement>();
const mode = ref<MonitorMode>("realtime");
const keyword = ref("");
const loading = ref(false);
const mapError = ref("");

/** 车辆最新位置列表（latestAll，按上报时间倒序） */
const vehicles = ref<LocationRecordItem[]>([]);

// ===== 地图与实时覆盖物 =====
let BMap: any = null;
let map: any = null;
/** 实时 marker 池：key = businessId#businessType，轮询增量更新位置 */
const markerMap = new Map<string, any>();
let pollTimer: ReturnType<typeof setInterval> | null = null;

// ===== 回放状态 =====
const replayVehicle = ref<LocationRecordItem | null>(null);
const replayPoints = ref<LocationRecordItem[]>([]);
const replayIndex = ref(0);
const replayLoading = ref(false);
const replayPlaying = ref(false);
let replayTimer: ReturnType<typeof setInterval> | null = null;
let replayMarker: any = null;
let replayPolyline: any = null;
let replayStartMark: any = null;
let replayEndMark: any = null;
/** 回放临时覆盖物集合（清场用） */
const replayOverlays: any[] = [];

/** 关键字过滤（车牌/司机/车队） */
const filteredVehicles = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  if (!kw) {
    return vehicles.value;
  }
  return vehicles.value.filter((v) =>
    [v.licensePlate, v.name, v.team].some((field) => field && String(field).toLowerCase().includes(kw))
  );
});

/** 回放当前点时间 */
const currentReplayTime = computed(() => {
  const point = replayPoints.value[replayIndex.value];
  return point ? formatTime(point.reportTime) : "--";
});

function formatTime(value?: string) {
  return String(value ?? "").slice(0, 19) || "--";
}

function pointOf(item: LocationRecordItem): any {
  const lng = Number(item.bd09Lng ?? item.longitude);
  const lat = Number(item.bd09Lat ?? item.latitude);
  return new BMap.Point(lng, lat);
}

/** 判定列表项是否为当前模式下的选中车辆 */
function isActiveVehicle(item: LocationRecordItem) {
  if (mode.value === "replay") {
    return replayVehicle.value?.businessId === item.businessId;
  }
  return activeKey.value === vehicleKey(item);
}

function vehicleKey(item: LocationRecordItem) {
  return `${item.businessId}#${item.businessType}`;
}

const activeKey = ref("");

// ===== 初始化 =====
onMounted(async () => {
  try {
    const { ak } = await MapConfigAPI.get();
    BMap = await loadBaiduMap(ak);
    await nextTick();
    initMap();
    await loadVehicles();
    startPolling();
  } catch (e: any) {
    mapError.value = e?.message || "地图初始化失败";
  }
});

/** keepAlive：激活恢复轮询，失活停轮询停播放 */
onActivated(() => {
  if (map && mode.value === "realtime") {
    startPolling();
  }
});
onDeactivated(() => {
  stopPolling();
  pauseReplay();
});

onBeforeUnmount(() => {
  stopPolling();
  pauseReplay();
  map = null;
});

function initMap() {
  if (!mapRef.value) {
    return;
  }
  map = new BMap.Map(mapRef.value, { enableMapClick: false });
  map.centerAndZoom(new BMap.Point(104.19, 35.86), 5); // 全国视角，加载到数据后自适应
  map.enableScrollWheelZoom(true);
}

async function loadVehicles() {
  loading.value = true;
  try {
    const list = await TraceAPI.latestAll({ type: TRACE_TYPE });
    vehicles.value = (list ?? []).filter((v) => v.bd09Lng != null && v.bd09Lat != null);
    if (map) {
      renderRealtimeMarkers();
    }
  } catch {
    // 拦截器已统一提示，静默保留旧数据
  } finally {
    loading.value = false;
  }
}

function startPolling() {
  stopPolling();
  pollTimer = setInterval(loadVehicles, POLL_INTERVAL_MS);
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
}

/** 实时 marker 增量更新：已存在则挪点，否则创建（含车牌 label 与点击弹窗） */
function renderRealtimeMarkers() {
  for (const item of vehicles.value) {
    const key = vehicleKey(item);
    const point = pointOf(item);
    let marker = markerMap.get(key);
    if (marker) {
      moveMarker(marker, point);
    } else {
      // GL 版 Label 是独立覆盖物（构造需position），不支持 marker.setLabel
      marker = createLabeledMarker(map, BMap, point, item.licensePlate || item.name || "", {
        background: "#2b6cff",
        onClick: () => {
          activeKey.value = key;
          openInfoWindow(item, point);
        },
      });
      markerMap.set(key, marker);
    }
  }
  // 首次有数据时自适应视野
  if (vehicles.value.length && !firstFitDone) {
    firstFitDone = true;
    map.setViewport(vehicles.value.map(pointOf));
  }
}
let firstFitDone = false;

function openInfoWindow(item: LocationRecordItem, point: any) {
  const rows = [
    ["车牌", item.licensePlate],
    ["司机", item.name],
    ["电话", item.phone],
    ["车队", item.team],
    ["上报时间", formatTime(item.reportTime)],
    ["数据来源", item.source],
  ].filter(([, v]) => v != null && v !== "");
  const html = rows
    .map(([k, v]) => `<div class="monitor-iw-row"><span>${k}</span>${v}</div>`)
    .join("");
  map.openInfoWindow(new BMap.InfoWindow(html, { width: 220 }), point);
}

// ===== 交互 =====
function onSelectVehicle(item: LocationRecordItem) {
  if (mode.value === "realtime") {
    const key = vehicleKey(item);
    activeKey.value = key;
    const marker = markerMap.get(key);
    if (marker) {
      const point = pointOf(item);
      map.panTo(point);
      openInfoWindow(item, point);
    }
    return;
  }
  loadReplay(item);
}

// ===== 轨迹回放（F2）=====
async function loadReplay(item: LocationRecordItem) {
  if (replayLoading.value) {
    return;
  }
  replayVehicle.value = item;
  replayLoading.value = true;
  try {
    const points = await TraceAPI.replay({ businessId: String(item.businessId), type: TRACE_TYPE });
    const usable = (points ?? []).filter((p) => p.bd09Lng != null && p.bd09Lat != null);
    if (!usable.length) {
      ElMessage.info("该车暂无轨迹数据");
      return;
    }
    clearReplayOverlays();
    replayPoints.value = usable;
    replayIndex.value = 0;
    drawReplay();
  } catch {
    // 拦截器已统一提示
  } finally {
    replayLoading.value = false;
  }
}

function drawReplay() {
  const points = replayPoints.value.map(pointOf);
  if (points.length >= 2) {
    replayPolyline = new BMap.Polyline(points, {
      strokeColor: "#FF0033",
      strokeWeight: 4,
      strokeOpacity: 0.8,
    });
    map.addOverlay(replayPolyline);
  }
  // 起终点标注：GL 版不支持 marker.setLabel，改为独立 Label 覆盖物
  const startLabel = makeEndpointLabel("起点", points[0]);
  const endLabel = makeEndpointLabel("终点", points[points.length - 1]);
  map.addOverlay(startLabel);
  map.addOverlay(endLabel);
  replayStartMark = startLabel;
  replayEndMark = endLabel;
  replayMarker = new BMap.Marker(points[0]);
  map.addOverlay(replayMarker);
  replayOverlays.push(replayPolyline, startLabel, endLabel, replayMarker);
  fitViewport(map, points);
}

function makeEndpointLabel(text: string, position: any) {
  const label = new BMap.Label(text, { position, offset: new BMap.Size(14, -10) });
  label.setStyle({
    padding: "2px 6px",
    fontSize: "12px",
    color: "#fff",
    background: "#FF0033",
    border: "none",
    borderRadius: "4px",
  });
  return label;
}

function clearReplayOverlays() {
  pauseReplay();
  for (const overlay of replayOverlays) {
    if (overlay) {
      map.removeOverlay(overlay);
    }
  }
  replayOverlays.length = 0;
  replayPolyline = null;
  replayMarker = null;
  replayStartMark = null;
  replayEndMark = null;
  replayPoints.value = [];
  replayIndex.value = 0;
  replayVehicle.value = null;
}

/** 时间轴拖动：直接定位到对应轨迹点
 *  el-slider 的 input 事件在未设 multiple 时发 number，但类型签名是 Arrayable<number>，
 *  这里兼容两种形态（原先声明 number 会导致 vue-tsc 报错、卡死构建）。 */
function onSliderInput(value: number | number[]) {
  const index = Array.isArray(value) ? value[0] : value;
  if (typeof index === "number") {
    moveReplayTo(index);
  }
}

function moveReplayTo(index: number) {
  const point = replayPoints.value[index];
  if (!point || !replayMarker || !map) {
    return;
  }
  replayMarker.setPosition(pointOf(point));
  map.panTo(pointOf(point));
}

function replayTooltip(index: number) {
  return formatTime(replayPoints.value[index]?.reportTime);
}

function togglePlay() {
  if (replayPlaying.value) {
    pauseReplay();
    return;
  }
  // 播到末尾后重新点击从头播放
  if (replayIndex.value >= replayPoints.value.length - 1) {
    replayIndex.value = 0;
    moveReplayTo(0);
  }
  replayPlaying.value = true;
  replayTimer = setInterval(() => {
    if (replayIndex.value >= replayPoints.value.length - 1) {
      pauseReplay();
      return;
    }
    replayIndex.value += 1;
    moveReplayTo(replayIndex.value);
  }, REPLAY_STEP_MS);
}

function pauseReplay() {
  replayPlaying.value = false;
  if (replayTimer) {
    clearInterval(replayTimer);
    replayTimer = null;
  }
}

// ===== 模式切换 =====
watch(mode, (next) => {
  if (next === "replay") {
    // 回放：停轮询、隐藏实时车辆点，避免遮挡轨迹
    stopPolling();
    markerMap.forEach((marker) => marker.hide());
  } else {
    // 回到实时：清回放覆盖物、恢复车辆点与轮询
    clearReplayOverlays();
    activeKey.value = "";
    markerMap.forEach((marker) => marker.show());
    if (map) {
      startPolling();
    }
  }
});
</script>

<style scoped lang="scss">
.monitor-page {
  --page-aside-width: 240px;
}

.monitor-aside {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.monitor-aside__head {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  border-bottom: 1px solid var(--card-border);
}

.monitor-aside__list {
  flex: 1;
  min-height: 0;
  padding: 8px;
  overflow-y: auto;
}

.vehicle-item {
  padding: 8px 10px;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.15s;

  &:hover {
    background: var(--el-fill-color-light);
  }

  &.is-active {
    background: var(--el-color-primary-light-9);

    .vehicle-item__plate {
      color: var(--el-color-primary);
    }
  }

  & + & {
    margin-top: 2px;
  }
}

.vehicle-item__plate {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.vehicle-item__meta {
  display: flex;
  gap: 8px;
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.vehicle-item__time {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.monitor-map-wrap {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: var(--content-bg);
  border: 1px solid var(--card-border);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
}

.monitor-map {
  width: 100%;
  height: 100%;
}

.monitor-map__error {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--content-bg);
}

.replay-bar {
  position: absolute;
  right: 16px;
  bottom: 24px;
  left: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px 16px;
  background: var(--content-bg);
  border: 1px solid var(--card-border);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
}

.replay-bar__slider {
  flex: 1;
}

.replay-bar__meta {
  min-width: 150px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-align: right;
  white-space: nowrap;
}

.replay-bar__count {
  margin-top: 2px;
  color: var(--el-text-color-placeholder);
}
</style>

<style lang="scss">
/* InfoWindow 为百度地图原生 DOM（非 scoped 作用域），全局注入行样式 */
.monitor-iw-row {
  display: flex;
  gap: 8px;
  font-size: 12px;
  line-height: 22px;

  span {
    flex: 0 0 56px;
    color: var(--el-text-color-secondary);
  }
}
</style>
