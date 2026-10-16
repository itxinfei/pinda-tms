<template>
  <div class="page-container">
    <div class="dispatch-board">
      <!-- 左：待调度订单 -->
      <el-card class="dispatch-board__side" shadow="never">
        <template #header>
          <div class="dispatch-board__side-header">
            <span>待调度订单（{{ selected.length }}/{{ orders.length }}）</span>
            <el-button
              v-hasPerm="['dispatch:board:assign']"
              type="primary"
              size="small"
              :disabled="!selected.length || !activeTruck"
              @click="openAssign"
            >
              一键派单
            </el-button>
          </div>
        </template>

        <div class="dispatch-board__stats">
          <el-tag type="warning" size="small" effect="plain">
            待调度 {{ summary.pendingOrderCount }}
          </el-tag>
          <el-tag type="info" size="small" effect="plain">线路 {{ summary.lineCount }}</el-tag>
          <el-tag type="success" size="small" effect="plain">网点 {{ summary.orgCount }}</el-tag>
        </div>

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
              @click="activeOrderId = item.id"
            >
              <el-checkbox :value="item.id" @click.stop />
              <div class="dispatch-board__order-body">
                <div class="dispatch-board__order-no">{{ item.orderNo || item.id }}</div>
                <div class="dispatch-board__order-meta">
                  {{ item.senderName || "-" }} → {{ item.receiverName || "-" }}
                </div>
                <div class="dispatch-board__order-org">
                  <el-tag size="small" type="info" effect="plain">
                    {{ item.currentOrgName || item.currentOrgId || "-" }}
                  </el-tag>
                  <span v-if="!hasCoord(item)" class="dispatch-board__order-warn">无坐标</span>
                </div>
              </div>
            </div>
          </el-checkbox-group>
          <el-empty v-if="!filteredOrders.length" description="暂无待调度订单" :image-size="60" />
        </el-scrollbar>
      </el-card>

      <!-- 右：地图 -->
      <el-card class="dispatch-board__map-card" shadow="never">
        <template #header>
          <div class="dispatch-board__map-header">
            <span class="dispatch-board__title">调度一张图</span>
            <div class="dispatch-board__legend">
              <span><i class="dot dot--order" />待调度订单 {{ orders.length }}</span>
              <span><i class="dot dot--truck" />在途车辆 {{ trucks.length }}</span>
              <span><i class="dot dot--line" />线路 {{ lines.length }}</span>
              <el-button size="small" :icon="Refresh" @click="loadAll">刷新</el-button>
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
    </div>

    <!-- 派单确认 -->
    <el-dialog v-model="assignVisible" title="一键派单" width="520px" append-to-body @closed="assignVisible = false">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="待调度订单">
          {{ selected.length }} 张<template v-if="startOrgName"> （起始网点：{{ startOrgName }}）</template>
        </el-descriptions-item>
        <el-descriptions-item label="承运车辆">
          {{ activeTruck?.licensePlate || activeTruck?.name || activeTruck?.businessId }}
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
import MapConfigAPI from "@/api/map-config";
import {
  createLabeledMarker,
  fitViewport,
  loadBaiduMap,
  moveMarker,
  wgs84ToBd09,
} from "@/utils/baidu-map";

/** 车辆轮询间隔外部化（红线：轮询间隔禁硬编码），与在途监控共用同一 env，缺省兜底仅防 env 漏配 */
const POLL_INTERVAL_MS = Number(import.meta.env.VITE_MAP_POLL_INTERVAL_MS || 10000);

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

async function loadAll() {
  loading.value = true;
  try {
    await loadBoard();
    await loadTrucks();
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
      background: "#2b6cff",
      onClick: () => {
        activeTruck.value = item;
      },
    });
    truckMarkers.set(key, marker);
  });
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
  if (!activeTruck.value) {
    ElMessage.warning("请先在地图上点击选择承运车辆");
    return;
  }
  assignVisible.value = true;
}

async function doAssign() {
  if (!activeTruck.value) return;
  assigning.value = true;
  try {
    const result = (await DispatchAPI.assign({
      orderIds: selected.value,
      truckId: String(activeTruck.value.businessId),
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
  gap: 12px;
  height: calc(100vh - 130px);

  &__side {
    width: 320px;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
  }

  &__side-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__stats {
    display: flex;
    gap: 6px;
    margin-bottom: 8px;
  }

  &__search {
    margin-bottom: 8px;
  }

  &__list {
    flex: 1;
    min-height: 0;
    margin-top: 6px;
  }

  &__group {
    display: flex;
    flex-direction: column;
    gap: 6px;
    width: 100%;
  }

  &__order {
    display: flex;
    gap: 6px;
    padding: 8px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 6px;
    cursor: pointer;

    &.is-active {
      border-color: var(--el-color-primary);
      background: var(--el-color-primary-light-9);
    }
  }

  &__order-body {
    flex: 1;
    min-width: 0;
  }

  &__order-no {
    font-size: 13px;
    font-weight: 600;
  }

  &__order-meta,
  &__order-org {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 2px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__order-warn {
    color: var(--el-color-warning);
  }

  &__map-card {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
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
    align-items: center;
    gap: 12px;
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