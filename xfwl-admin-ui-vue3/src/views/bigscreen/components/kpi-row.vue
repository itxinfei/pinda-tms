<template>
  <div class="bs-kpi">
    <div v-for="(item, idx) in items" :key="item.label" class="bs-kpi__item" :style="{ '--c': item.color }">
      <div class="bs-kpi__icon">
        <!-- 内联 SVG，避免大屏依赖管理端图标包 -->
        <svg v-if="item.icon === 'order'" viewBox="0 0 1024 1024" fill="none" stroke="currentColor" stroke-width="48">
          <path d="M296 136h308l156 156v596H296z" />
          <path d="M604 136v156h156" />
          <path d="M372 452h308M372 568h308M372 684h200" />
        </svg>
        <svg v-else-if="item.icon === 'waybill'" viewBox="0 0 1024 1024" fill="none" stroke="currentColor" stroke-width="48">
          <path d="M180 300l332-140 332 140-332 140z" />
          <path d="M180 300v360l332 164 332-164V300" />
          <path d="M512 440v384" />
        </svg>
        <svg v-else-if="item.icon === 'task'" viewBox="0 0 1024 1024" fill="none" stroke="currentColor" stroke-width="48">
          <rect x="200" y="160" width="624" height="704" rx="40" />
          <path d="M360 360h304M360 520h304M360 680h200" />
        </svg>
        <svg v-else-if="item.icon === 'truck'" viewBox="0 0 1024 1024" fill="none" stroke="currentColor" stroke-width="48">
          <path d="M120 240h560v440H120z" />
          <path d="M680 360h140l84 120v200H680z" />
          <circle cx="300" cy="740" r="72" />
          <circle cx="740" cy="740" r="72" />
        </svg>
        <svg v-else-if="item.icon === 'alarm'" viewBox="0 0 1024 1024" fill="none" stroke="currentColor" stroke-width="48">
          <path d="M512 140c-150 168-232 282-232 404a232 232 0 0 0 464 0c0-122-82-236-232-404z" />
          <path d="M420 820a92 92 0 0 0 184 0" />
        </svg>
        <svg v-else viewBox="0 0 1024 1024" fill="none" stroke="currentColor" stroke-width="48">
          <path d="M512 160a352 352 0 1 1 0 704 352 352 0 0 1 0-704z" />
          <path d="M512 320v192l128 76" />
        </svg>
      </div>
      <div class="bs-kpi__main">
        <div class="bs-kpi__value">{{ display[idx] }}</div>
        <div class="bs-kpi__label">{{ item.label }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";

defineOptions({ name: "KpiRow" });

const props = defineProps<{
  todayOrderCount: number;
  todayTransportOrderCount: number;
  todayTaskCount: number;
  activeTruckCount: number;
  pendingTruckCount: number;
  idleTruckCount: number;
  pendingAlarmCount: number;
  /** 准时率（百分比，phase2 前传 null 显示 --） */
  onTimeRate?: number | null;
}>();

interface KpiItem {
  label: string;
  value: number;
  icon: string;
  color: string;
  isRate?: boolean;
  placeholder?: boolean;
}

const items = computed<KpiItem[]>(() => [
  { label: "今日订单", value: props.todayOrderCount, icon: "order", color: "#00eaff" },
  { label: "今日运单", value: props.todayTransportOrderCount, icon: "waybill", color: "#00ff7f" },
  { label: "今日任务", value: props.todayTaskCount, icon: "task", color: "#9d7bff" },
  { label: "在途车辆", value: props.activeTruckCount, icon: "truck", color: "#2f6bff" },
  { label: "待执行车辆", value: props.pendingTruckCount, icon: "truck", color: "#ffc14d" },
  { label: "空闲车辆", value: props.idleTruckCount, icon: "truck", color: "#5ad1c4" },
  { label: "未处理告警", value: props.pendingAlarmCount, icon: "alarm", color: "#ff5b6b" },
  {
    label: "准时率",
    value: props.onTimeRate ?? 0,
    icon: "rate",
    color: "#00eaff",
    isRate: true,
    placeholder: props.onTimeRate == null,
  },
]);

// 数字滚动动画：每次刷新从旧值平滑补间到新值
const display = ref<string[]>(items.value.map(() => "0"));
let raf = 0;

const formatValue = (val: number, item: KpiItem) =>
  item.placeholder ? "--" : item.isRate ? `${val}%` : val.toLocaleString("en-US");

const animateTo = () => {
  cancelAnimationFrame(raf);
  const starts = display.value.map((d) => Number(d.replace(/[^0-9.]/g, "")) || 0);
  const targets = items.value.map((i) => i.value);
  const duration = 800;
  const t0 = performance.now();
  const step = (now: number) => {
    const p = Math.min(1, (now - t0) / duration);
    const eased = 1 - Math.pow(1 - p, 3);
    display.value = targets.map((t, i) =>
      formatValue(Math.round(starts[i] + (t - starts[i]) * eased), items.value[i]),
    );
    if (p < 1) raf = requestAnimationFrame(step);
  };
  raf = requestAnimationFrame(step);
};

watch(items, animateTo, { immediate: true });
onBeforeUnmount(() => cancelAnimationFrame(raf));
</script>

<style scoped lang="scss">
.bs-kpi {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 10px;
  align-items: stretch;

  &__item {
    position: relative;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 7px 12px 7px 14px;
    background: linear-gradient(135deg, rgba(2, 18, 56, 0.6), rgba(2, 10, 34, 0.35));
    border: 1px solid var(--bs-panel-border);
    border-radius: 5px;
    overflow: hidden;
    transition: transform 0.2s, box-shadow 0.2s;

    /* 左侧主题色光条 */
    &::before {
      content: "";
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 3px;
      background: var(--c);
      box-shadow: 0 0 10px var(--c);
    }

    /* 主题色淡染层 */
    &::after {
      content: "";
      position: absolute;
      inset: 0;
      background: var(--c);
      opacity: 0.08;
      pointer-events: none;
    }

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 18px rgba(0, 0, 0, 0.35);
    }
  }

  &__icon {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    flex: none;
    border-radius: 8px;
    color: var(--c);
    background: rgba(2, 16, 54, 0.55);
    border: 1px solid var(--c);
    box-shadow: 0 0 8px var(--c);

    svg {
      width: 19px;
      height: 19px;
    }
  }

  &__main {
    position: relative;
    z-index: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    line-height: 1.05;
  }

  &__value {
    font-family: var(--bs-font-title);
    font-size: 23px;
    font-weight: 700;
    color: #eaf6ff;
    white-space: nowrap;
    text-shadow: 0 0 10px var(--c);
  }

  &__label {
    margin-top: 2px;
    font-size: 11px;
    letter-spacing: 0.5px;
    color: var(--bs-text-dim);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
</style>
