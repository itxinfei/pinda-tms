<template>
  <div class="dash">
    <!-- 工具条：手动刷新全部区块 + 最后成功刷新时间 -->
    <div class="dash-toolbar">
      <span class="dash-toolbar__hint">数据来自后端实时聚合接口</span>
      <div class="dash-toolbar__right">
        <span v-if="lastRefreshTime" class="dash-toolbar__time">最后更新 {{ lastRefreshTime }}</span>
        <el-button size="small" :icon="Refresh" :loading="anyLoading" @click="refreshAll">刷新</el-button>
      </div>
    </div>

    <!-- 4 个核心 KPI（数字均来自后端聚合接口，无环比数据源故不展示假箭头） -->
    <section v-loading="overviewLoading" class="dash-stats">
      <el-result
        v-if="overviewError"
        icon="error"
        title="总览数据加载失败"
        class="dash-result"
      >
        <template #extra>
          <el-button size="small" type="primary" @click="loadOverview">重试</el-button>
        </template>
      </el-result>
      <template v-else>
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
      </template>
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
          <el-result v-if="trendError" icon="error" title="趋势数据加载失败" class="dash-result">
            <template #extra>
              <el-button size="small" type="primary" @click="loadTrend">重试</el-button>
            </template>
          </el-result>
          <el-empty v-else-if="trendEmpty" description="所选时段暂无订单数据" :image-size="64" />
          <ECharts v-else :options="trendChartOptions" height="248px" />
        </div>
      </div>

      <div class="dash-chart__side">
        <div class="card">
          <div class="card__head">
            <h3 class="card__title">订单状态概览</h3>
            <el-tag type="primary" size="small" effect="plain">
              待处理 {{ overviewError ? "--" : formatNumber(overview.pendingCount) }}
            </el-tag>
          </div>
          <div v-loading="overviewLoading" class="card__body overview-card">
            <el-result v-if="overviewError" icon="error" title="总览数据加载失败" class="dash-result">
              <template #extra>
                <el-button size="small" type="primary" @click="loadOverview">重试</el-button>
              </template>
            </el-result>
            <template v-else>
              <div class="overview-summary">
                <div class="overview-summary__item">
                  <span class="overview-summary__label">待处理</span>
                  <strong class="overview-summary__value">{{ formatNumber(overview.pendingCount) }}</strong>
                </div>
                <div class="overview-summary__item">
                  <span class="overview-summary__label">运输中</span>
                  <strong class="overview-summary__value">{{ formatNumber(overview.inTransitCount) }}</strong>
                </div>
                <div class="overview-summary__item">
                  <span class="overview-summary__label">已签收</span>
                  <strong class="overview-summary__value">{{ formatNumber(overview.completedCount) }}</strong>
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
                    <span class="overview-bars__value">{{ formatNumber(item.value) }} 单</span>
                  </div>
                  <span class="overview-bars__track">
                    <span class="overview-bars__bar" />
                  </span>
                </div>
              </div>
            </template>
          </div>
        </div>

        <div class="card">
          <div class="card__head">
            <h3 class="card__title">运输任务状态</h3>
            <el-tag size="small" effect="plain">
              今日新增 {{ overviewError ? "--" : formatNumber(overview.todayTaskCount) }}
            </el-tag>
          </div>
          <div v-loading="overviewLoading" class="card__body task-card">
            <el-result v-if="overviewError" icon="error" title="总览数据加载失败" class="dash-result">
              <template #extra>
                <el-button size="small" type="primary" @click="loadOverview">重试</el-button>
              </template>
            </el-result>
            <el-empty v-else-if="taskPieEmpty" description="暂无任务状态数据" :image-size="56" />
            <ECharts v-else :options="taskPieOptions" height="172px" />
          </div>
        </div>
      </div>
    </section>

    <!-- 底部：待办 + 告警（均为后端真实数据） -->
    <section class="dash-bottom">
      <div class="card">
        <div class="card__head">
          <h3 class="card__title">待处理订单</h3>
          <div class="card__head-right">
            <el-button link type="primary" size="small" @click="goOrders">查看更多</el-button>
            <el-tag size="small">{{ todoList.length }} 单</el-tag>
          </div>
        </div>
        <div v-loading="todoLoading" class="card__body">
          <el-result v-if="todoError" icon="error" title="待办加载失败" class="dash-result">
            <template #extra>
              <el-button size="small" type="primary" @click="loadTodo">重试</el-button>
            </template>
          </el-result>
          <el-empty v-else-if="todoList.length === 0" :image-size="56" description="暂无待处理订单" />
          <div
            v-for="item in todoList"
            :key="item.id"
            class="todo-row"
          >
            <el-icon :size="16" class="todo-row__icon--pending">
              <Clock />
            </el-icon>
            <el-tooltip
              :content="`${item.orderNo}${item.senderName ? ` - ${item.senderName}` : ''}`"
              placement="top"
              :show-after="300"
            >
              <span class="todo-row__title">
                {{ item.orderNo }}<template v-if="item.senderName"> - {{ item.senderName }}</template>
              </span>
            </el-tooltip>
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
          <div class="card__head-right">
            <el-button link type="primary" size="small" @click="goAlarms">查看更多</el-button>
            <el-tag type="danger" size="small">{{ alarmError ? "--" : formatNumber(alertCount) }} 条未处理</el-tag>
          </div>
        </div>
        <div v-loading="alarmLoading" class="card__body card__body--scroll">
          <div class="feed">
            <el-result v-if="alarmError" icon="error" title="告警加载失败" class="dash-result">
              <template #extra>
                <el-button size="small" type="primary" @click="loadAlarms">重试</el-button>
              </template>
            </el-result>
            <el-empty v-else-if="alertList.length === 0" :image-size="56" description="暂无未处理告警" />
            <div v-for="item in alertList" :key="item.id" class="feed__item">
              <el-tag
                size="small"
                :type="alarmTagType(item.alarmType)"
                effect="light"
                class="feed__tag"
              >
                {{ alarmTypeText(item.alarmType) }}
              </el-tag>
              <el-tooltip
                :content="item.alarmContent || '运输过程异常，请及时关注'"
                placement="top"
                :show-after="300"
              >
                <span class="feed__text">{{ item.alarmContent || "运输过程异常，请及时关注" }}</span>
              </el-tooltip>
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
import { useRouter } from "vue-router";
import type { EChartsOption } from "echarts";
import {
  Document,
  Van,
  Warning,
  CircleCheck,
  Clock,
  Refresh,
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
import { formatNumber } from "@/utils/format";

const router = useRouter();

// 问候语已上移至全局顶栏 LayoutToolbar

// ---- 总览 KPI + 订单三桶（pd-oms 订单统计 + pd-work 任务/车辆聚合） ----
const overviewLoading = ref(false);
/** 总览接口失败：数字统一显 "--"，绝不静默显 0 误导判断 */
const overviewError = ref(false);
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

// 4 张 KPI 卡（第 4 张由原"准时率（无数据源）"替换为真实"今日签收"）；接口失败显 "--"
const statCards = computed(() => [
  {
    label: "今日新增订单",
    value: overviewError.value ? "--" : formatNumber(overview.todayOrderCount),
    icon: Document,
    tone: "order",
    badge: "今日",
    badgeClass: "",
  },
  {
    label: "在途车辆",
    value: overviewError.value ? "--" : formatNumber(overview.onTruckCount),
    icon: Van,
    tone: "truck",
    badge: "实时",
    badgeClass: "stat-card__badge--on",
  },
  {
    label: "异常告警",
    value: alarmError.value ? "--" : formatNumber(alertCount.value),
    icon: Warning,
    tone: "alert",
    badge: "未处理",
    badgeClass: "stat-card__badge--danger",
  },
  {
    label: "今日签收",
    value: overviewError.value ? "--" : formatNumber(overview.todayCompletedCount),
    icon: CircleCheck,
    tone: "rate",
    badge: "今日",
    badgeClass: "stat-card__badge--on",
  },
]);

// 未处理告警：与"最新告警"列表共用一次 netty 请求，total 即未处理总数
const alertCount = ref(0);
const alarmLoading = ref(false);
const alarmError = ref(false);
const alertList = ref<AlarmRecord[]>([]);

// 待处理订单
const todoLoading = ref(false);
const todoError = ref(false);
const todoList = ref<PendingOrder[]>([]);

// 任一区块在加载，刷新按钮即转圈（首次加载四个并发，完成前按钮保持 loading）
const anyLoading = computed(
  () => overviewLoading.value || trendLoading.value || todoLoading.value || alarmLoading.value,
);

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

// 任务环形图空数据判定：无分类或各分类计数全为 0 时不渲染空 canvas
const taskPieEmpty = computed(() => {
  const counts = overview.taskStatusCounts ?? [];
  return counts.length === 0 || counts.every((item) => !item.value);
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
const trendError = ref(false);
const trendEmpty = ref(false);
const trendChartOptions = ref<EChartsOption>({});
/** 趋势请求序号：7/30 天快速切换时丢弃过期响应，防止旧数据覆盖新数据 */
let trendRequestSeq = 0;

/** 后端 "yyyy-MM-dd HH:mm:ss" → "MM-dd HH:mm"；无值显示 "-" */
function formatShortTime(value?: string) {
  if (!value) return "-";
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

  // 30 天口径点数密集：斜排 + 隔点显示，防止标签重叠
  const dense = points.length > 15;
  trendChartOptions.value = {
    tooltip: { trigger: "axis", valueFormatter: (value) => `${value} 单` },
    legend: { data: ["下单量", "签收量"], bottom: 0 },
    grid: { left: "0%", right: "3%", bottom: "14%", top: "5%", containLabel: true },
    xAxis: {
      type: "category",
      data: points.map((p) => dayLabel(p.date)),
      axisLabel: {
        fontSize: 11,
        color: textSecondary,
        interval: dense ? 1 : 0,
        rotate: dense ? 35 : 0,
      },
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
  overviewError.value = false;
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
  } catch {
    // 错误提示由请求拦截器统一弹出；区块切换为错误态 + 重试，数字显 "--" 而非 0
    overviewError.value = true;
  } finally {
    overviewLoading.value = false;
  }
}

async function loadTrend() {
  // 自增序号：响应返回时若已不是最新请求（快速切换 7/30 天），直接丢弃
  const seq = ++trendRequestSeq;
  trendLoading.value = true;
  trendError.value = false;
  try {
    const points = await DashboardAPI.getTrend(trendDateRange.value);
    if (seq !== trendRequestSeq) return;
    const list = Array.isArray(points) ? points : [];
    trendEmpty.value = list.length === 0;
    renderTrend(list);
  } catch {
    if (seq !== trendRequestSeq) return;
    trendError.value = true;
  } finally {
    if (seq === trendRequestSeq) trendLoading.value = false;
  }
}

async function loadTodo() {
  todoLoading.value = true;
  todoError.value = false;
  try {
    const list = await DashboardAPI.getTodo(5);
    todoList.value = Array.isArray(list) ? list : [];
  } catch {
    // 错误提示由请求拦截器统一弹出；区块切换为错误态 + 重试
    todoError.value = true;
  } finally {
    todoLoading.value = false;
  }
}

async function loadAlarms() {
  alarmLoading.value = true;
  alarmError.value = false;
  try {
    // 一次请求同时拿到最新 5 条未处理告警与未处理总数 total
    const res = await AlarmAPI.page({
      page: 1,
      pageSize: 5,
      status: AlarmHandleStatus.PENDING,
    });
    alertList.value = res?.items ?? [];
    alertCount.value = Number(res?.total) || 0;
  } catch {
    // 错误提示由请求拦截器统一弹出；区块切换为错误态 + 重试
    alarmError.value = true;
  } finally {
    alarmLoading.value = false;
  }
}

/** 当前时间 HH:mm:ss，作为"最后更新"文案 */
function nowTimeText(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

/** 手动刷新：四个区块全部重拉（趋势按当前选中的 7/30 天），完成后记录时间 */
async function refreshAll() {
  await Promise.allSettled([loadOverview(), loadTrend(), loadTodo(), loadAlarms()]);
  lastRefreshTime.value = nowTimeText();
}

function goOrders() {
  router.push("/order");
}

function goAlarms() {
  router.push("/alarm");
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
const lastRefreshTime = ref("");
onMounted(() => {
  refreshAll();
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

// 顶部工具条
.dash-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;

  &__hint {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__right {
    display: flex;
    gap: 10px;
    align-items: center;
  }

  &__time {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

// 卡片内紧凑错误态：去掉大图标与大留白，只留文案 + 重试
.dash-result {
  padding: 4px 0;

  :deep(.el-result__icon) {
    display: none;
  }

  :deep(.el-result__title p) {
    margin: 0;
    font-size: 14px;
    line-height: 1.6;
  }

  :deep(.el-result__subtitle) {
    padding-top: 2px;
  }

  :deep(.el-result__extra) {
    padding-top: 8px;
  }
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
      color: var(--el-color-white);
      background: linear-gradient(135deg, var(--el-color-primary), var(--el-color-primary-light-3));
      box-shadow: 0 6px 14px color-mix(in srgb, var(--el-color-primary) 30%, transparent);
    }
    &--truck {
      color: var(--el-color-white);
      background: linear-gradient(135deg, var(--el-color-success), var(--el-color-success-light-3));
      box-shadow: 0 6px 14px color-mix(in srgb, var(--el-color-success) 30%, transparent);
    }
    &--alert {
      color: var(--el-color-white);
      background: linear-gradient(135deg, var(--el-color-danger), var(--el-color-danger-light-3));
      box-shadow: 0 6px 14px color-mix(in srgb, var(--el-color-danger) 30%, transparent);
    }
    &--rate {
      color: var(--el-color-white);
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

  &__head-right {
    display: flex;
    gap: 8px;
    align-items: center;
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
