<template>
  <div class="dash">
    <!-- 头部：问候语 -->
    <section class="dash-header">
      <div class="card dash-header__card">
        <div class="dash-header__start">
          <div class="dash-avatar">
            <img v-if="userStore.userInfo.avatar" :src="userStore.userInfo.avatar" alt="" />
            <el-icon v-else :size="22"><User /></el-icon>
          </div>
          <div class="dash-header__text">
            <h1 class="dash-header__greeting">{{ greetings }}</h1>
            <p class="dash-header__date">{{ currentDateStr }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 4 个核心 KPI（数字均来自后端聚合接口，无环比数据源故不展示假箭头） -->
    <section v-loading="overviewLoading" class="dash-stats">
      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--order">
          <el-icon :size="18"><Document /></el-icon>
        </div>
        <div class="stat-card__body">
          <span class="stat-card__label">今日新增运单</span>
          <span class="stat-card__num">{{ overview.todayOrderCount }}</span>
        </div>
        <span class="stat-card__badge">今日</span>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--truck">
          <el-icon :size="18"><Van /></el-icon>
        </div>
        <div class="stat-card__body">
          <span class="stat-card__label">在途车辆</span>
          <span class="stat-card__num">{{ overview.onTruckCount }}</span>
        </div>
        <span class="stat-card__badge stat-card__badge--on">实时</span>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--alert">
          <el-icon :size="18"><Warning /></el-icon>
        </div>
        <div class="stat-card__body">
          <span class="stat-card__label">异常告警</span>
          <span class="stat-card__num">{{ alertCount }}</span>
        </div>
        <span class="stat-card__badge stat-card__badge--danger">未处理</span>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--rate">
          <el-icon :size="18"><CircleCheck /></el-icon>
        </div>
        <div class="stat-card__body">
          <span class="stat-card__label">准时率</span>
          <span class="stat-card__num">--</span>
        </div>
        <span class="stat-card__badge">暂无数据</span>
      </div>
    </section>

    <!-- 图表区 -->
    <section class="dash-chart">
      <div class="card dash-chart__trend">
        <div class="card__head">
          <h3 class="card__title">运输量趋势</h3>
          <el-radio-group v-model="trendDateRange" size="small">
            <el-radio-button label="近7天" :value="7" />
            <el-radio-button label="近30天" :value="30" />
          </el-radio-group>
        </div>
        <div v-loading="trendLoading" class="card__body card__body--chart">
          <ECharts :options="trendChartOptions" height="260px" />
        </div>
      </div>

      <div class="card dash-chart__overview">
        <div class="card__head">
          <h3 class="card__title">运单状态概览</h3>
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
    </section>

    <!-- 底部：待办 + 告警（均为后端真实数据） -->
    <section class="dash-bottom">
      <div class="card">
        <div class="card__head">
          <h3 class="card__title">待处理运单</h3>
          <el-tag size="small">{{ todoList.length }} 单</el-tag>
        </div>
        <div v-loading="todoLoading" class="card__body">
          <el-empty v-if="todoList.length === 0" :image-size="56" description="暂无待处理运单" />
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
          <el-tag type="danger" size="small">{{ alertList.length }} 条</el-tag>
        </div>
        <div v-loading="alarmLoading" class="card__body card__body--scroll">
          <el-empty v-if="alertList.length === 0" :image-size="56" description="暂无未处理告警" />
          <div class="feed">
            <div v-for="item in alertList" :key="item.id" class="feed__item">
              <span class="feed__dot feed__dot--danger" />
              <span class="feed__text">{{ item.alarmContent || "未命名告警" }}</span>
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
import { useUserStore } from "@/stores/user";
import {
  User,
  Document,
  Van,
  Warning,
  CircleCheck,
  Clock,
} from "@element-plus/icons-vue";
import DashboardAPI, {
  type DashboardOverview,
  type OrderTrendPoint,
  type PendingOrder,
} from "@/api/dashboard";
import AlarmAPI, { AlarmHandleStatus, type AlarmRecord } from "@/api/alarm";

const userStore = useUserStore();

// 问候语
const hours = new Date().getHours();
const greetings = computed(() => {
  const n = userStore.userInfo.nickname || "管理员";
  if (hours >= 6 && hours < 8) return `早安，${n}`;
  if (hours >= 8 && hours < 12) return `上午好，${n}`;
  if (hours >= 12 && hours < 18) return `下午好，${n}`;
  if (hours >= 18 && hours < 24) return `晚上好，${n}`;
  return `夜深了，${n}`;
});

const currentDateStr = computed(() => {
  const d = new Date();
  const w = ["日", "一", "二", "三", "四", "五", "六"];
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${w[d.getDay()]}`;
});

// ---- 总览 KPI + 运单三桶（pd-oms 订单统计 + pd-work 在途任务聚合） ----
const overviewLoading = ref(false);
const overview = reactive<DashboardOverview>({
  todayOrderCount: 0,
  onTruckCount: 0,
  pendingCount: 0,
  inTransitCount: 0,
  completedCount: 0,
});

// 未处理告警：与"最新告警"列表共用一次 netty 请求，total 即未处理总数
const alertCount = ref(0);
const alarmLoading = ref(false);
const alertList = ref<AlarmRecord[]>([]);

// 待处理运单
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

// ---- 运输量趋势 ----
const trendDateRange = ref(7);
const trendLoading = ref(false);
const trendChartOptions = ref({});

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
  const primary = getCssVar("--el-color-primary", "#409eff");
  const success = getCssVar("--el-color-success", "#67c23a");
  const textSecondary = getCssVar("--el-text-color-secondary", "#909399");
  const borderLighter = getCssVar("--el-border-color-lighter", "#ebeef5");

  trendChartOptions.value = {
    tooltip: { trigger: "axis" },
    legend: { data: ["运单量", "完成量"], bottom: 0 },
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
        name: "运单量",
        type: "line",
        smooth: true,
        data: points.map((p) => Number(p.orderCount) || 0),
        lineStyle: { color: primary, width: 2.2 },
        itemStyle: { color: primary },
        areaStyle: { opacity: 0.1 },
      },
      {
        name: "完成量",
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

// Header
.dash-header {
  &__card {
    display: flex;
    flex-wrap: wrap;
    gap: 18px;
    align-items: center;
    justify-content: space-between;
    min-height: 78px;
    padding: 16px 18px;
  }

  &__start {
    display: flex;
    flex: 1;
    gap: 12px;
    align-items: center;
    min-width: 260px;
  }

  &__text {
    display: flex;
    flex-direction: column;
    gap: 3px;
  }

  &__greeting {
    margin: 0;
    font-size: 18px;
    font-weight: 500;
    line-height: 1.3;
    color: var(--el-text-color-primary);
  }

  &__date {
    margin: 0;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

.dash-avatar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  overflow: hidden;
  color: var(--el-color-primary);
  background: color-mix(in srgb, var(--el-color-primary) 14%, var(--el-bg-color-overlay));
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 18%, transparent);
  border-radius: 50%;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
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
  @extend %card;

  &__icon {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
    border-radius: 10px;

    &--order {
      color: var(--el-color-primary);
      background: color-mix(in srgb, var(--el-color-primary) 10%, var(--el-bg-color-overlay));
    }
    &--truck {
      color: var(--el-color-success);
      background: color-mix(in srgb, var(--el-color-success) 10%, var(--el-bg-color-overlay));
    }
    &--alert {
      color: var(--el-color-danger);
      background: color-mix(in srgb, var(--el-color-danger) 10%, var(--el-bg-color-overlay));
    }
    &--rate {
      color: var(--el-color-warning);
      background: color-mix(in srgb, var(--el-color-warning) 10%, var(--el-bg-color-overlay));
    }
  }

  &__body {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-width: 0;
  }

  &__num {
    font-size: 24px;
    font-weight: 600;
    line-height: 1.15;
    color: var(--el-text-color-primary);
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

  &__trend {
    display: inline-flex;
    flex-shrink: 0;
    gap: 3px;
    align-items: center;
    font-size: 12px;
    font-weight: 700;
    color: var(--el-text-color-secondary);

    &--success {
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
  grid-template-columns: minmax(0, 3fr) minmax(280px, 1fr);
  gap: $gap;
}

.overview-card {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 20px;
  min-height: 0;
}

.overview-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;

  &__item {
    display: flex;
    flex-direction: column;
    justify-content: center;
    min-height: 64px;
    padding: 12px;
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
    color: var(--el-color-primary);
  }
}

.overview-bars {
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: space-between;
  min-height: 142px;

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
  padding: 10px 20px 16px;

  &__item {
    position: relative;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: baseline;
    padding: 10px 0 10px 16px;

    &::before {
      position: absolute;
      top: 22px;
      bottom: -4px;
      left: 3px;
      width: 1px;
      content: "";
      background: var(--el-border-color-lighter);
    }

    &:last-child::before {
      display: none;
    }
  }

  &__dot {
    position: absolute;
    top: 12px;
    left: 0;
    width: 7px;
    height: 7px;
    border-radius: 50%;

    &--danger {
      background: var(--el-color-danger);
      border: 2px solid var(--el-color-danger-light-8);
    }
  }

  &__text {
    flex: 1;
    min-width: 0;
    font-size: 13px;
    line-height: 1.4;
    color: var(--el-text-color-regular);
  }

  &__time {
    flex-shrink: 0;
    font-size: 12px;
    color: var(--el-text-color-secondary);
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
