<template>
  <div class="page-container">
    <div class="dispatch-board">
      <!-- 顶部统计条（三栏共享） -->
      <div class="dispatch-board__stats" role="status" aria-label="调度概览统计">
        <el-tag type="warning" size="small" effect="plain">待调度 {{ summary.pendingOrderCount }}</el-tag>
        <el-tag type="info" size="small" effect="plain">线路 {{ summary.lineCount }}</el-tag>
        <el-tag type="success" size="small" effect="plain">网点 {{ summary.orgCount }}</el-tag>
      </div>

      <div class="dispatch-board__main">
      <!-- 左：待调度订单 -->
      <el-card class="dispatch-board__side" shadow="never">
        <template #header>
          <div class="dispatch-board__side-header">
            <el-tooltip :content="`待调度订单（${selected.length}/${orders.length}）`" placement="top">
              <span>待调度订单（{{ selected.length }}/{{ orders.length }}）</span>
            </el-tooltip>
            <el-button
              v-hasPerm="['dispatch:board:assign']"
              type="primary"
              size="small"
              :disabled="!selected.length"
              @click="openAssign"
            >
              一键派单
            </el-button>
          </div>
        </template>

        <el-input
          v-model="keyword"
          placeholder="搜索订单号 / 发件人 / 收件人"
          clearable
          size="small"
          class="dispatch-board__search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <el-checkbox v-model="selectAll" :disabled="!filteredOrders.length" @change="toggleSelectAll(Boolean($event))">
          全选当前筛选
        </el-checkbox>

        <el-scrollbar class="dispatch-board__list">
          <el-checkbox-group v-model="selected" class="dispatch-board__group">
            <div
              v-for="item in filteredOrders"
              :key="item.id"
              class="dispatch-board__order"
              :class="{ 'is-active': activeOrderId === item.id }"
              role="button"
              tabindex="0"
              :aria-label="`订单 ${item.orderNo || item.id}，${item.senderName || '-'} 至 ${item.receiverName || '-'}`"
              @click="activeOrderId = item.id"
              @keydown.enter.prevent="activeOrderId = item.id"
              @keydown.space.prevent="activeOrderId = item.id"
            >
              <el-checkbox :value="item.id" @click.stop @keydown.space.stop />
              <div class="dispatch-board__order-body">
                <el-tooltip :content="item.orderNo || item.id" placement="top">
                  <div class="dispatch-board__order-no">{{ item.orderNo || item.id }}</div>
                </el-tooltip>
                <el-tooltip
                  :content="`${item.senderName || '-'} → ${item.receiverName || '-'}`"
                  placement="top"
                >
                  <div class="dispatch-board__order-meta">
                    {{ item.senderName || "-" }} → {{ item.receiverName || "-" }}
                  </div>
                </el-tooltip>
                <div class="dispatch-board__order-org">
                  <el-tag size="small" type="info" effect="plain">
                    {{ item.currentOrgName || item.currentOrgId || "-" }}
                  </el-tag>
                  <span v-if="!hasCoord(item)" class="dispatch-board__order-warn">无坐标</span>
                </div>
                <!-- 货物摘要（board 聚合 quantity/totalWeight/totalVolume） -->
                <div class="dispatch-board__order-cargo" :class="{ 'is-empty': !hasCargo(item) }">
                  {{ cargoSummary(item) }}
                </div>
              </div>
            </div>
          </el-checkbox-group>
          <el-empty v-if="!filteredOrders.length" description="暂无待调度订单" :image-size="60" />
        </el-scrollbar>

        <!-- 勾选聚合条：右栏载重匹配的输入源 -->
        <div v-if="selected.length" class="dispatch-board__agg" aria-live="polite">
          已选 {{ selected.length }} 单 · 总重 {{ formatNum(selectedWeight) }} kg · 总体积
          {{ formatNum(selectedVolume) }} m³
        </div>
      </el-card>

      <!-- 右：地图 -->
      <el-card class="dispatch-board__map-card" shadow="never">
        <template #header>
          <div class="dispatch-board__map-header">
            <span class="dispatch-board__title">调度工作台</span>
            <div class="dispatch-board__legend">
              <span><i class="dot dot--order" />待调度订单 {{ orders.length }}</span>
              <span><i class="dot dot--truck" />在途车辆 {{ trucks.length }}</span>
              <span><i class="dot dot--line" />线路 {{ lines.length }}</span>
              <el-tag v-if="activeTruck" type="success" size="small" effect="dark">
                已选车辆：{{ truckLabel(activeTruck) }}
              </el-tag>
              <el-button size="small" :icon="Refresh" @click="loadAll">刷新</el-button>
              <el-button size="small" @click="showTruckPanel = !showTruckPanel">
                {{ showTruckPanel ? "收起车辆栏" : "展开车辆栏" }}
              </el-button>
            </div>
          </div>
        </template>

        <el-alert
          v-if="mapError"
          type="warning"
          :closable="false"
          show-icon
          :title="mapError"
          class="dispatch-board__alert"
        />
        <div v-else ref="mapRef" v-loading="loading" class="dispatch-board__map"></div>
      </el-card>

      <!-- 右：候选车辆（实时位置 ⋈ 档案载重匹配） -->
      <el-card v-show="showTruckPanel" class="dispatch-board__trucks" shadow="never">
        <template #header>
          <div class="dispatch-board__trucks-header">
            <span>候选车辆（{{ visibleTruckCards.length }}/{{ truckCards.length }}）</span>
            <el-checkbox v-model="assignableOnly" size="small">仅看可派</el-checkbox>
          </div>
        </template>
        <el-scrollbar class="dispatch-board__truck-list">
          <div
            v-for="card in visibleTruckCards"
            :key="card.key"
            class="dispatch-board__truck"
            :class="{ 'is-active': isActiveTruck(card), 'is-disabled': !card.selectable }"
            role="button"
            tabindex="0"
            :aria-label="`${card.label}，${card.statusLabel}，${card.matchLabel}`"
            :title="card.disabledReason || undefined"
            @click="pickTruckCard(card)"
            @keydown.enter.prevent="pickTruckCard(card)"
          >
            <div class="dispatch-board__truck-top">
              <span class="dispatch-board__truck-plate">{{ card.label }}</span>
              <el-tag :type="card.statusTag" size="small" effect="plain">{{ card.statusLabel }}</el-tag>
            </div>
            <div class="dispatch-board__truck-meta">{{ card.fleetName || "-" }} · {{ card.truckTypeName || "-" }}</div>
            <div class="dispatch-board__truck-load">
              载重 {{ formatNum(card.allowableLoad, "未知") }} kg / 容积
              {{ formatNum(card.allowableVolume, "未知") }} m³
            </div>
            <div class="dispatch-board__truck-badges">
              <el-tag :type="card.matchTag" size="small" effect="light">{{ card.matchLabel }}</el-tag>
              <el-tag v-if="card.onDuty" type="warning" size="small" effect="plain">在途</el-tag>
            </div>
          </div>
          <el-empty v-if="!visibleTruckCards.length" description="暂无候选车辆" :image-size="60" />
        </el-scrollbar>
        <div v-if="activeTruck" class="dispatch-board__truck-selected" aria-live="polite">
          已选：{{ truckLabel(activeTruck) }}
        </div>
      </el-card>
      </div>
    </div>

    <!-- 派单确认 -->
    <el-dialog v-model="assignVisible" title="一键派单" width="520px" append-to-body @closed="assignVisible = false">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="待调度订单">
          {{ selected.length }} 张<template v-if="startOrgName"> （起始网点：{{ startOrgName }}）</template>
        </el-descriptions-item>
        <el-descriptions-item label="货物合计">
          {{ formatNum(selectedWeight) }} kg / {{ formatNum(selectedVolume) }} m³
        </el-descriptions-item>
        <el-descriptions-item label="承运车辆">
          <el-select
            v-model="selectedTruckId"
            filterable
            remote
            clearable
            reserve-keyword
            size="small"
            placeholder="输入车牌搜索并选择"
            :remote-method="searchTrucks"
            :loading="truckLoading"
            class="dispatch-board__truck-select"
          >
            <el-option
              v-for="truck in truckOptions"
              :key="truck.id"
              :label="truck.licensePlate || truck.id"
              :value="String(truck.id)"
            >
              <span>{{ truck.licensePlate || truck.id }}</span>
              <span class="dispatch-board__truck-option-meta">
                {{ truck.fleet?.name || truck.truckType?.name || "" }}
              </span>
            </el-option>
          </el-select>
          <div v-if="activeTruck" class="dispatch-board__truck-hint">
            已由车辆栏/地图点选：{{ truckLabel(activeTruck) }}
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="目的网点">{{ targetLine?.endOrgName || "按线路自动匹配" }}</el-descriptions-item>
        <el-descriptions-item v-if="targetLine" label="线路">
          {{ targetLine.name }}（{{ targetLine.distance ?? "-" }} 公里）
        </el-descriptions-item>
      </el-descriptions>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="派单将创建运输单并生成运输任务，订单进入待分配状态"
        class="dispatch-board__assign-alert"
      />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="doAssign">确认派单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { computed, nextTick, onActivated, onBeforeUnmount, onDeactivated, onMounted, ref, shallowRef, watch } from "vue";
import { Refresh, Search } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import DispatchAPI, {
  type DispatchAssignResult,
  type DispatchBoard,
  type DispatchLine,
  type DispatchOrderPoint,
} from "@/api/dispatch";
import TraceAPI, { type LocationRecordItem } from "@/api/trace";
import TruckAPI, { type TruckRecord } from "@/api/truck";
import MapConfigAPI from "@/api/map-config";
import {
  createLabeledMarker,
  fitViewport,
  highlightMarker,
  loadBaiduMap,
  moveMarker,
  wgs84ToBd09,
} from "@/utils/baidu-map";

/** 车辆轮询间隔外部化（红线：轮询间隔禁硬编码），与在途监控共用同一 env，缺省兜底仅防 env 漏配 */
const POLL_INTERVAL_MS = Number(import.meta.env.VITE_MAP_POLL_INTERVAL_MS || 10000);

/** GPS 在线判定阈值（分钟）外部化，与后端 gps.offline.timeout.minutes 同源口径；缺省兜底仅防 env 漏配 */
const GPS_OFFLINE_TIMEOUT_MS = Number(import.meta.env.VITE_GPS_OFFLINE_TIMEOUT_MINUTES || 30) * 60_000;

/** 从 CSS 变量取实际色值（Polyline 为 canvas 渲染，内联 var() 不生效；DOM label 可直接用 var()） */
function cssVar(name: string, fallback: string): string {
  const value = getComputedStyle(document.documentElement).getPropertyValue(name)?.trim();
  return value || fallback;
}

const mapRef = ref<HTMLElement>();
/** 地图实例不进响应式（避免深层代理影响 BMap 内部状态） */
const map = shallowRef<any>(null);
let BMap: any = null;

const loading = ref(false);
const assigning = ref(false);
const mapError = ref("");
const orders = ref<DispatchOrderPoint[]>([]);
const lines = ref<DispatchLine[]>([]);
const trucks = ref<LocationRecordItem[]>([]);
const summary = ref({ pendingOrderCount: 0, lineCount: 0, orgCount: 0 });

const keyword = ref("");
const selected = ref<string[]>([]);
const selectAll = ref(false);
const activeOrderId = ref("");
const activeTruck = ref<LocationRecordItem | null>(null);
const assignVisible = ref(false);
/** 派单弹窗里的车辆下拉：地图点选只是快捷方式，最终以本值为准 */
const truckOptions = ref<TruckRecord[]>([]);
const truckLoading = ref(false);
const selectedTruckId = ref("");
/** 右栏候选车辆面板（窄屏默认收起，onMounted 判定） */
const showTruckPanel = ref(true);
/** 仅看可派：隐藏离线/无定位/超限车辆，默认开 */
const assignableOnly = ref(true);
/** 车辆档案（TruckAPI 分页全量，补 latestAll 缺失的载重/容积/车队），key=truckId */
const truckArchives = ref<Map<string, TruckRecord>>(new Map());

let pollTimer: ReturnType<typeof setInterval> | null = null;
const orderMarkers = new Map<string, any>();
const truckMarkers = new Map<string, any>();
let lineOverlay: any = null;

const filteredOrders = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  if (!kw) return orders.value;
  return orders.value.filter((item) =>
    [item.orderNo, item.id, item.senderName, item.receiverName, item.currentOrgName]
      .filter(Boolean)
      .some((field) => String(field).toLowerCase().includes(kw))
  );
});

/** 待调度订单的起始网点（勾选订单须同机构，否则线路无法确定） */
const startOrgId = computed(() => {
  const first = orders.value.find((item) => selected.value.includes(item.id));
  return first?.currentOrgId ?? "";
});
const startOrgName = computed(
  () => orders.value.find((item) => item.currentOrgId === startOrgId.value)?.currentOrgName ?? ""
);
/** 起始网点的第一条可用线路（派单目的网点） */
const targetLine = computed<DispatchLine | undefined>(() =>
  lines.value.find((line) => line.startOrgId === startOrgId.value)
);

/** 数值格式化：空值回退占位（载重/容积档案缺失时显示"未知"） */
function formatNum(value: number | undefined | null, fallback = "0"): string {
  if (value == null || !Number.isFinite(Number(value))) return fallback;
  const num = Number(value);
  return num % 1 === 0 ? String(num) : num.toFixed(1);
}

/** 货物汇总是否有效（board 聚合 quantity/totalWeight/totalVolume，无数据显示 —） */
function hasCargo(item: DispatchOrderPoint): boolean {
  return Number(item.quantity) > 0 || Number(item.totalWeight) > 0 || Number(item.totalVolume) > 0;
}

function cargoSummary(item: DispatchOrderPoint): string {
  if (!hasCargo(item)) return "—";
  const parts: string[] = [];
  if (Number(item.quantity) > 0) parts.push(`${formatNum(item.quantity)}件`);
  if (Number(item.totalWeight) > 0) parts.push(`${formatNum(item.totalWeight)}kg`);
  if (Number(item.totalVolume) > 0) parts.push(`${formatNum(item.totalVolume)}m³`);
  return parts.join(" / ") || "—";
}

/** 勾选订单货物聚合（右栏载重匹配的输入源） */
const selectedWeight = computed(() =>
  orders.value
    .filter((item) => selected.value.includes(item.id))
    .reduce((sum, item) => sum + (Number(item.totalWeight) || 0), 0)
);
const selectedVolume = computed(() =>
  orders.value
    .filter((item) => selected.value.includes(item.id))
    .reduce((sum, item) => sum + (Number(item.totalVolume) || 0), 0)
);

// ===== 右栏：候选车辆载重匹配 =====
/** 右栏卡片视图模型（latestAll 实时位置 ⋈ TruckAPI 档案合并后的展示态） */
interface TruckCardVM {
  key: string;
  label: string;
  /** latestAll 实时记录（有定位），无定位车辆为 null */
  live: LocationRecordItem | null;
  online: boolean;
  statusLabel: string;
  statusTag: "success" | "info" | "danger";
  matchLabel: string;
  matchTag: "success" | "danger" | "warning" | "info";
  /** 硬约束：超限（DSP-04 口径）+ 离线/无定位（R2-2/FR-36 同口径）禁选 */
  selectable: boolean;
  disabledReason: string;
  onDuty: boolean;
  allowableLoad?: number;
  allowableVolume?: number;
  fleetName?: string;
  truckTypeName?: string;
}

function isTruckOnline(item: LocationRecordItem): boolean {
  const time = Date.parse(String(item.reportTime || "").replace(/-/g, "/"));
  if (!Number.isFinite(time)) return false;
  return Date.now() - time <= GPS_OFFLINE_TIMEOUT_MS;
}

const truckCards = computed<TruckCardVM[]>(() => {
  const cards: TruckCardVM[] = [];
  // 1) 有实时定位的车辆，档案补载重/容积/车队
  trucks.value.forEach((item) => {
    const truckId = item.businessId == null ? "" : String(item.businessId);
    const archive = truckArchives.value.get(truckId);
    const online = isTruckOnline(item);
    const load = archive?.allowableLoad;
    const volume = archive?.allowableVolume;
    let matchLabel = "载重未知";
    let matchTag: TruckCardVM["matchTag"] = "info";
    let overLimit = false;
    if (load != null && Number(load) > 0) {
      if (selectedWeight.value > Number(load)) {
        matchLabel = "超重";
        matchTag = "danger";
        overLimit = true;
      } else if (volume != null && Number(volume) > 0 && selectedVolume.value > Number(volume)) {
        matchLabel = "超容积";
        matchTag = "warning";
        overLimit = true;
      } else {
        matchLabel = "可装";
        matchTag = "success";
      }
    }
    const onDuty = Boolean(item.transportTaskId);
    cards.push({
      key: `${item.businessId}#${item.businessType}`,
      label: item.licensePlate || item.name || truckId || "-",
      live: item,
      online,
      statusLabel: online ? "在线" : "离线",
      statusTag: online ? "success" : "danger",
      matchLabel,
      matchTag,
      selectable: online && !overLimit,
      disabledReason: !online
        ? "GPS 离线，禁止派单（FR-36 口径）"
        : overLimit
          ? "已选订单超出准载限制，禁止派单（DSP-04 口径）"
          : "",
      onDuty,
      allowableLoad: load,
      allowableVolume: volume,
      fleetName: archive?.fleet?.name,
      truckTypeName: archive?.truckType?.name,
    });
  });
  // 2) 有档案但无实时定位的车辆：无定位禁选，置底保留档案可见性
  truckArchives.value.forEach((archive, truckId) => {
    if (trucks.value.some((item) => String(item.businessId ?? "") === truckId)) return;
    cards.push({
      key: `archive#${truckId}`,
      label: archive.licensePlate || truckId,
      live: null,
      online: false,
      statusLabel: "无定位",
      statusTag: "info",
      matchLabel: "载重未知",
      matchTag: "info",
      selectable: false,
      disabledReason: "无实时定位，禁止派单（FR-36 口径）",
      onDuty: false,
      allowableLoad: archive.allowableLoad,
      allowableVolume: archive.allowableVolume,
      fleetName: archive.fleet?.name,
      truckTypeName: archive.truckType?.name,
    });
  });
  // 排序：可派 → 在途 → 不可派（离线/超限/无定位）
  const rank = (card: TruckCardVM) => (card.selectable ? (card.onDuty ? 1 : 0) : 2);
  return cards.sort((a, b) => rank(a) - rank(b));
});

const visibleTruckCards = computed(() =>
  assignableOnly.value ? truckCards.value.filter((card) => card.selectable) : truckCards.value
);

function isActiveTruck(card: TruckCardVM): boolean {
  return Boolean(
    activeTruck.value && card.live && `${activeTruck.value.businessId}#${activeTruck.value.businessType}` === card.key
  );
}

/** 右栏点选车辆（与地图点选同一入口，回写派单下拉） */
function pickTruckCard(card: TruckCardVM) {
  if (!card.selectable) {
    ElMessage.warning(card.disabledReason || "该车辆当前不可派");
    return;
  }
  if (card.live) {
    selectTruck(card.live);
  }
}

function hasCoord(item: DispatchOrderPoint): boolean {
  return Number.isFinite(Number(item.longitude)) && Number.isFinite(Number(item.latitude));
}

// ===== 数据加载 =====
async function loadBoard() {
  const data: DispatchBoard = await DispatchAPI.board();
  orders.value = data?.orders ?? [];
  lines.value = data?.lines ?? [];
  summary.value = data?.summary ?? { pendingOrderCount: 0, lineCount: 0, orgCount: 0 };
}

async function loadTrucks() {
  // 车辆实时点由 xf-netty 公开端点提供（定位职责单一，不经调度聚合转发），出参已附 BD09 坐标
  const list = await TraceAPI.latestAll({ type: "truck" });
  trucks.value = list ?? [];
}

/** 车辆档案全量（分页拉取，补载重/容积/车队供右栏匹配；档案变更低频，仅首载+手动刷新） */
async function loadTruckArchives() {
  const res = await TruckAPI.page({ page: 1, pageSize: 200 });
  const map = new Map<string, TruckRecord>();
  (res?.items ?? []).forEach((item) => map.set(String(item.id), item));
  truckArchives.value = map;
}

async function loadAll() {
  loading.value = true;
  try {
    await loadBoard();
    await loadTrucks();
    // 档案拉取失败不阻断工作台：右栏降级为"载重未知"
    await loadTruckArchives().catch(() => {});
    await nextTick();
    renderAll();
  } finally {
    loading.value = false;
  }
}

function startPolling() {
  stopPolling();
  pollTimer = setInterval(async () => {
    try {
      await loadTrucks();
      renderTrucks();
    } catch {
      // 拦截器统一提示，保留旧数据
    }
  }, POLL_INTERVAL_MS);
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
}

// ===== 地图渲染 =====
async function initMap() {
  if (!mapRef.value) return;
  const { ak } = await MapConfigAPI.get();
  if (!ak) {
    mapError.value = "未配置百度地图 AK（后端 map-config-manager 返回 ak 为空）";
    return;
  }
  BMap = await loadBaiduMap(ak);
  const instance = new BMap.Map(mapRef.value, { enableMapClick: false });
  instance.centerAndZoom(new BMap.Point(104.19, 35.86), 5);
  instance.enableScrollWheelZoom(true);
  map.value = instance;
}

function renderAll() {
  renderOrders();
  renderTrucks();
  renderLines();
  fitView();
}

function renderOrders() {
  const instance = map.value;
  if (!instance) return;
  // 清掉旧点，避免筛选后残留
  orderMarkers.forEach((marker) => instance.removeOverlay(marker));
  orderMarkers.clear();

  orders.value.forEach((item) => {
    if (!hasCoord(item)) return;
    // 网点坐标 WGS84 落库，百度地图渲染前转 BD09，否则数百米偏移
    const bd = wgs84ToBd09(Number(item.longitude), Number(item.latitude));
    const point = new BMap.Point(bd.lng, bd.lat);
    const marker = createLabeledMarker(instance, BMap, point,
      `${item.orderNo || item.id}（${item.currentOrgName ?? ""}）`, {
        background: "#f56c6c",
        offset: [12, -12],
        onClick: () => {
          activeOrderId.value = item.id;
        },
      });
    orderMarkers.set(item.id, marker);
  });
}

function renderTrucks() {
  const instance = map.value;
  if (!instance) return;

  trucks.value.forEach((item) => {
    const lng = Number(item.bd09Lng);
    const lat = Number(item.bd09Lat);
    if (!Number.isFinite(lng) || !Number.isFinite(lat)) return;
    const key = `${item.businessId}#${item.businessType}`;
    const point = new BMap.Point(lng, lat);
    const exist = truckMarkers.get(key);
    if (exist) {
      moveMarker(exist, point);
      return;
    }
    const marker = createLabeledMarker(instance, BMap, point, item.licensePlate || item.name || "车辆", {
      background: cssVar("--el-color-primary", "#FF0033"),
      onClick: () => selectTruck(item),
    });
    truckMarkers.set(key, marker);
  });
  syncTruckHighlight();
}

/** 车辆标注显示名 */
function truckLabel(item: LocationRecordItem): string {
  return item.licensePlate || item.name || (item.businessId == null ? "-" : String(item.businessId));
}

/**
 * 选中车辆（地图点选的快捷入口）
 *
 * 同时回写派单弹窗的下拉，避免「地图选了、下拉还空」的两处状态不一致；
 * 选中后给标注描边高亮，否则用户无法确认自己到底点没点上。
 */
function selectTruck(item: LocationRecordItem) {
  activeTruck.value = item;
  selectedTruckId.value = item.businessId == null ? "" : String(item.businessId);
  syncTruckHighlight();
  ElMessage.success(`已选择车辆 ${truckLabel(item)}`);
}

/** 同步地图车辆标注的选中态描边 */
function syncTruckHighlight() {
  const activeKey = activeTruck.value
    ? `${activeTruck.value.businessId}#${activeTruck.value.businessType}`
    : "";
  truckMarkers.forEach((marker, key) => highlightMarker(marker, key === activeKey));
}

/** 车辆下拉远程搜索（车牌模糊） */
async function searchTrucks(keyword: string) {
  truckLoading.value = true;
  try {
    const res = await TruckAPI.page({
      page: 1,
      pageSize: 50,
      licensePlate: keyword?.trim() || undefined,
    });
    truckOptions.value = res?.items ?? [];
  } finally {
    truckLoading.value = false;
  }
}

function renderLines() {
  const instance = map.value;
  if (!instance) return;
  if (lineOverlay) {
    instance.removeOverlay(lineOverlay);
    lineOverlay = null;
  }
  const points: any[] = [];
  lines.value.forEach((line) => {
    const start = lineOrgCoord(line.startOrgId, line.startOrgLng, line.startOrgLat);
    const end = lineOrgCoord(line.endOrgId, line.endOrgLng, line.endOrgLat);
    if (start && end) {
      points.push(start, end);
    }
  });
  if (points.length >= 2) {
    lineOverlay = new BMap.Polyline(points, {
      // canvas 渲染取 CSS 变量实际色值，跟随主题（品牌色统一心飞红）
      strokeColor: cssVar("--el-color-info", "#909399"),
      strokeWeight: 2,
      strokeOpacity: 0.5,
    });
    instance.addOverlay(lineOverlay);
  }
}

/** 线路端点坐标：优先线路自带机构坐标，缺失时回退从待调度订单反查（同一 WGS84 源，渲染前转 BD09） */
function lineOrgCoord(orgId: string | undefined, lng?: string, lat?: string): any | null {
  if (orgId && Number.isFinite(Number(lng)) && Number.isFinite(Number(lat))) {
    const bd = wgs84ToBd09(Number(lng), Number(lat));
    return new BMap.Point(bd.lng, bd.lat);
  }
  if (orgId) {
    const order = orders.value.find((item) => item.currentOrgId === orgId);
    if (order && hasCoord(order)) {
      const bd = wgs84ToBd09(Number(order.longitude), Number(order.latitude));
      return new BMap.Point(bd.lng, bd.lat);
    }
  }
  return null;
}

function fitView() {
  const instance = map.value;
  if (!instance) return;
  const points: any[] = [];
  orderMarkers.forEach((marker) => {
    const position = marker?.getPosition?.();
    if (position) points.push(position);
  });
  truckMarkers.forEach((marker) => {
    const position = marker?.getPosition?.();
    if (position) points.push(position);
  });
  fitViewport(instance, points);
}

// ===== 交互 =====
function toggleSelectAll(checked: boolean) {
  selected.value = checked ? filteredOrders.value.map((item) => item.id) : [];
}

watch(selected, () => {
  const ids = new Set(filteredOrders.value.map((item) => item.id));
  selectAll.value = selected.value.length > 0 && selected.value.every((id) => ids.has(id));
});

function openAssign() {
  if (!selected.value.length) {
    ElMessage.warning("请先勾选待调度订单");
    return;
  }
  // 勾选订单须同一起始网点，否则后端无法确定唯一线路
  const orgIds = new Set(
    orders.value.filter((item) => selected.value.includes(item.id)).map((item) => item.currentOrgId)
  );
  if (orgIds.size > 1) {
    ElMessage.warning("勾选订单须为同一网点的入库订单，请按网点分别派单");
    return;
  }
  // 车辆改为弹窗内下拉选择，地图上已点选时作为默认值带出（地图点选只是快捷方式）
  if (activeTruck.value) {
    selectedTruckId.value = String(activeTruck.value.businessId ?? "");
  }
  if (!truckOptions.value.length) {
    searchTrucks("");
  }
  assignVisible.value = true;
}

async function doAssign() {
  const truckId =
    selectedTruckId.value ||
    (activeTruck.value ? String(activeTruck.value.businessId ?? "") : "");
  if (!truckId) {
    ElMessage.warning("请选择承运车辆");
    return;
  }
  // 兜底下拉选择的载重硬约束校验（右栏点选已在卡片层拦截；档案缺数据不阻断）
  const archive = truckArchives.value.get(truckId);
  if (archive?.allowableLoad != null && Number(archive.allowableLoad) > 0) {
    if (selectedWeight.value > Number(archive.allowableLoad)) {
      ElMessage.warning(
        `所选车辆准载 ${formatNum(archive.allowableLoad)} kg，超出已选订单总重，禁止派单`
      );
      return;
    }
    if (
      archive.allowableVolume != null &&
      Number(archive.allowableVolume) > 0 &&
      selectedVolume.value > Number(archive.allowableVolume)
    ) {
      ElMessage.warning(
        `所选车辆准载体积 ${formatNum(archive.allowableVolume)} m³，超出已选订单总体积，禁止派单`
      );
      return;
    }
  }
  assigning.value = true;
  try {
    const result = (await DispatchAPI.assign({
      orderIds: selected.value,
      truckId,
    })) as DispatchAssignResult;
    ElMessage.success(`派单成功：生成 ${result?.transportOrderCount ?? 0} 张运输单`);
    assignVisible.value = false;
    selected.value = [];
    await loadBoard();
    renderOrders();
    fitView();
  } finally {
    assigning.value = false;
  }
}

// ===== 生命周期 =====
onMounted(async () => {
  // 窄屏默认收起右栏（规格：三栏 <1440px 可折叠）
  showTruckPanel.value = window.innerWidth >= 1440;
  try {
    await initMap();
  } catch (error: any) {
    mapError.value = error?.message || "地图初始化失败";
  }
  await loadAll();
  startPolling();
});

// keepAlive：激活恢复数据与轮询，失活停轮询（与在途监控同模式）
onActivated(async () => {
  if (map.value) {
    await loadTrucks();
    renderTrucks();
  }
  startPolling();
});
onDeactivated(() => {
  stopPolling();
});

onBeforeUnmount(() => {
  stopPolling();
  map.value = null;
});
</script>

<style lang="scss" scoped>
.dispatch-board {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: calc(100vh - 130px);

  // 三栏主体：左订单 / 中地图 / 右候选车辆
  &__main {
    display: flex;
    gap: 12px;
    flex: 1;
    min-height: 0;
  }

  &__side {
    display: flex;
    flex-shrink: 0;
    flex-direction: column;
    width: 312px;
  }

  &__side-header {
    display: flex;
    gap: 8px;
    align-items: center;
    justify-content: space-between;

    // 标题「待调度订单（0/2）」：窄空间省略防与按钮挤压，计数数字等宽不跳动
    > span {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      font-variant-numeric: tabular-nums;
      white-space: nowrap;
    }

    :deep(.el-button) {
      flex-shrink: 0;
    }
  }

  &__stats {
    display: flex;
    flex-shrink: 0;
    flex-wrap: wrap;
    gap: 8px;

    // 统计数字等宽，刷新跳变时标签不抖
    :deep(.el-tag) {
      font-variant-numeric: tabular-nums;
    }
  }

  &__search {
    margin-bottom: 8px;
  }

  &__list {
    flex: 1;
    min-height: 0;
    margin-top: 8px;
  }

  &__group {
    display: flex;
    flex-direction: column;
    gap: 8px;
    width: 100%;
  }

  &__order {
    display: flex;
    gap: 8px;
    align-items: flex-start;
    padding: 8px;
    cursor: pointer;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 6px;

    // 勾选框与首行文字顶对齐（checkbox 默认固定高度会把首行往下顶）
    :deep(.el-checkbox) {
      height: auto;
      margin-right: 0;
    }

    &.is-active {
      background: var(--el-color-primary-light-9);
      border-color: var(--el-color-primary);
    }
  }

  &__order-body {
    flex: 1;
    min-width: 0;
  }

  // 长订单号单行省略，防撑破 320px 侧栏
  &__order-no {
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 13px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
  }

  // 发件人→收件人：块级单行省略。
  // 此前用 display:flex 包纯文本节点，文本失去换行/省略能力，长名字溢出挤压即「文字乱」根因
  &__order-meta {
    margin-top: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
  }

  &__order-org {
    display: flex;
    gap: 4px;
    align-items: center;
    margin-top: 4px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__order-warn {
    color: var(--el-color-warning);
  }

  // 订单卡货物摘要（board 聚合：件数/重量/体积）
  &__order-cargo {
    margin-top: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 12px;
    font-variant-numeric: tabular-nums;
    color: var(--el-text-color-regular);
    white-space: nowrap;

    &.is-empty {
      color: var(--el-text-color-placeholder);
    }
  }

  // 勾选聚合条（右栏载重匹配的输入源）
  &__agg {
    flex-shrink: 0;
    padding: 6px 8px;
    margin-top: 8px;
    overflow: hidden;
    font-size: 12px;
    font-variant-numeric: tabular-nums;
    color: var(--el-color-primary);
    text-overflow: ellipsis;
    white-space: nowrap;
    background: var(--el-color-primary-light-9);
    border-radius: 6px;
  }

  // 右栏：候选车辆
  &__trucks {
    display: flex;
    flex-direction: column;
    flex-shrink: 0;
    width: 312px;
  }

  &__trucks-header {
    display: flex;
    align-items: center;
    justify-content: space-between;

    > span {
      overflow: hidden;
      font-variant-numeric: tabular-nums;
      white-space: nowrap;
    }
  }

  &__truck-list {
    flex: 1;
    min-height: 0;
  }

  &__truck {
    padding: 8px;
    margin-bottom: 8px;
    cursor: pointer;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 6px;

    &.is-active {
      background: var(--el-color-primary-light-9);
      border-color: var(--el-color-primary);
    }

    // 不可派车辆弱化展示，悬停提示禁选原因
    &.is-disabled {
      opacity: 0.55;
      cursor: not-allowed;
    }
  }

  &__truck-top {
    display: flex;
    gap: 8px;
    align-items: center;
    justify-content: space-between;
  }

  &__truck-plate {
    overflow: hidden;
    font-size: 13px;
    font-weight: 600;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  &__truck-meta {
    margin-top: 4px;
    overflow: hidden;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  &__truck-load {
    margin-top: 4px;
    font-size: 12px;
    font-variant-numeric: tabular-nums;
    color: var(--el-text-color-regular);
  }

  &__truck-badges {
    display: flex;
    gap: 6px;
    margin-top: 6px;
  }

  &__truck-selected {
    flex-shrink: 0;
    padding: 6px 8px;
    margin-top: 8px;
    overflow: hidden;
    font-size: 12px;
    white-space: nowrap;
    text-overflow: ellipsis;
    color: var(--el-color-success);
    background: var(--el-color-success-light-9);
    border-radius: 6px;
  }

  &__map-card {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-width: 0;
  }

  &__map-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__title {
    font-weight: 600;
  }

  &__legend {
    display: flex;
    gap: 12px;
    align-items: center;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__map {
    height: 100%;
    min-height: 480px;
    border-radius: 6px;
  }

  &__alert {
    margin-bottom: 8px;
  }

  &__assign-alert {
    margin-top: 12px;
  }

  // 派单弹窗车辆下拉：撑满描述项宽度，避免窄栏被挤成两三字符
  &__truck-select {
    width: 100%;
    max-width: 320px;
  }

  // 下拉项右侧的车队/车型副标题，右对齐弱化
  &__truck-option-meta {
    float: right;
    margin-left: 12px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__truck-hint {
    margin-top: 4px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 4px;
  border-radius: 50%;

  &--order {
    background: var(--el-color-danger);
  }

  &--truck {
    background: var(--el-color-primary);
  }

  &--line {
    background: var(--el-color-info);
  }
}
</style>