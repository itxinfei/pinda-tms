<template>
  <div class="bs-risk">
    <!-- 风险等级汇总（控制塔式概览） -->
    <div class="bs-risk__summary">
      <div class="bs-risk__chip bs-risk__chip--high">
        <span class="bs-risk__chip-num">{{ counts.high }}</span>
        <span class="bs-risk__chip-label">高危</span>
      </div>
      <div class="bs-risk__chip bs-risk__chip--mid">
        <span class="bs-risk__chip-num">{{ counts.mid }}</span>
        <span class="bs-risk__chip-label">中危</span>
      </div>
      <div class="bs-risk__chip bs-risk__chip--low">
        <span class="bs-risk__chip-num">{{ counts.low }}</span>
        <span class="bs-risk__chip-label">低危</span>
      </div>
      <div class="bs-risk__total">
        在险事件 <b>{{ items.length }}</b> 起
      </div>
    </div>

    <!-- 风险清单 -->
    <div v-if="!items.length" class="bs-risk__empty">暂无在险事件</div>
    <div v-else class="bs-risk__viewport bs-scroll">
      <div
        v-for="item in topItems"
        :key="item.id"
        class="bs-risk__item"
        :class="`bs-risk__item--${item.level}`"
      >
        <span class="bs-risk__bar" />
        <span class="bs-risk__tag">{{ item.category }}</span>
        <span class="bs-risk__title">{{ item.title }}</span>
        <span class="bs-risk__detail">{{ item.detail }}</span>
        <span class="bs-risk__time">{{ item.time ? item.time.slice(5, 16) : "" }}</span>
        <i v-if="item.level === 'high'" class="bs-risk__dot" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

export interface RiskItem {
  id: string;
  /** 风险等级：高危/中危/低危 */
  level: "high" | "mid" | "low";
  /** 风险类别（偏航/离线/超时/超速…） */
  category: string;
  /** 主体（车辆/任务/订单） */
  title: string;
  /** 详情描述 */
  detail: string;
  /** 时间 yyyy-MM-dd HH:mm:ss */
  time?: string;
}

defineOptions({ name: "RiskWarning" });

const props = defineProps<{
  items: RiskItem[];
}>();

const counts = computed(() => {
  const c = { high: 0, mid: 0, low: 0 };
  for (const i of props.items) c[i.level] += 1;
  return c;
});

// 卡片只展示 Top 若干，避免挤占地图区域
const topItems = computed(() => props.items.slice(0, 6));
</script>

<style scoped lang="scss">
.bs-risk {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;

  &__summary {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 8px;
  }

  &__chip {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: 64px;
    padding: 4px 10px;
    border: 1px solid;
    border-radius: 4px;
    background: rgba(2, 16, 54, 0.45);

    &--high {
      color: var(--bs-red);
      border-color: rgba(255, 91, 107, 0.5);
      box-shadow: 0 0 12px rgba(255, 91, 107, 0.25);
    }

    &--mid {
      color: var(--bs-orange);
      border-color: rgba(255, 138, 61, 0.5);
    }

    &--low {
      color: var(--bs-blue);
      border-color: rgba(47, 107, 255, 0.5);
    }
  }

  &__chip-num {
    font-family: var(--bs-font-title);
    font-size: 22px;
    line-height: 1.1;
    text-shadow: 0 0 10px currentColor;
  }

  &__chip-label {
    font-size: 12px;
    color: var(--bs-text-dim);
  }

  &__total {
    margin-left: auto;
    font-size: 13px;
    color: var(--bs-text-dim);

    b {
      color: var(--bs-cyan);
      font-family: var(--bs-font-num);
      font-size: 16px;
    }
  }

  &__empty {
    display: flex;
    align-items: center;
    justify-content: center;
    flex: 1;
    color: var(--bs-text-dim);
    font-size: 13px;
    letter-spacing: 2px;
  }

  &__viewport {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  &__item {
    position: relative;
    display: grid;
    grid-template-columns: 3px 56px 1fr 1.4fr 78px;
    align-items: center;
    gap: 8px;
    height: 34px;
    padding: 0 6px;
    border-bottom: 1px dashed rgba(0, 190, 255, 0.12);
    font-size: 13px;

    &--high {
      background: linear-gradient(90deg, rgba(255, 91, 107, 0.12), transparent);
    }

    &--mid {
      background: linear-gradient(90deg, rgba(255, 138, 61, 0.1), transparent);
    }
  }

  &__bar {
    width: 3px;
    height: 16px;
    border-radius: 2px;
    background: var(--bs-blue);
  }

  &__item--high &__bar {
    background: var(--bs-red);
    box-shadow: 0 0 8px var(--bs-red);
  }

  &__item--mid &__bar {
    background: var(--bs-orange);
  }

  &__tag {
    color: var(--bs-text);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__title {
    color: var(--bs-cyan);
    font-family: var(--bs-font-num);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__detail {
    color: var(--bs-text-dim);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__time {
    color: var(--bs-text-dim);
    text-align: right;
    font-family: var(--bs-font-num);
  }

  &__dot {
    position: absolute;
    top: 4px;
    right: 4px;
  }

  &__item--high &__dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--bs-red);
    box-shadow: 0 0 8px var(--bs-red);
    animation: bs-blink 1.4s ease-in-out infinite;
  }
}

@keyframes bs-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.25; }
}
</style>
