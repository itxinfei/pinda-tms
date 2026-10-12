<template>
  <header class="bs-header">
    <div class="bs-header__side">
      <div class="bs-header__date">{{ dateText }}</div>
      <div class="bs-header__week">星期{{ weekText }}</div>
    </div>
    <h1 class="bs-header__title">品达物流 TMS 运营监控中心</h1>
    <div class="bs-header__side bs-header__side--right">
      <div class="bs-header__time">{{ timeText }}</div>
      <router-link class="bs-header__exit" to="/dashboard">退出大屏</router-link>
    </div>
    <span class="bs-header__scan" />
  </header>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from "vue";
import { parseTime } from "../utils/time";

defineOptions({ name: "ScreenHeader" });

const dateText = ref("");
const weekText = ref("");
const timeText = ref("");
let timer: ReturnType<typeof setInterval> | null = null;

const refresh = () => {
  const now = new Date();
  dateText.value = parseTime(now, "YYYY年MM月DD日");
  weekText.value = "日一二三四五六"[now.getDay()];
  timeText.value = parseTime(now, "HH:mm:ss");
};

onMounted(() => {
  refresh();
  timer = setInterval(refresh, 1000);
});

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
});
</script>

<style scoped lang="scss">
.bs-header {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  padding: 0 32px;
  background: url("../assets/images/header-bg.webp") center / 100% 100% no-repeat;

  &__side {
    display: flex;
    flex-direction: column;
    gap: 2px;
    width: 360px;
    font-size: 14px;
    color: var(--bs-text-dim);

    &--right {
      align-items: flex-end;
    }
  }

  &__date {
    font-family: var(--bs-font-num);
    font-size: 16px;
    color: var(--bs-text);
    letter-spacing: 1px;
  }

  &__time {
    font-family: var(--bs-font-num);
    font-size: 22px;
    color: var(--bs-cyan);
    letter-spacing: 2px;
    text-shadow: 0 0 12px rgba(0, 234, 255, 0.55);
  }

  &__title {
    position: absolute;
    left: 50%;
    top: 50%;
    transform: translate(-50%, -54%);
    margin: 0;
    font-family: var(--bs-font-title);
    font-size: 36px;
    font-weight: 400;
    letter-spacing: 10px;
    color: #fff;
    text-shadow: 0 0 18px rgba(0, 234, 255, 0.65), 0 2px 4px rgba(0, 0, 0, 0.6);
    animation: bs-title-glow 3.6s ease-in-out infinite;
  }

  &__scan {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 2px;
    background: linear-gradient(90deg, transparent, var(--bs-cyan), transparent);
    box-shadow: 0 0 12px rgba(0, 234, 255, 0.8);
    animation: bs-scan-x 5s linear infinite;
  }

  &__exit {
    font-size: 13px;
    color: var(--bs-text-dim);
    text-decoration: none;
    border: 1px solid var(--bs-panel-border);
    padding: 2px 10px;
    border-radius: 2px;
    transition: color 0.2s, border-color 0.2s;

    &:hover {
      color: var(--bs-cyan);
      border-color: var(--bs-cyan);
    }
  }
}

@keyframes bs-title-glow {
  0%, 100% { text-shadow: 0 0 18px rgba(0, 234, 255, 0.55), 0 2px 4px rgba(0, 0, 0, 0.6); }
  50% { text-shadow: 0 0 28px rgba(0, 234, 255, 0.95), 0 0 8px rgba(0, 234, 255, 0.6), 0 2px 4px rgba(0, 0, 0, 0.6); }
}

@keyframes bs-scan-x {
  0% { transform: translateX(-30%); opacity: 0; }
  20% { opacity: 1; }
  80% { opacity: 1; }
  100% { transform: translateX(30%); opacity: 0; }
}
</style>
