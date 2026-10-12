<template>
  <div class="bs-todo">
    <div v-if="!items.length" class="bs-todo__empty">暂无待处理订单</div>
    <div v-else class="bs-todo__viewport bs-scroll">
      <div class="bs-todo__track" :class="{ 'is-scrolling': items.length >= 6 }">
        <div v-for="(item, key) in doubled" :key="`${item.id}-${key}`" class="bs-todo__item">
          <span class="bs-todo__no">{{ item.orderNo }}</span>
          <span class="bs-todo__sender">{{ item.senderName || "未知发货方" }}</span>
          <span class="bs-todo__status">{{ item.statusText }}</span>
          <span class="bs-todo__time">{{ item.createTime ? item.createTime.slice(5, 16) : "" }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { PendingOrder } from "@/api/dashboard";

defineOptions({ name: "TodoList" });

const props = defineProps<{
  items: PendingOrder[];
}>();

// 复制一份用于无缝滚动
const doubled = computed(() => [...props.items, ...props.items]);
</script>

<style scoped lang="scss">
.bs-todo {
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
    animation: bs-scroll-up 16s linear infinite;

    &:hover {
      animation-play-state: paused;
    }
  }

  &__item {
    display: grid;
    grid-template-columns: 1.4fr 1fr 0.9fr 0.9fr;
    align-items: center;
    gap: 8px;
    height: 38px;
    padding: 0 6px;
    border-bottom: 1px dashed rgba(0, 190, 255, 0.14);
    font-size: 13px;
  }

  &__no {
    font-family: var(--bs-font-num);
    color: var(--bs-cyan);
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  &__sender {
    color: var(--bs-text);
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  &__status {
    color: var(--bs-yellow);
    white-space: nowrap;
  }

  &__time {
    font-family: var(--bs-font-num);
    color: var(--bs-text-dim);
    text-align: right;
  }
}
</style>
