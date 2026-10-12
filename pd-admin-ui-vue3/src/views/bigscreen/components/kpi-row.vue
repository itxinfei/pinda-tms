<template>
  <div class="bs-kpi">
    <div v-for="(item, idx) in items" :key="item.label" class="bs-kpi__item">
      <span class="bs-corner bs-corner--tl" />
      <span class="bs-corner bs-corner--br" />
      <div class="bs-kpi__icon" :style="{ color: item.color, borderColor: item.color }">
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
        <div class="bs-kpi__value" :style="{ color: item.color }">{{ display[idx] }}</div>
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
  gap: 12px;
  height: 100%;

  &__item {
    position: relative;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 0 14px;
    background: linear-gradient(90deg, rgba(0, 120, 255, 0.18), rgba(0, 60, 160, 0.06));
    border: 1px solid var(--bs-panel-border);
    overflow: hidden;

    &::after {
      content: "";
      position: absolute;
      inset: 0;
      background: linear-gradient(120deg, transparent 30%, rgba(0, 234, 255, 0.08) 50%, transparent 70%);
      background-size: 220% 100%;
      animation: bs-kpi-sheen 6s linear infinite;
      pointer-events: none;
    }
  }

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 46px;
    height: 46px;
    flex: none;
    border: 1px solid;
    border-radius: 50%;
    background: rgba(2, 16, 54, 0.5);
    box-shadow: 0 0 16px rgba(0, 234, 255, 0.18), inset 0 0 10px rgba(0, 234, 255, 0.12);

    svg {
      width: 26px;
      height: 26px;
    }
  }

  &__main {
    min-width: 0;
  }

  &__value {
    font-family: var(--bs-font-title);
    font-size: 28px;
    line-height: 1.1;
    white-space: nowrap;
    text-shadow: 0 0 12px currentColor;
  }

  &__label {
    margin-top: 2px;
    font-size: 13px;
    letter-spacing: 1px;
    color: var(--bs-text-dim);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

@keyframes bs-kpi-sheen {
  0% { background-position: 120% 0; }
  100% { background-position: -120% 0; }
}
</style>
