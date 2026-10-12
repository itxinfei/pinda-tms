<template>
  <div class="bs-kpi">
    <div v-for="item in items" :key="item.label" class="bs-kpi__item">
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
        <svg v-else-if="item.icon === 'truck'" viewBox="0 0 1024 1024" fill="none" stroke="currentColor" stroke-width="48">
          <path d="M120 240h560v440H120z" />
          <path d="M680 360h140l84 120v200H680z" />
          <circle cx="300" cy="740" r="72" />
          <circle cx="740" cy="740" r="72" />
        </svg>
        <svg v-else viewBox="0 0 1024 1024" fill="none" stroke="currentColor" stroke-width="48">
          <path d="M512 140c-150 168-232 282-232 404a232 232 0 0 0 464 0c0-122-82-236-232-404z" />
          <path d="M420 820a92 92 0 0 0 184 0" />
        </svg>
      </div>
      <div class="bs-kpi__main">
        <div class="bs-kpi__value" :style="{ color: item.color }">{{ format(item.value) }}</div>
        <div class="bs-kpi__label">{{ item.label }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

defineOptions({ name: "KpiRow" });

const props = defineProps<{
  todayOrderCount: number;
  todayTransportOrderCount: number;
  activeTruckCount: number;
  pendingAlarmCount: number;
}>();

const items = computed(() => [
  { label: "今日订单", value: props.todayOrderCount, icon: "order", color: "#00eaff" },
  { label: "今日运单", value: props.todayTransportOrderCount, icon: "waybill", color: "#00ff7f" },
  { label: "在途车辆", value: props.activeTruckCount, icon: "truck", color: "#2f6bff" },
  { label: "未处理告警", value: props.pendingAlarmCount, icon: "alarm", color: "#ff5b6b" },
]);

const format = (value: number) => (Number.isFinite(value) ? String(value) : "0");
</script>

<style scoped lang="scss">
.bs-kpi {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  height: 100%;

  &__item {
    position: relative;
    display: flex;
    align-items: center;
    gap: 18px;
    padding: 0 24px;
    background: linear-gradient(90deg, rgba(0, 120, 255, 0.18), rgba(0, 60, 160, 0.06));
    border: 1px solid var(--bs-panel-border);
  }

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 64px;
    height: 64px;
    border: 1px solid;
    border-radius: 50%;
    background: rgba(2, 16, 54, 0.5);
    box-shadow: 0 0 18px rgba(0, 234, 255, 0.18), inset 0 0 12px rgba(0, 234, 255, 0.12);

    svg {
      width: 36px;
      height: 36px;
    }
  }

  &__value {
    font-family: var(--bs-font-title);
    font-size: 42px;
    line-height: 1.1;
    text-shadow: 0 0 14px currentColor;
  }

  &__label {
    margin-top: 4px;
    font-size: 15px;
    letter-spacing: 3px;
    color: var(--bs-text-dim);
  }
}
</style>
