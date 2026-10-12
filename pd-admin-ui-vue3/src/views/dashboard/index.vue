<template>
  <div class="dash">
    <!-- 4 个核心 KPI（数字均来自后端聚合接口，无环比数据源故不展示假箭头） -->
    <section v-loading="overviewLoading" class="dash-stats">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <div class="stat-card__icon" :class="`stat-card__icon--${card.tone}`">
          <el-icon :size="18"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-card__body">
          <span class="stat-card__label">{{ card.label }}</span>
          <span class="stat-card__num">{{ card.value }}</span>
        </div>
        <span class="stat-card__badge" :class="card.badgeClass">{{ card.badge }}</span>
      </div>
    </section>

    <!-- 图表区 -->
    <section class="dash-chart">
      <div class="card dash-chart__trend">
        <div class="card__head">
          <h3 class="card__title">订单量趋势</h3>
          <el-radio-group v-model="trendDateRange" size="small">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
        <div v-loading="trendLoading" class="card__body card__body--chart">
          <ECharts :options="trendChartOptions" height="248px" />
        </div>
      </div>

      <div class="dash-chart__side">
        <div class="card">
          <div class="card__head">
            <h3 class="card__title">订单状态概览</h3>
            <el-tag type="primary" size="small" effect="plain">待处理 {{ overview.pendingCount }}</el-tag>
          </div>
          <div v-loading="overviewLoading" class="card__body overview-card">
            <div class="overview-summary">
              <div class="overview-summary__item">
                <span class="overview-summary__label">待处理</span>
                <strong class="overview-summary__value">{{ overview.pendingCount }}</strong>
              </div>
              <div class="overview-summary__item">
                <span class="overview-summary__label">运输中</span>
                <strong class="overview-summary__value">{{ overview.inTransitCount }}</strong>
              </div>
              <div class="overview-summary__item">
                <span class="overview-summary__label">已签收</span>
                <strong class="overview-summary__value">{{ overview.completedCount }}</strong>
              </div>
            </div>
            <div class="overview-bars">
              <div
                v-for="item in statusBars"
                :key="item.label"
                class="overview-bars__item"
                :style="{ '--overview-percent': `${item.percent}%` }"
              >
                <div class="overview-bars__meta">
                  <span class="overview-bars__label">
                    <span class="overview-bars__dot" />
                    {{ item.label }}
                  </span>
                  <span class="overview-bars__value">{{ item.value }} 单</span>
                </div>
                <span class="overview-bars__track">
                  <span class="overview-bars__bar" />
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card__head">
            <h3 class="card__title">运输任务状态</h3>
            <el-tag size="small" effect="plain">今日新增 {{ overview.todayTaskCount ?? 0 }}</el-tag>
          </div>
          <div v-loading="overviewLoading" class="card__body task-card">
            <ECharts :options="taskPieOptions" height="172px" />
          </div>
        </div>
      </div>
    </section>

    <!-- 底部：待办 + 告警（均为后端真实数据） -->
    <section class="dash-bottom">
      <div class="card">
        <div class="card__head">
          <h3 class="card__title">待处理订单</h3>
          <el-tag size="small">{{ todoList.length }} 单</el-tag>
        </div>
        <div v-loading="todoLoading" class="card__body">
          <el-empty v-if="todoList.length === 0" :image-size="56" description="暂无待处理订单" />
          <div
            v-for="item in todoList"
            :key="item.id"
            class="todo-row"
          >
            <el-icon :size="16" class="todo-row__icon--pending">
              <Clock />
            </el-icon>
            <span class="todo-row__title">
              {{ item.orderNo }}<template v-if="item.senderName"> - {{ item.senderName }}</template>
            </span>
            <el-tag type="warning" size="small" effect="plain" class="todo-row__tag">
              {{ item.statusText || "待处理" }}
            </el-tag>
            <span class="todo-row__time">{{ formatShortTime(item.createTime) }}</span>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card__head">
          <h3 class="card__title">最新告警</h3>
          <el-tag type="danger" size="small">{{ alertCount }} 条未处理</el-tag>
        </div>
        <div v-loading="alarmLoading" class="card__body card__body--scroll">
          <el-empty v-if="alertList.length === 0" :image-size="56" description="暂无未处理告警" />
          <div class="feed">
            <div v-for="item in alertList" :key="item.id" class="feed__item">
              <el-tag
                size="small"
                :type="alarmTagType(item.alarmType)"
                effect="light"
                class="feed__tag"
              >
                {{ alarmTypeText(item.alarmType) }}
              </el-tag>
              <span class="feed__text">{{ item.alarmContent || "运输过程异常，请及时关注" }}</span>
              <span class="feed__time">{{ formatShortTime(item.alarmTime || item.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: "Dashboard", inheritAttrs: false });

import { ref, reactive, computed, watch, onMounted } from "vue";
import type { EChartsOption } from "echarts";
import {
  Document,
  Van,
  Warning,
  CircleCheck,
  Clock,
} from "@element-plus/icons-vue";
import DashboardAPI, {
  type DashboardOverview,
  type NameValue,
  type OrderTrendPoint,
  type PendingOrder,
} from "@/api/dashboard";
import AlarmAPI, {
  AlarmHandleStatus,
  AlarmTypeKey,
  alarmTypeKeyOf,
  alarmTypeText,
  type AlarmRecord,
} from "@/api/alarm";

// 问候语已上移至全局顶栏 LayoutToolbar

// ---- 总览 KPI + 订单三桶（pd-oms 订单统计 + pd-work 任务/车辆聚合） ----
const overviewLoading = ref(false);
const overview = reactive<DashboardOverview>({
  todayOrderCount: 0,
  onTruckCount: 0,
  pendingCount: 0,
  inTransitCount: 0,
  completedCount: 0,
  todayCompletedCount: 0,
  todayTaskCount: 0,
  taskStatusCounts: [],
});

// 4 张 KPI 卡（第 4 张由原"准时率（无数据源）"替换为真实"今日签收"）
const statCards = computed(() => [
  {
    label: "今日新增订单",
    value: overview.todayOrderCount,
    icon: Document,
    tone: "order",
    badge: "今日",
    badgeClass: "",
  },
  {
    label: "在途车辆",
    value: overview.onTruckCount,
    icon: Van,
    tone: "truck",
    badge: "实时",
    badgeClass: "stat-card__badge--on",
  },
  {
    label: "异常告警",
    value: alertCount.value,
    icon: Warning,
    tone: "alert",
    badge: "未处理",
    badgeClass: "stat-card__badge--danger",
  },
  {
    label: "今日签收",
    value: overview.todayCompletedCount,
    icon: CircleCheck,
    tone: "rate",
    badge: "今日",
    badgeClass: "stat-card__badge--on",
  },
]);

// 未处理告警：与"最新告警"列表共用一次 netty 请求，total 即未处理总数
const alertCount = ref(0);
const alarmLoading = ref(false);
const alertList = ref<AlarmRecord[]>([]);

// 待处理订单
const todoLoading = ref(false);
const todoList = ref<PendingOrder[]>([]);

// 三桶进度条（占比按三桶总量，拒收/取消不计入）
const statusBars = computed(() => {
  const p = overview.pendingCount;
  const t = overview.inTransitCount;
  const c = overview.completedCount;
  const total = p + t + c;
  const pct = (v: number) => (total > 0 ? Math.round((v / total) * 100) : 0);
  return [
    { label: "待处理", value: p, percent: pct(p) },
    { label: "运输中", value: t, percent: pct(t) },
    { label: "已签收", value: c, percent: pct(c) },
  ];
});

// 运输任务五态环形图（中文名后端给出，前端只配颜色，不做状态码映射）
const TASK_STATUS_COLORS = ["#165DFF", "#00b42a", "#ff7d00", "#0fb5c9", "#86909c"];
const taskPieOptions = computed<EChartsOption>(() => ({
  tooltip: { trigger: "item", formatter: "{b}：{c} 个（{d}%）" },
  legend: {
    bottom: 0,
    itemWidth: 8,
    itemHeight: 8,
    icon: "circle",
    // canvas 不认 CSS var()，需取计算值
    textStyle: { fontSize: 11, color: getCssVar("--el-text-color-regular", "#4e5969") },
  },
  color: TASK_STATUS_COLORS,
  series: [
    {
      type: "pie",
      radius: ["45%", "68%"],
      center: ["50%", "42%"],
      avoidLabelOverlap: true,
      label: { show: false },
      data: (overview.taskStatusCounts ?? []).map((item: NameValue) => ({
        name: item.name,
        value: item.value ?? 0,
      })),
    },
  ],
}));

// ---- 订单量趋势 ----
const trendDateRange = ref(7);
const trendLoading = ref(false);
const trendChartOptions = ref<EChartsOption>({});

/** 后端 "yyyy-MM-dd HH:mm:ss" → "MM-dd HH:mm"；无值返回空串 */
function formatShortTime(value?: string) {
  if (!value) return "";
  return value.length >= 16 ? value.substring(5, 16) : value;
}

/** 后端 yyyy-MM-dd → M/d */
function dayLabel(date: string) {
  const parts = date.split("-");
  return parts.length === 3 ? `${Number(parts[1])}/${Number(parts[2])}` : date;
}

function renderTrend(points: OrderTrendPoint[]) {
  const primary = getCssVar("--el-color-primary", "#165DFF");
  const success = getCssVar("--el-color-success", "#00b42a");
  const textSecondary = getCssVar("--el-text-color-secondary", "#909399");
  const borderLighter = getCssVar("--el-border-color-lighter", "#ebeef5");

  trendChartOptions.value = {
    tooltip: { trigger: "axis" },
    legend: { data: ["下单量", "签收量"], bottom: 0 },
    grid: { left: "0%", right: "3%", bottom: "14%", top: "5%", containLabel: true },
    xAxis: {
      type: "category",
      data: points.map((p) => dayLabel(p.date)),
      axisLabel: { fontSize: 11, color: textSecondary },
    },
    yAxis: {
      type: "value",
      minInterval: 1,
      axisLabel: { fontSize: 11, color: textSecondary },
      splitLine: { lineStyle: { type: "dashed", color: borderLighter } },
    },
    series: [
      {
        name: "下单量",
        type: "line",
        smooth: true,
        data: points.map((p) => Number(p.orderCount) || 0),
        lineStyle: { color: primary, width: 2.2 },
        itemStyle: { color: primary },
        areaStyle: { opacity: 0.1 },
      },
      {
        name: "签收量",
        type: "line",
        smooth: true,
        data: points.map((p) => Number(p.completedCount) || 0),
        lineStyle: { color: success, width: 1.8 },
        itemStyle: { color: success },
      },
    ],
  };
}

function getCssVar(name: string, fallback: string) {
  if (typeof window === "undefined") return fallback;
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
}

async function loadOverview() {
  overviewLoading.value = true;
  try {
    const data = await DashboardAPI.getOverview();
    if (data) {
      overview.todayOrderCount = Number(data.todayOrderCount) || 0;
      overview.onTruckCount = Number(data.onTruckCount) || 0;
      overview.pendingCount = Number(data.pendingCount) || 0;
      overview.inTransitCount = Number(data.inTransitCount) || 0;
      overview.completedCount = Number(data.completedCount) || 0;
      overview.todayCompletedCount = Number(data.todayCompletedCount) || 0;
      overview.todayTaskCount = Number(data.todayTaskCount) || 0;
      overview.taskStatusCounts = Array.isArray(data.taskStatusCounts) ? data.taskStatusCounts : [];
    }
  } finally {
    overviewLoading.value = false;
  }
}

async function loadTrend() {
  trendLoading.value = true;
  try {
    const points = await DashboardAPI.getTrend(trendDateRange.value);
    renderTrend(Array.isArray(points) ? points : []);
  } catch (e) {
    renderTrend([]);
  } finally {
    trendLoading.value = false;
  }
}

async function loadTodo() {
  todoLoading.value = true;
  try {
    const list = await DashboardAPI.getTodo(5);
    todoList.value = Array.isArray(list) ? list : [];
  } finally {
    todoLoading.value = false;
  }
}

async function loadAlarms() {
  alarmLoading.value = true;
  try {
    // 一次请求同时拿到最新 5 条未处理告警与未处理总数 total
    const res = await AlarmAPI.page({
      page: 1,
      pageSize: 5,
      status: AlarmHandleStatus.PENDING,
    });
    alertList.value = res?.items ?? [];
    alertCount.value = Number(res?.total) || 0;
  } finally {
    alarmLoading.value = false;
  }
}

/** 告警 tag 配色：超速/离线危险红，停留/偏离警示橙，人工上报类中性蓝 */
function alarmTagType(code?: number): "danger" | "warning" | "info" {
  const key = alarmTypeKeyOf(code);
  if (key === AlarmTypeKey.SPEED_OVER || key === AlarmTypeKey.VEHICLE_OFFLINE) return "danger";
  if (key === AlarmTypeKey.STAY_TOO_LONG || key === AlarmTypeKey.DEVIATE_ROUTE) return "warning";
  return "info";
}

watch(() => trendDateRange.value, () => loadTrend());

// 各区块独立加载、独立容错，单个接口失败不拖垮整页
onMounted(() => {
  loadOverview();
  loadTrend();
  loadTodo();
  loadAlarms();
});
</script>

<style lang="scss" scoped>
$gap: 12px;
$pad: 10px;

%card {
  overflow: hidden;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--card-border);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
}

.dash {
  display: flex;
  flex-direction: column;
  gap: $gap;
  padding: $pad;
  background: var(--page-bg);
}

// Stat cards
.dash-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $gap;
}

.stat-card {
  display: flex;
  gap: 14px;
  align-items: center;
  min-height: 84px;
  padding: 18px;
  cursor: default;
  @extend %card;
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--el-box-shadow-light);
  }

  &__icon {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 46px;
    height: 46px;
    border-radius: 12px;

    &--order {
      color: #fff;
      background: linear-gradient(135deg, var(--el-color-primary), var(--el-color-primary-light-3));
      box-shadow: 0 6px 14px color-mix(in srgb, var(--el-color-primary) 30%, transparent);
    }
    &--truck {
      color: #fff;
      background: linear-gradient(135deg, var(--el-color-success), var(--el-color-success-light-3));
      box-shadow: 0 6px 14px color-mix(in srgb, var(--el-color-success) 30%, transparent);
    }
    &--alert {
      color: #fff;
      background: linear-gradient(135deg, var(--el-color-danger), var(--el-color-danger-light-3));
      box-shadow: 0 6px 14px color-mix(in srgb, var(--el-color-danger) 30%, transparent);
    }
    &--rate {
      color: #fff;
      background: linear-gradient(135deg, var(--el-color-warning), var(--el-color-warning-light-3));
      box-shadow: 0 6px 14px color-mix(in srgb, var(--el-color-warning) 30%, transparent);
    }
  }

  &__body {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-width: 0;
  }

  &__num {
    font-size: 26px;
    font-weight: 700;
    line-height: 1.15;
    color: var(--el-text-color-primary);
    font-variant-numeric: tabular-nums;
  }

  &__label {
    margin-bottom: 3px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  &__badge {
    flex-shrink: 0;
    font-size: 11px;
    font-weight: 500;

    &--on {
      color: var(--el-color-success);
    }

    &--danger {
      color: var(--el-color-danger);
    }
  }
}

// Generic card
.card {
  display: flex;
  flex-direction: column;
  @extend %card;

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 48px;
    padding: 13px 18px;
    border-bottom: 1px solid var(--card-border);
  }

  &__title {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  &__body {
    padding: 16px 18px 18px;

    &--chart {
      padding: 14px 18px 16px;
    }

    &--scroll {
      flex: 1;
      padding: 0;
      overflow-y: auto;
    }
  }
}

// Chart & bottom grids
.dash-chart {
  display: grid;
  grid-template-columns: minmax(0, 3fr) minmax(300px, 1.15fr);
  gap: $gap;

  &__side {
    display: flex;
    flex-direction: column;
    gap: $gap;
    min-width: 0;

    .card {
      flex: 1;
      min-height: 0;
    }
  }
}

.overview-card {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.task-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.overview-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;

  &__item {
    display: flex;
    flex-direction: column;
    justify-content: center;
    min-height: 60px;
    padding: 10px 12px;
    background: color-mix(in srgb, var(--el-color-primary) 4%, var(--el-bg-color-overlay));
    border: 1px solid color-mix(in srgb, var(--el-color-primary) 10%, var(--el-border-color-lighter));
    border-radius: 6px;
  }

  &__label {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__value {
    margin-top: 5px;
    font-size: 18px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    color: var(--el-color-primary);
  }
}

.overview-bars {
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: space-evenly;
  min-height: 120px;

  &__item {
    display: flex;
    flex-direction: column;
    gap: 7px;
  }

  &__meta {
    display: flex;
    gap: 10px;
    align-items: center;
    justify-content: space-between;
  }

  &__label,
  &__value {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__value {
    font-variant-numeric: tabular-nums;
  }

  &__label {
    display: inline-flex;
    gap: 6px;
    align-items: center;
  }

  &__dot {
    width: 6px;
    height: 6px;
    background: var(--el-color-primary);
    border-radius: 50%;
  }

  &__track {
    height: 5px;
    overflow: hidden;
    background: color-mix(in srgb, var(--el-color-primary) 10%, var(--el-fill-color-light));
    border-radius: 999px;
  }

  &__bar {
    display: block;
    width: var(--overview-percent);
    height: 100%;
    background: linear-gradient(90deg, var(--el-color-primary), var(--el-color-primary-light-3));
    border-radius: inherit;
    transition: width 0.4s ease;
  }
}

.dash-bottom {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $gap;
}

// Todo rows
.todo-row {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 11px 0;

  & + & {
    border-top: 1px solid var(--el-border-color-lighter);
  }

  &__icon--pending {
    flex-shrink: 0;
    color: var(--el-color-warning);
  }

  &__title {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 13px;
    color: var(--el-text-color-regular);
    white-space: nowrap;
  }

  &__tag {
    flex-shrink: 0;
  }

  &__time {
    flex-shrink: 0;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

// Alert feed
.feed {
  display: flex;
  flex-direction: column;
  padding: 8px 18px 12px;

  &__item {
    display: flex;
    gap: 8px;
    align-items: center;
    padding: 9px 0;

    & + & {
      border-top: 1px solid var(--el-border-color-lighter);
    }
  }

  &__tag {
    flex-shrink: 0;
  }

  &__text {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    font-size: 13px;
    line-height: 1.4;
    color: var(--el-text-color-regular);
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__time {
    flex-shrink: 0;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    font-variant-numeric: tabular-nums;
  }
}

// Responsive
@media (max-width: 1200px) {
  .dash-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 992px) {
  .dash-chart {
    grid-template-columns: 1fr;
  }

  .dash-bottom {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .dash {
    gap: 10px;
    padding: 10px;
  }

  .dash-stats {
    grid-template-columns: 1fr;
  }
}
</style>
