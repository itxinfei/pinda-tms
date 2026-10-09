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

    <!-- 4 个核心 KPI -->
    <section class="dash-stats">
      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--order">
          <el-icon :size="18"><Document /></el-icon>
        </div>
        <div class="stat-card__body">
          <span class="stat-card__label">今日运单</span>
          <span class="stat-card__num">{{ todayOrderCount }}</span>
        </div>
        <span class="stat-card__trend stat-card__trend--success">
          <el-icon :size="12"><ArrowUp /></el-icon>
          12.5%
        </span>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--truck">
          <el-icon :size="18"><Van /></el-icon>
        </div>
        <div class="stat-card__body">
          <span class="stat-card__label">在途车辆</span>
          <span class="stat-card__num">{{ onTruckCount }}</span>
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
        <span class="stat-card__trend stat-card__trend--danger">
          <el-icon :size="12"><ArrowUp /></el-icon>
          3
        </span>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--rate">
          <el-icon :size="18"><CircleCheck /></el-icon>
        </div>
        <div class="stat-card__body">
          <span class="stat-card__label">准时率</span>
          <span class="stat-card__num">{{ onTimeRate }}%</span>
        </div>
        <span class="stat-card__trend stat-card__trend--success">
          <el-icon :size="12"><ArrowUp /></el-icon>
          2.1%
        </span>
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
        <div class="card__body card__body--chart">
          <ECharts :options="trendChartOptions" height="260px" />
        </div>
      </div>

      <div class="card dash-chart__overview">
        <div class="card__head">
          <h3 class="card__title">运单状态概览</h3>
          <el-tag type="primary" size="small" effect="plain">待处理 {{ pendingCount }}</el-tag>
        </div>
        <div class="card__body overview-card">
          <div class="overview-summary">
            <div class="overview-summary__item">
              <span class="overview-summary__label">待调度</span>
              <strong class="overview-summary__value">{{ pendingDispatch }}</strong>
            </div>
            <div class="overview-summary__item">
              <span class="overview-summary__label">运输中</span>
              <strong class="overview-summary__value">{{ transporting }}</strong>
            </div>
            <div class="overview-summary__item">
              <span class="overview-summary__label">已完成</span>
              <strong class="overview-summary__value">{{ completed }}</strong>
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

    <!-- 底部：待办 + 告警 -->
    <section class="dash-bottom">
      <div class="card">
        <div class="card__head">
          <h3 class="card__title">待处理运单</h3>
          <el-tag size="small">{{ pendingList.length }} 单</el-tag>
        </div>
        <div class="card__body">
          <div
            v-for="item in pendingList"
            :key="item.id"
            class="todo-row"
          >
            <el-icon :size="16" class="todo-row__icon--pending">
              <Clock />
            </el-icon>
            <span class="todo-row__title">{{ item.orderNo }} - {{ item.customer }}</span>
            <el-tag type="warning" size="small" effect="plain" class="todo-row__tag">
              {{ item.status }}
            </el-tag>
            <span class="todo-row__time">{{ item.time }}</span>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card__head">
          <h3 class="card__title">最新告警</h3>
          <el-tag type="danger" size="small">{{ alertList.length }} 条</el-tag>
        </div>
        <div class="card__body card__body--scroll">
          <div class="feed">
            <div v-for="item in alertList" :key="item.id" class="feed__item">
              <span class="feed__dot feed__dot--danger" />
              <span class="feed__text">{{ item.content }}</span>
              <span class="feed__time">{{ item.time }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: "Dashboard", inheritAttrs: false });

import { ref, computed, watch, onMounted } from "vue";
import { useUserStore } from "@/stores/user";
import {
  User,
  Document,
  Van,
  Warning,
  CircleCheck,
  ArrowUp,
  Clock,
} from "@element-plus/icons-vue";

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

// KPI 数据（后面接真实接口）
const todayOrderCount = ref(128);
const onTruckCount = ref(36);
const alertCount = ref(5);
const onTimeRate = ref(96.2);

// 运单状态
const pendingCount = ref(23);
const pendingDispatch = ref(15);
const transporting = ref(36);
const completed = ref(77);

// 待处理运单列表
const pendingList = ref([
  { id: 1, orderNo: "YD20261008001", customer: "华为技术有限公司", status: "待调度", time: "10分钟前" },
  { id: 2, orderNo: "YD20261008002", customer: "小米科技", status: "待装车", time: "30分钟前" },
  { id: 3, orderNo: "YD20261008003", customer: "顺丰速运", status: "待确认", time: "1小时前" },
  { id: 4, orderNo: "YD20261008004", customer: "京东物流", status: "待调度", time: "2小时前" },
  { id: 5, orderNo: "YD20261008005", customer: "菜鸟网络", status: "待装车", time: "昨天 18:30" },
]);

// 告警列表
const alertList = ref([
  { id: 1, content: "车辆 鄂A12345 偏离路线", time: "5分钟前" },
  { id: 2, content: "运单 YD20261007089 预计超时", time: "20分钟前" },
  { id: 3, content: "司机 张三 连续驾驶超4小时", time: "1小时前" },
  { id: 4, content: "车辆 鄂A67890 温度异常", time: "2小时前" },
  { id: 5, content: "仓库 武汉仓 库存不足", time: "昨天 16:42" },
]);

// 状态进度条
const statusBars = computed(() => [
  { label: "待调度", value: pendingDispatch.value, percent: pendingDispatch.value / 100 * 100 },
  { label: "运输中", value: transporting.value, percent: transporting.value / 100 * 100 },
  { label: "已完成", value: completed.value, percent: completed.value / 100 * 100 },
]);

// 趋势图表
const trendDateRange = ref(7);
const trendChartOptions = ref({});

function updateTrendChart() {
  const primary = getCssVar("--el-color-primary", "#409eff");
  const success = getCssVar("--el-color-success", "#67c23a");
  const textSecondary = getCssVar("--el-text-color-secondary", "#909399");
  const borderLighter = getCssVar("--el-border-color-lighter", "#ebeef5");

  const days = trendDateRange.value;
  const dates = [];
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    dates.push(`${d.getMonth() + 1}/${d.getDate()}`);
  }

  trendChartOptions.value = {
    tooltip: { trigger: "axis" },
    legend: { data: ["运单量", "完成量"], bottom: 0 },
    grid: { left: "0%", right: "3%", bottom: "14%", top: "5%", containLabel: true },
    xAxis: {
      type: "category",
      data: dates,
      axisLabel: { fontSize: 11, color: textSecondary },
    },
    yAxis: {
      type: "value",
      axisLabel: { fontSize: 11, color: textSecondary },
      splitLine: { lineStyle: { type: "dashed", color: borderLighter } },
    },
    series: [
      {
        name: "运单量",
        type: "line",
        smooth: true,
        data: dates.map(() => Math.floor(Math.random() * 50) + 80),
        lineStyle: { color: primary, width: 2.2 },
        itemStyle: { color: primary },
        areaStyle: { opacity: 0.1 },
      },
      {
        name: "完成量",
        type: "line",
        smooth: true,
        data: dates.map(() => Math.floor(Math.random() * 40) + 70),
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

watch(() => trendDateRange.value, () => updateTrendChart());

onMounted(() => {
  updateTrendChart();
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
