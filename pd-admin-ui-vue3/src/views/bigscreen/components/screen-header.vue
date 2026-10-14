<template>
  <header class="bs-header">
    <div class="bs-header__side">
      <div class="bs-header__date">{{ dateText }}</div>
      <div class="bs-header__week">
        星期{{ weekText }}<i class="bs-header__ampm">{{ ampmText }}</i>
      </div>
    </div>
    <h1 class="bs-header__title">心飞物流 TMS 运营监控中心</h1>
    <div class="bs-header__side bs-header__side--right">
      <div class="bs-header__weather">
        <!-- 内联天气图标，避免依赖图标包 -->
        <svg class="bs-header__wicon" viewBox="0 0 1024 1024" fill="none" stroke="currentColor" stroke-width="44" stroke-linecap="round">
          <circle cx="512" cy="512" r="190" />
          <path d="M512 120v-56M512 960v-56M120 512H64M960 512h-56M254 254l-40-40M810 810l-40-40M254 770l-40 40M810 214l-40 40" />
        </svg>
        <div class="bs-header__wtext">
          <div class="bs-header__wtemp">{{ weatherTemp }}</div>
          <div class="bs-header__wdesc">{{ weatherDesc }}</div>
        </div>
      </div>
      <div class="bs-header__time">{{ timeText }}</div>
      <router-link class="bs-header__exit" to="/dashboard">退出</router-link>
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
const ampmText = ref("");
const timeText = ref("");
// 天气为示例数据，接入天气 API 后替换（项目暂无天气端点）
const weatherTemp = ref("23°C");
const weatherDesc = ref("晴");
let timer: ReturnType<typeof setInterval> | null = null;

const refresh = () => {
  const now = new Date();
  dateText.value = parseTime(now, "YYYY年MM月DD日");
  weekText.value = "日一二三四五六"[now.getDay()];
  ampmText.value = now.getHours() < 12 ? "上午" : "下午";
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
    flex: 1 1 0;
    min-width: 0;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 4px;

    &--right {
      flex-direction: row;
      align-items: center;
      justify-content: flex-end;
      gap: 24px;
    }
  }

  &__date {
    font-family: var(--bs-font-num);
    font-size: 22px;
    font-weight: 600;
    color: var(--bs-text);
    letter-spacing: 1px;
    text-shadow: 0 0 10px rgba(0, 234, 255, 0.25);
  }

  &__week {
    font-size: 15px;
    color: var(--bs-text-dim);
    letter-spacing: 1px;
  }

  &__ampm {
    margin-left: 10px;
    padding: 0 8px;
    font-style: normal;
    font-family: var(--bs-font-num);
    font-size: 14px;
    color: var(--bs-cyan);
    border: 1px solid var(--bs-panel-border);
    border-radius: 2px;
  }

  &__weather {
    display: flex;
    align-items: center;
    gap: 8px;
    color: var(--bs-cyan);
  }

  &__wicon {
    width: 32px;
    height: 32px;
    filter: drop-shadow(0 0 6px rgba(0, 234, 255, 0.5));
  }

  &__wtext {
    display: flex;
    flex-direction: column;
    line-height: 1.1;
  }

  &__wtemp {
    font-family: var(--bs-font-num);
    font-size: 18px;
    font-weight: 600;
    color: var(--bs-text);
  }

  &__wdesc {
    font-size: 12px;
    color: var(--bs-text-dim);
  }

  &__time {
    font-family: var(--bs-font-num);
    font-size: 32px;
    font-weight: 700;
    color: var(--bs-cyan);
    letter-spacing: 2px;
    text-shadow: 0 0 14px rgba(0, 234, 255, 0.6);
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
