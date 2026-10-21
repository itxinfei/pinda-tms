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
            role="button"
            tabindex="0"
            :aria-label="`车辆 ${item.licensePlate || item.name || item.businessId}`"
            @click="onSelectVehicle(item)"
            @keydown.enter.prevent="onSelectVehicle(item)"
            @keydown.space.prevent="onSelectVehicle(item)"
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
        <MapContainer class="monitor-map" @ready="onMapReady" />

        <!-- 回放控制浮层：时间轴拖动 + 播放/暂停 + 倍速 + 跟随开关 -->
        <div v-show="mode === 'replay' && replayPoints.length" class="replay-bar">
          <el-button
            :icon="replayPlaying ? VideoPause : VideoPlay"
            circle
            type="primary"
            :disabled="replayPoints.length < 2"
            :aria-label="replayPlaying ? '暂停回放' : '播放回放'"
            @click="togglePlay"
          />
          <div class="replay-bar__slider">
            <el-slider
              v-model="replayProgress"
              :min="0"
              :max="1000"
              :disabled="replayPoints.length < 2"
              :format-tooltip="replayTooltip"
              @input="onSliderInput"
            />
          </div>
          <el-radio-group v-model="replaySpeed" size="small" class="replay-bar__speed">
            <el-radio-button :value="1">1x</el-radio-button>
            <el-radio-button :value="2">2x</el-radio-button>
            <el-radio-button :value="4">4x</el-radio-button>
          </el-radio-group>
          <div class="replay-bar__follow">
            <span class="replay-bar__follow-label">跟随</span>
            <el-switch v-model="followReplay" size="small" aria-label="跟随车辆视角" />
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
import { computed, onActivated, onBeforeUnmount, onDeactivated, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { Search, VideoPause, VideoPlay } from "@element-plus/icons-vue";
import TraceAPI, { type LocationRecordItem } from "@/api/trace";
import MapContainer from "@/components/MapContainer/index.vue";
import { createMapProvider, type LngLat, type MapProvider } from "@/utils/map";
import { MarkerClusterLayer } from "@/utils/map/cluster";

defineOptions({ name: "Monitor" });

// 轮询与播放节奏外部化（红线：轮询间隔禁硬编码），缺省兜底仅防 env 漏配
const POLL_INTERVAL_MS = Number(import.meta.env.VITE_MAP_POLL_INTERVAL_MS || 10000);
/** 回放全程归一播放时长：真实时间轴按比例压放至此时长（点距不均也匀速平滑） */
const REPLAY_TOTAL_MS = Number(import.meta.env.VITE_MAP_REPLAY_TOTAL_MS || 30000);

/** 在途监控监控车辆（type 别名 truck/courier 为接口契约） */
const TRACE_TYPE = "truck" as const;

type MonitorMode = "realtime" | "replay";

const mode = ref<MonitorMode>("realtime");
const keyword = ref("");
const loading = ref(false);

/** 车辆最新位置列表（latestAll，按上报时间倒序） */
const vehicles = ref<LocationRecordItem[]>([]);

// ===== 地图与实时覆盖物 =====
/** 厂商无关地图 provider（MapContainer ready 时注入；vendor 由 map-config 下发，缺省百度） */
let provider: MapProvider = createMapProvider();
let map: any = null;
/** 车辆点聚合层（规范§6.4：>50 点聚合下钻），替代页面手写 marker 池 */
let clusterLayer: MarkerClusterLayer | null = null;
let pollTimer: ReturnType<typeof setInterval> | null = null;

// ===== 回放状态 =====
const replayVehicle = ref<LocationRecordItem | null>(null);
const replayPoints = ref<LocationRecordItem[]>([]);
/** 回放进度（千分位 0~1000，slider 绑定值） */
const replayProgress = ref(0);
/** 当前所在轨迹段索引（时间/计数展示用） */
const replayIndex = ref(0);
const replayLoading = ref(false);
const replayPlaying = ref(false);
/** 倍速（1/2/4，播放中即时生效） */
const replaySpeed = ref(1);
/** 跟随模式：跨段时平移地图保持车标居中；用户拖动地图自动关闭 */
const followReplay = ref(true);
let replayRaf = 0;
/** 已播放时长（按倍速缩放后） */
let replayElapsedMs = 0;
let lastFrameTs = 0;
/** 上次渲染所在段（跨段才刷新双色分界与跟随居中，避免每帧重设路径） */
let lastRenderedSeg = -1;
let replayMarker: any = null;
/** 已行驶（品牌红）/未行驶（灰弱线）双段折线 */
let playedLine: any = null;
let remainLine: any = null;
/** 轨迹渲染坐标缓存（drawReplay 时重建，帧循环内只读） */
let replayCoords: LngLat[] = [];
/** 轨迹段时长（毫秒）：segDurations[i] = 上报时差，无效时戳按 1s 均分兜底 */
let segDurations: number[] = [];
let traceTotalMs = 0;
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

/** 轨迹点 → 渲染坐标（bd09* 为后端已转好的百度底图坐标，缺省回落原始值） */
function lngLatOf(item: { bd09Lng?: number | string | null; bd09Lat?: number | string | null; longitude?: number | string | null; latitude?: number | string | null }): LngLat {
  return {
    lng: Number(item.bd09Lng ?? item.longitude),
    lat: Number(item.bd09Lat ?? item.latitude),
  };
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

// ===== 初始化：SDK/错误/重试收敛在 MapContainer，页面只在 ready 后接管 =====
/** MapContainer 初始化（含重试）成功：接管 provider/map，建聚合层、拉数据、起轮询 */
function onMapReady(payload: { provider: MapProvider; map: any }) {
  provider = payload.provider;
  map = payload.map;
  clusterLayer?.destroy();
  clusterLayer = new MarkerClusterLayer(provider, map);
  bindMapEvents();
  loadVehicles().then(() => startPolling());
}

/** 用户拖动地图 = 主动接管视角：自动退出回放跟随，不再与 panTo 抢地图 */
function bindMapEvents() {
  provider.onMapEvent(map, "dragstart", () => {
    followReplay.value = false;
  });
}

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
  clusterLayer?.destroy();
  clusterLayer = null;
  map = null;
});

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

/** 从 CSS 变量取实际色值（marker label 为 DOM 渲染，canvas/原生覆盖物内联 var() 不生效） */
function getCssVar(name: string, fallback: string): string {
  if (typeof window === "undefined") return fallback;
  return getComputedStyle(document.documentElement).getPropertyValue(name)?.trim() || fallback;
}

/** 实时车辆点全量交给聚合层（散射态增量挪点；聚合态签名变化才重建） */
function renderRealtimeMarkers() {
  clusterLayer?.setPoints(
    vehicles.value.map((item) => ({
      key: vehicleKey(item),
      lngLat: lngLatOf(item),
      label: item.licensePlate || item.name || "",
      background: getCssVar("--el-color-primary", "#FF0033"),
      onClick: () => {
        activeKey.value = vehicleKey(item);
        openInfoWindow(item, lngLatOf(item));
      },
    }))
  );
  // 首次有数据时自适应视野
  if (vehicles.value.length && !firstFitDone) {
    firstFitDone = true;
    provider.fitViewport(map, vehicles.value.map(lngLatOf));
  }
}
let firstFitDone = false;

function openInfoWindow(item: LocationRecordItem, lngLat: LngLat) {
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
  provider.openInfoWindow(map, lngLat, html, { width: 220 });
}

// ===== 交互 =====
function onSelectVehicle(item: LocationRecordItem) {
  if (mode.value === "realtime") {
    const key = vehicleKey(item);
    clusterLayer?.highlight(activeKey.value, false);
    clusterLayer?.highlight(key, true);
    activeKey.value = key;
    const lngLat = lngLatOf(item);
    provider.panTo(map, lngLat);
    openInfoWindow(item, lngLat);
    return;
  }
  loadReplay(item);
}

// ===== 轨迹回放：按上报时间轴匀速插值（替代固定步长跳点） =====
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
    buildTimeAxis();
    drawReplay();
    renderReplayAt(0);
  } catch {
    // 拦截器已统一提示
  } finally {
    replayLoading.value = false;
  }
}

/** "2026-10-17 12:00:00" → 毫秒（iOS Safari 不认空格分隔，统一替换为 T 再解析） */
function parseTimeMs(value?: string): number {
  if (!value) return NaN;
  const ts = Date.parse(String(value).replace(" ", "T"));
  return Number.isNaN(ts) ? NaN : ts;
}

/** 构建时间轴：段时长 = 相邻上报点真实时差；时戳缺失/乱序按 1s 均分兜底（进度仍平滑） */
function buildTimeAxis() {
  const times = replayPoints.value.map((p) => parseTimeMs(p.reportTime));
  segDurations = [];
  traceTotalMs = 0;
  for (let i = 0; i < times.length - 1; i++) {
    let d = times[i + 1] - times[i];
    if (!Number.isFinite(d) || d <= 0) {
      d = 1000;
    }
    segDurations.push(d);
    traceTotalMs += d;
  }
}

function drawReplay() {
  replayCoords = replayPoints.value.map(lngLatOf);
  if (replayCoords.length >= 2) {
    // 已行驶（品牌红实线）+ 未行驶（灰弱线）双段：播放中只 setPath 不重建覆盖物
    playedLine = provider.drawPolyline(map, [replayCoords[0]], {
      strokeColor: "#FF0033",
      strokeWeight: 4,
      strokeOpacity: 0.9,
    });
    remainLine = provider.drawPolyline(map, replayCoords, {
      strokeColor: "#86909C",
      strokeWeight: 3,
      strokeOpacity: 0.35,
    });
  }
  // 起终点标注：绿=起点、红=终点（规范§6.5）；GL 版不支持 marker.setLabel，用独立 Label
  const endpointStyle = { padding: "2px 6px", fontSize: "12px", color: "#fff", border: "none", borderRadius: "4px" };
  const startLabel = provider.createLabel(map, "起点", replayCoords[0], { ...endpointStyle, background: "#00B42A" });
  const endLabel = provider.createLabel(map, "终点", replayCoords[replayCoords.length - 1], { ...endpointStyle, background: "#FF0033" });
  replayMarker = provider.createMarker(map, replayCoords[0]);
  replayOverlays.push(playedLine, remainLine, startLabel, endLabel, replayMarker);
  provider.fitViewport(map, replayCoords);
}

function clearReplayOverlays() {
  pauseReplay();
  for (const overlay of replayOverlays) {
    provider.removeOverlay(map, overlay);
  }
  replayOverlays.length = 0;
  playedLine = null;
  remainLine = null;
  replayMarker = null;
  replayCoords = [];
  segDurations = [];
  traceTotalMs = 0;
  replayElapsedMs = 0;
  lastRenderedSeg = -1;
  replayProgress.value = 0;
  replayPoints.value = [];
  replayIndex.value = 0;
  replayVehicle.value = null;
}

/** 进度(0~1) → 插值坐标与所在段索引：按真实时差在段内线性插值，点距不均也匀速 */
function locateAt(progress: number): { lngLat: LngLat; segIndex: number } {
  const last = replayCoords.length - 1;
  if (last < 0) {
    return { lngLat: { lng: 0, lat: 0 }, segIndex: 0 };
  }
  let target = traceTotalMs * progress;
  let i = 0;
  while (i < last - 1 && target > segDurations[i]) {
    target -= segDurations[i];
    i += 1;
  }
  const d = segDurations[i] || 1;
  const frac = Math.min(Math.max(target / d, 0), 1);
  const from = replayCoords[i];
  const to = replayCoords[i + 1] ?? replayCoords[i];
  return {
    lngLat: { lng: from.lng + (to.lng - from.lng) * frac, lat: from.lat + (to.lat - from.lat) * frac },
    segIndex: i,
  };
}

/** 渲染一帧：车标插值位置；跨段时才刷新双色分界与跟随居中（避免每帧 setPath/panTo） */
function renderReplayAt(progress: number) {
  const { lngLat, segIndex } = locateAt(progress);
  replayProgress.value = Math.round(progress * 1000);
  if (!replayMarker || !map) {
    return;
  }
  provider.moveMarker(replayMarker, lngLat);
  if (segIndex !== lastRenderedSeg) {
    lastRenderedSeg = segIndex;
    replayIndex.value = segIndex;
    if (playedLine && remainLine) {
      provider.setPolylinePath(playedLine, [...replayCoords.slice(0, segIndex + 1), lngLat]);
      provider.setPolylinePath(remainLine, [lngLat, ...replayCoords.slice(segIndex + 1)]);
    }
    if (followReplay.value) {
      provider.panTo(map, lngLat);
    }
  }
}

/** 时间轴拖动：暂停播放并直接定位
 *  el-slider 的 input 事件在未设 multiple 时发 number，但类型签名是 Arrayable<number>，
 *  这里兼容两种形态（原先声明 number 会导致 vue-tsc 报错、卡死构建）。
 *  拖动中若保持播放，rAF 每帧回写进度会与 slider 抢值，故拖动即暂停。 */
function onSliderInput(value: number | number[]) {
  const raw = Array.isArray(value) ? value[0] : value;
  if (typeof raw !== "number") {
    return;
  }
  const progress = raw / 1000;
  pauseReplay();
  replayElapsedMs = progress * REPLAY_TOTAL_MS;
  renderReplayAt(progress);
}

function replayTooltip(value: number | number[]) {
  const raw = Array.isArray(value) ? value[0] : value;
  if (typeof raw !== "number") {
    return "--";
  }
  const { segIndex } = locateAt(raw / 1000);
  return formatTime(replayPoints.value[segIndex]?.reportTime);
}

function togglePlay() {
  if (replayPlaying.value) {
    pauseReplay();
    return;
  }
  // 播到末尾后重新点击从头播放
  if (replayElapsedMs >= REPLAY_TOTAL_MS) {
    replayElapsedMs = 0;
    renderReplayAt(0);
  }
  replayPlaying.value = true;
  lastFrameTs = performance.now();
  const frame = (now: number) => {
    const dt = now - lastFrameTs;
    lastFrameTs = now;
    replayElapsedMs = Math.min(replayElapsedMs + dt * replaySpeed.value, REPLAY_TOTAL_MS);
    renderReplayAt(replayElapsedMs / REPLAY_TOTAL_MS);
    if (replayElapsedMs >= REPLAY_TOTAL_MS) {
      pauseReplay();
      return;
    }
    replayRaf = requestAnimationFrame(frame);
  };
  replayRaf = requestAnimationFrame(frame);
}

function pauseReplay() {
  replayPlaying.value = false;
  if (replayRaf) {
    cancelAnimationFrame(replayRaf);
    replayRaf = 0;
  }
}

// ===== 模式切换 =====
watch(mode, (next) => {
  if (next === "replay") {
    // 回放：停轮询、隐藏实时车辆点（marker 与车牌文字一并隐藏），避免遮挡轨迹
    stopPolling();
    clusterLayer?.setVisible(false);
  } else {
    // 回到实时：清回放覆盖物、恢复车辆点与轮询
    clearReplayOverlays();
    activeKey.value = "";
    clusterLayer?.setVisible(true);
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
  gap: 8px;
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
  padding: 8px 12px;
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

.replay-bar__speed {
  flex-shrink: 0;
}

.replay-bar__follow {
  display: flex;
  flex-shrink: 0;
  gap: 6px;
  align-items: center;
}

.replay-bar__follow-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
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
  font-variant-numeric: tabular-nums;
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
