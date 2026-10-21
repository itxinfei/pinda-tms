<template>
  <div class="bs-alert">
    <div v-if="!alarms.length" class="bs-alert__empty">暂无实时告警</div>
    <div v-else class="bs-alert__viewport bs-scroll">
      <div class="bs-alert__track" :class="{ 'is-scrolling': alarms.length >= 6 }">
        <div v-for="(item, key) in doubled" :key="`${item.id}-${key}`" class="bs-alert__item">
          <span class="bs-alert__tag" :class="tagClass(item.alarmType)">{{ alarmTypeText(item.alarmType) }}</span>
          <span class="bs-alert__content">{{ item.alarmContent || "运输过程异常，请及时关注" }}</span>
          <span class="bs-alert__time">{{ item.alarmTime || item.createTime || "" }}</span>
          <i v-if="item.status === 0" class="bs-alert__dot" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { alarmTypeText, type AlarmRecord } from "@/api/alarm";

defineOptions({ name: "AlertScroll" });

const props = defineProps<{
  alarms: AlarmRecord[];
}>();

// 复制一份用于无缝滚动（CSS 位移 -50% 恰好回到起点）
const doubled = computed(() => [...props.alarms, ...props.alarms]);

const tagClass = (code?: number) =>
  code !== undefined && code !== null && code >= 1 && code <= 9 ? "bs-alert__tag--gps" : "bs-alert__tag--manual";
</script>

<style scoped lang="scss">
.bs-alert {
  height: 100%;

  &__empty {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: var(--bs-text-dim);
    font-size: 13px;
    letter-spacing: 2px;
  }

  &__viewport {
    height: 100%;
    overflow: hidden;
  }

  &__track.is-scrolling {
    animation: bs-scroll-up 18s linear infinite;

    &:hover {
      animation-play-state: paused;
    }
  }

  &__item {
    display: flex;
    align-items: center;
    gap: 10px;
    height: 40px;
    padding: 0 8px;
    border-bottom: 1px dashed rgba(0, 190, 255, 0.15);
    font-size: 13px;
  }

  &__tag {
    flex: none;
    padding: 1px 8px;
    border: 1px solid;
    border-radius: 2px;
    font-size: 12px;
    line-height: 18px;

    &--gps {
      color: #ff8a80;
      border-color: rgba(255, 91, 107, 0.55);
      background: rgba(255, 91, 107, 0.12);
    }

    &--manual {
      color: #ffd166;
      border-color: rgba(255, 193, 77, 0.5);
      background: rgba(255, 193, 77, 0.1);
    }
  }

  &__content {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    color: var(--bs-text);
  }

  &__time {
    flex: none;
    font-family: var(--bs-font-num);
    font-size: 12px;
    color: var(--bs-text-dim);
  }

  &__dot {
    flex: none;
    width: 7px;
    height: 7px;
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
