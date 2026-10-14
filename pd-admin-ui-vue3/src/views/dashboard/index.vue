<template>
  <div class="dash">
    <!-- 4 个核心 KPI（数字均来自后端聚合接口，无环比数据源故不展示假箭头） -->
    <StatCards
      :cards="statCards"
      :loading="overviewLoading"
      :error="overviewError"
      @retry="loadOverview"
    />

    <!-- 图表区 -->
    <section class="dash-chart">
      <ChartTrend
        :options="trendChartOptions"
        :loading="trendLoading"
        :error="trendError"
        :empty="trendEmpty"
        :date-range="trendDateRange"
        @retry="loadTrend"
        @view-all="goOrders"
        @update:dateRange="trendDateRange = $event"
        @click-date="onTrendClick"
      />

      <ChartTaskPie
        :options="taskPieOptions"
        :loading="overviewLoading"
        :error="overviewError"
        :empty="taskPieEmpty"
        :head-tag="`今日新增 ${overviewError ? '--' : formatNumber(overview.todayTaskCount)}`"
        @retry="loadOverview"
        @click="goTrucks"
      />
    </section>

    <!-- 次图区：三桶占比 + 线路运量 Top5（等宽均衡） -->
    <section class="dash-chart dash-chart--half">
      <ChartOverview
        :bars="statusBars"
        :loading="overviewLoading"
        :error="overviewError"
        :head-tag="`待处理 ${overviewError ? '--' : formatNumber(overview.pendingCount)}`"
        @retry="loadOverview"
        @click="goOrders"
      />

      <ChartRank
        :options="lineRankOptions"
        :loading="lineVolumeLoading"
        :error="lineVolumeError"
        :empty="lineVolumeEmpty"
        @retry="loadLineVolumes"
        @click="goLines"
      />
    </section>

    <!-- 底部：待办 + 告警（均为后端真实数据） -->
    <BottomLists
      :todo-list="todoList"
      :todo-loading="todoLoading"
      :todo-error="todoError"
      :alert-list="alertList"
      :alarm-loading="alarmLoading"
      :alarm-error="alarmError"
      :alert-count-text="alarmError ? '--' : `${formatNumber(alertCount)} 条未处理`"
      @go-orders="goOrders"
      @go-alarms="goAlarms"
      @retry-todo="loadTodo"
      @retry-alarm="loadAlarms"
    />
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: "Dashboard", inheritAttrs: false });

import { ref, reactive, computed, watch, onMounted } from "vue";
import { useRouter } from "vue-router";
import type { EChartsOption } from "echarts";
import { Document, Van, Warning, CircleCheck } from "@element-plus/icons-vue";
import DashboardAPI, {
  type DashboardOverview,
  type NameValue,
  type OrderTrendPoint,
  type PendingOrder,
} from "@/api/dashboard";
import AlarmAPI, { AlarmHandleStatus, type AlarmRecord } from "@/api/alarm";
import { formatNumber } from "@/utils/format";
import { ElMessage } from "element-plus";
import StatCards, { type StatCardItem } from "./components/stat-cards.vue";
import ChartTrend from "./components/chart-trend.vue";
import ChartTaskPie from "./components/chart-task-pie.vue";
import ChartOverview, { type OverviewBar } from "./components/chart-overview.vue";
import ChartRank from "./components/chart-rank.vue";
import BottomLists from "./components/bottom-lists.vue";

const router = useRouter();

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
// 每张卡可点击直达对应业务页（action 为路由跳转）；spark 为近 7 日迷你趋势（无数据源卡不显示）
const statCards = computed<StatCardItem[]>(() => [
  {
    label: "今日新增订单",
    value: overviewError.value ? "--" : formatNumber(overview.todayOrderCount),
    icon: Document,
    tone: "order",
    badge: "今日",
    badgeClass: "",
    action: goOrders,
    spark: trendPoints.value.map((p) => Number(p.orderCount) || 0),
  },
  {
    label: "在途车辆",
    value: overviewError.value ? "--" : formatNumber(overview.onTruckCount),
    icon: Van,
    tone: "truck",
    badge: "实时",
    badgeClass: "stat-card__badge--on",
    action: goTrucks,
    spark: null,
  },
  {
    label: "异常告警",
    value: alarmError.value ? "--" : formatNumber(alertCount.value),
    icon: Warning,
    tone: "alert",
    badge: "未处理",
    badgeClass: "stat-card__badge--danger",
    action: goAlarms,
    spark: null,
  },
  {
    label: "今日签收",
    value: overviewError.value ? "--" : formatNumber(overview.todayCompletedCount),
    icon: CircleCheck,
    tone: "rate",
    badge: "今日",
    badgeClass: "stat-card__badge--on",
    action: goOrders,
    spark: trendPoints.value.map((p) => Number(p.completedCount) || 0),
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

// 三桶进度条（占比按三桶总量，拒收/取消不计入；各桶固定色 + 渐变）
const statusBars = computed<OverviewBar[]>(() => {
  const p = overview.pendingCount;
  const t = overview.inTransitCount;
  const c = overview.completedCount;
  const total = p + t + c;
  const pct = (v: number) => (total > 0 ? Math.round((v / total) * 100) : 0);
  return [
    {
      label: "待处理",
      valueText: `${formatNumber(p)} 单 · ${pct(p)}%`,
      percent: pct(p),
      color: getCssVar("--el-color-primary", "#FF0033"),
      gradient: `linear-gradient(90deg, ${getCssVar("--el-color-primary", "#FF0033")}, ${getCssVar("--el-color-primary-light-5", "#6ea1ff")})`,
    },
    {
      label: "运输中",
      valueText: `${formatNumber(t)} 单 · ${pct(t)}%`,
      percent: pct(t),
      color: getCssVar("--el-color-warning", "#ff7d00"),
      gradient: `linear-gradient(90deg, ${getCssVar("--el-color-warning", "#ff7d00")}, ${getCssVar("--el-color-warning-light-5", "#ffc96b")})`,
    },
    {
      label: "已签收",
      valueText: `${formatNumber(c)} 单 · ${pct(c)}%`,
      percent: pct(c),
      color: getCssVar("--el-color-success", "#00b42a"),
      gradient: `linear-gradient(90deg, ${getCssVar("--el-color-success", "#00b42a")}, ${getCssVar("--el-color-success-light-5", "#7be188")})`,
    },
  ];
});

// 近7日线路运量 Top5（数据来自大屏聚合 lineVolumes，取前 5）
const lineVolumeLoading = ref(false);
const lineVolumeError = ref(false);
const lineVolumeEmpty = ref(false);
const lineVolumes = ref<{ name: string; taskCount: number }[]>([]);
const lineRankOptions = computed<EChartsOption>(() => {
  const textSecondary = getCssVar("--el-text-color-secondary", "#909399");
  const textRegular = getCssVar("--el-text-color-regular", "#4e5969");
  const primary = getCssVar("--el-color-primary", "#165DFF");
  const primaryLight = getCssVar("--el-color-primary-light-5", "#a8c4ff");
  const fillLight = getCssVar("--el-fill-color-light", "#f2f3f5");
  const rows = lineVolumes.value.map((item) => Number(item.taskCount) || 0);
  const grad = (from: string, to: string) => ({
    type: "linear" as const,
    x: 0,
    y: 0,
    x2: 1,
    y2: 0,
    colorStops: [
      { offset: 0, color: from },
      { offset: 1, color: to },
    ],
  });
  return {
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "shadow" },
      formatter: (params) => {
        const list = params as { name: string; value: number }[];
        const p = list[0];
        const rank = (lineVolumes.value.findIndex((i) => i.name === p.name) ?? 0) + 1;
        return `第 ${rank} 名 ${p.name}<br/>运输任务 ${p.value} 个`;
      },
    },
    grid: { left: "2%", right: "10%", bottom: "2%", top: "4%", containLabel: true },
    xAxis: { type: "value", show: false },
    yAxis: {
      type: "category",
      inverse: true,
      data: lineVolumes.value.map((item) => item.name),
      axisLabel: {
        fontSize: 12,
        color: textRegular,
        // 序号徽章：第 1 名主题色实心，其余浅灰
        formatter: (name: string, idx: number) =>
          idx === 0 ? `{rankTop|${idx + 1}} {name|${name}}` : `{rank|${idx + 1}} {name|${name}}`,
        rich: {
          rankTop: {
            display: "inline-block",
            width: 16,
            height: 16,
            lineHeight: 16,
            textAlign: "center",
            borderRadius: 3,
            color: "#fff",
            fontSize: 10,
            backgroundColor: primary,
          },
          rank: {
            display: "inline-block",
            width: 16,
            height: 16,
            lineHeight: 16,
            textAlign: "center",
            borderRadius: 3,
            color: textSecondary,
            fontSize: 10,
            backgroundColor: fillLight,
          },
          name: { color: textRegular, fontSize: 12 },
        },
      },
      axisLine: { show: false },
      axisTick: { show: false },
    },
    series: [
      {
        type: "bar",
        barWidth: 12,
        data: rows,
        label: { show: true, position: "right", fontSize: 12, color: textSecondary, formatter: "{c}" },
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          // 第 1 名主题色渐变强调，其余同色系浅渐变（跟随主题）
          color: (p: { dataIndex: number }) =>
            p.dataIndex === 0
              ? grad(primary, primaryLight)
              : grad(getCssVar("--el-color-primary-light-7", "#c9d7f5"), getCssVar("--el-color-primary-light-9", "#e8f0ff")),
        },
      },
    ],
  };
});

// 任务环形图空数据判定：无分类或各分类计数全为 0 时不渲染空 canvas
const taskPieEmpty = computed(() => {
  const counts = overview.taskStatusCounts ?? [];
  return counts.length === 0 || counts.every((item) => !item.value);
});

// 运输任务五态环形图（中文名后端给出，前端只配颜色；中心显示任务总数 + 今日新增）
// 配色跟随主题：前 3 色取 primary/success/warning，后 2 色为固定中性色（无对应主题变量）
const taskPieOptions = computed<EChartsOption>(() => {
  const textSecondary = getCssVar("--el-text-color-secondary", "#909399");
  const textPrimary = getCssVar("--el-text-color-primary", "#1d2129");
  const taskStatusColors = [
    getCssVar("--el-color-primary", "#165DFF"),
    getCssVar("--el-color-success", "#00b42a"),
    getCssVar("--el-color-warning", "#ff7d00"),
    "#0fb5c9",
    "#86909c",
  ];
  const items = (overview.taskStatusCounts ?? []).map((item: NameValue) => ({
    name: item.name,
    value: item.value ?? 0,
  }));
  const total = items.reduce((sum, item) => sum + item.value, 0);
  const pctOf = (v: number) => (total > 0 ? Math.round((v / total) * 100) : 0);
  return {
    tooltip: {
      trigger: "item",
      formatter: (p) => {
        const d = p as { name: string; value: number; percent: number };
        return `${d.name}：${d.value} 个（${d.percent}%）`;
      },
    },
    legend: {
      bottom: 0,
      left: "center",
      icon: "circle",
      itemWidth: 7,
      itemHeight: 7,
      itemGap: 4,
      // canvas 不认 CSS var()，需取计算值；图例带数量 + 占比
      textStyle: { fontSize: 10, color: textSecondary },
      formatter: (name: string) => {
        const item = items.find((i) => i.name === name);
        if (!item) return name;
        return `${name}${item.value}·${pctOf(item.value)}%`;
      },
    },
    color: taskStatusColors,
    graphic: [
      {
        type: "text",
        left: "center",
        top: "30%",
        style: { text: "任务总数", fontSize: 12, fill: textSecondary, textAlign: "center" },
      },
      {
        type: "text",
        left: "center",
        top: "37%",
        style: { text: String(total), fontSize: 22, fontWeight: 700, fill: textPrimary, textAlign: "center" },
      },
      {
        type: "text",
        left: "center",
        top: "50%",
        style: {
          text: `今日新增 ${Number(overview.todayTaskCount) || 0}`,
          fontSize: 11,
          fill: textSecondary,
          textAlign: "center",
        },
      },
    ],
    series: [
      {
        type: "pie",
        radius: ["52%", "72%"],
        center: ["50%", "42%"],
        avoidLabelOverlap: true,
        emphasis: { scaleSize: 4, itemStyle: { shadowBlur: 12, shadowColor: "rgba(0,0,0,0.15)" } },
        itemStyle: { borderRadius: 6, borderColor: "#fff", borderWidth: 2 },
        label: { show: false },
        data: items,
      },
    ],
  };
});

// ---- 订单量趋势 ----
const trendDateRange = ref(7);
const trendLoading = ref(false);
const trendError = ref(false);
const trendEmpty = ref(false);
// 近 7 日原始数据点：KPI 迷你趋势线复用（仅 7 天口径）
const trendPoints = ref<OrderTrendPoint[]>([]);
const trendChartOptions = ref<EChartsOption>({});
/** 趋势请求序号：7/30 天快速切换时丢弃过期响应，防止旧数据覆盖新数据 */
let trendRequestSeq = 0;

/** 后端 yyyy-MM-dd → M/d */
function dayLabel(date: string) {
  const parts = date.split("-");
  return parts.length === 3 ? `${Number(parts[1])}/${Number(parts[2])}` : date;
}

function renderTrend(points: OrderTrendPoint[]) {
  const primary = getCssVar("--el-color-primary", "#165DFF");
  const success = getCssVar("--el-color-success", "#00b42a");
  const textSecondary = getCssVar("--el-text-color-secondary", "#909399");
  const textRegular = getCssVar("--el-text-color-regular", "#4e5969");
  const borderLighter = getCssVar("--el-border-color-lighter", "#ebeef5");

  // 30 天口径点数密集：斜排 + 隔点显示，防止标签重叠
  const dense = points.length > 15;
  trendChartOptions.value = {
    tooltip: { trigger: "axis", valueFormatter: (value) => `${value} 单` },
    legend: { data: ["下单量", "签收量"], right: 4, top: 2, itemWidth: 14, itemHeight: 8, icon: "roundRect", textStyle: { fontSize: 12, color: textRegular } },
    grid: { left: "0%", right: "3%", bottom: "4%", top: "18%", containLabel: true },
    xAxis: {
      type: "category",
      data: points.map((p) => dayLabel(p.date)),
      axisLabel: {
        fontSize: 11,
        color: textSecondary,
        interval: dense ? 1 : 0,
        rotate: dense ? 35 : 0,
      },
      axisLine: { lineStyle: { color: borderLighter } },
      axisTick: { show: false },
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
        symbol: "circle",
        symbolSize: 6,
        showSymbol: true,
        // 7 天点数少：直接标注数值，一眼可读
        label: dense
          ? { show: false }
          : { show: true, position: "top", fontSize: 10, color: textSecondary, formatter: "{c}" },
        lineStyle: { color: primary, width: 2.4 },
        itemStyle: { color: primary, borderColor: "#fff", borderWidth: 1.5 },
        areaStyle: {
          color: {
            type: "linear",
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: hexToRgba(primary, 0.22) },
              { offset: 1, color: hexToRgba(primary, 0) },
            ],
          },
        },
        markLine: {
          silent: true,
          symbol: "none",
          lineStyle: { type: "dashed", color: getCssVar("--el-color-primary-light-5", "#b9c6e6"), width: 1 },
          label: { formatter: "均值 {c}", fontSize: 11, color: textSecondary },
          data: [{ type: "average", name: "均值" }],
        },
        markPoint: {
          symbol: "pin",
          symbolSize: 34,
          label: { fontSize: 11, color: "#fff", fontWeight: 600 },
          itemStyle: { color: primary },
          data: [{ type: "max", name: "峰值" }],
        },
      },
      {
        name: "签收量",
        type: "line",
        smooth: true,
        data: points.map((p) => Number(p.completedCount) || 0),
        symbol: "circle",
        symbolSize: 6,
        showSymbol: true,
        label: dense
          ? { show: false }
          : { show: true, position: "bottom", fontSize: 10, color: textSecondary, formatter: "{c}" },
        lineStyle: { color: success, width: 2 },
        itemStyle: { color: success, borderColor: "#fff", borderWidth: 1.5 },
        areaStyle: {
          color: {
            type: "linear",
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: hexToRgba(success, 0.14) },
              { offset: 1, color: hexToRgba(success, 0) },
            ],
          },
        },
      },
    ],
  };
}

function getCssVar(name: string, fallback: string) {
  if (typeof window === "undefined") return fallback;
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
}

/** #RRGGBB → rgba(r, g, b, a)，用于 ECharts 面积渐变（canvas 不认 CSS var） */
function hexToRgba(hex: string, alpha: number): string {
  const h = hex.replace("#", "");
  const full = h.length === 3 ? h.split("").map((c) => c + c).join("") : h;
  const n = Number.parseInt(full, 16);
  if (Number.isNaN(n) || full.length !== 6) return hex;
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${alpha})`;
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
    // 无论 7/30 天都保留近 7 天窗口给 KPI sparkline
    trendPoints.value = list.slice(-7);
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

async function loadLineVolumes() {
  lineVolumeLoading.value = true;
  lineVolumeError.value = false;
  try {
    // 复用大屏聚合接口：近 7 日各线路运量 Top10，工作台取前 5
    const data = await DashboardAPI.getBigScreenSummary();
    const list = Array.isArray(data?.lineVolumes) ? data.lineVolumes : [];
    lineVolumes.value = list.slice(0, 5);
    lineVolumeEmpty.value = list.length === 0;
  } catch {
    // 错误提示由请求拦截器统一弹出；区块切换为错误态 + 重试
    lineVolumeError.value = true;
  } finally {
    lineVolumeLoading.value = false;
  }
}

/**
 * 带权限的跳转：菜单为动态注册，未开通模块的路由不在路由表中，
 * 直接 push 会落到 404；先校验再跳，无权限给明确提示。
 */
function guardPush(path: string, query?: Record<string, string>) {
  const exists = router.getRoutes().some((r) => r.path === path);
  if (!exists) {
    ElMessage.warning("暂无该模块访问权限，请联系管理员开通");
    return;
  }
  router.push(query ? { path, query } : path);
}

function goOrders() {
  guardPush("/order");
}

/** 趋势图日期下钻：点击数据点/柱子 → 订单列表并筛选该日 */
function onTrendClick(date: string) {
  guardPush("/order", { date });
}

function goAlarms() {
  guardPush("/alarm");
}

function goTrucks() {
  guardPush("/truck");
}

function goLines() {
  guardPush("/transport-line");
}

watch(() => trendDateRange.value, () => loadTrend());

// 各区块独立加载、独立容错，单个接口失败不拖垮整页；进入页面即并发拉取
onMounted(() => {
  Promise.allSettled([loadOverview(), loadTrend(), loadTodo(), loadAlarms(), loadLineVolumes()]);
});
</script>

<style lang="scss">
@use "./components/dashboard-card.scss";
</style>
