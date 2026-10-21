<template>
  <div class="bs-track">
    <div class="bs-track__bar">
      <span class="bs-track__name">{{ vehicleName || "未选择车辆" }}</span>
      <div class="bs-track__speeds">
        <button
          v-for="s in [1, 2, 4]"
          :key="s"
          class="bs-track__speed"
          :class="{ 'is-active': speed === s }"
          :disabled="!trail.length"
          @click="speed = s"
        >
          {{ s }}x
        </button>
      </div>
      <button class="bs-track__play" :disabled="!trail.length" @click="toggle">
        {{ playing ? "暂停" : "回放" }}
      </button>
      <button class="bs-track__reset" :disabled="!trail.length" @click="reset">重置</button>
    </div>

    <div class="bs-track__map">
      <ECharts :options="option" width="100%" height="100%" />
    </div>

    <div class="bs-track__ctrl">
      <span class="bs-track__time">{{ currentTime }}</span>
      <input
        class="bs-track__range"
        type="range"
        min="0"
        :max="maxIndex"
        :value="currentIndex"
        :disabled="!trail.length"
        @input="onSeek"
      />
      <span class="bs-track__idx">{{ trail.length ? `${currentIndex + 1}/${trail.length}` : "0/0" }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
// echarts/core 是模块单例；此处注册 "china" 地图与全局 ECharts 组件共享同一注册表
import * as echarts from "echarts/core";
import chinaGeoJSON from "../assets/chinaGeoJSON.json";
import { computed, onBeforeUnmount, ref, watch } from "vue";
import type { EChartsOption } from "echarts";
import ECharts from "@/components/ECharts/index.vue";

echarts.registerMap("china", chinaGeoJSON as never);

export interface TrailPoint {
  lng: number;
  lat: number;
  /** 时间戳（毫秒） */
  t: number;
}

defineOptions({ name: "TrajectoryReplay" });

const props = defineProps<{
  /** 该车辆的轨迹点序列（按时间升序） */
  trail: TrailPoint[];
  /** 车辆名称（车牌等） */
  vehicleName?: string;
}>();

const maxIndex = computed(() => Math.max(0, props.trail.length - 1));
const currentIndex = ref(0);
const playing = ref(false);
/** 播放倍速：1x / 2x / 4x，基础步进 600ms 除以倍速 */
const speed = ref(1);
const STEP_BASE = 600;
let timer: ReturnType<typeof setInterval> | null = null;

// 轨迹更新时，若当前索引越界则夹紧到末尾
watch(
  () => props.trail.length,
  (len) => {
    if (currentIndex.value > len - 1) currentIndex.value = Math.max(0, len - 1);
  },
);

const currentTime = computed(() => {
  const p = props.trail[currentIndex.value];
  if (!p) return "--:--:--";
  const d = new Date(p.t);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
});

const traveled = computed(() => props.trail.slice(0, currentIndex.value + 1).map((p) => [p.lng, p.lat]));
const fullPath = computed(() => props.trail.map((p) => [p.lng, p.lat]));
const currentPoint = computed(() => {
  const p = props.trail[currentIndex.value];
  return p ? [p.lng, p.lat] : null;
});

const option = computed<EChartsOption>(() => ({
  tooltip: { show: false },
  geo: {
    map: "china",
    aspectScale: 0.85,
    layoutCenter: ["50%", "50%"],
    layoutSize: "96%",
    roam: true,
    scaleLimit: { min: 1, max: 8 },
    label: { show: false },
    itemStyle: {
      areaColor: "rgba(7,21,55,0.8)",
      borderColor: "#0177ff",
      borderWidth: 1,
    },
    emphasis: { disabled: true },
  },
  series: [
    {
      name: "完整轨迹",
      type: "lines",
      coordinateSystem: "geo",
      zlevel: 1,
      silent: true,
      lineStyle: { color: "rgba(0,234,255,0.22)", width: 1, curveness: 0 },
      data: fullPath.value.length > 1 ? [{ coords: fullPath.value }] : [],
    },
    {
      name: "已行驶",
      type: "lines",
      coordinateSystem: "geo",
      zlevel: 2,
      silent: true,
      lineStyle: { color: "#00eaff", width: 2, curveness: 0, shadowColor: "rgba(0,234,255,0.6)", shadowBlur: 6 },
      data: traveled.value.length > 1 ? [{ coords: traveled.value }] : [],
    },
    {
      name: "当前位置",
      type: "effectScatter",
      coordinateSystem: "geo",
      zlevel: 3,
      symbolSize: 9,
      showEffectOn: "render",
      rippleEffect: { brushType: "stroke", scale: 3 },
      itemStyle: { color: "#ff8a3d", shadowBlur: 10, shadowColor: "rgba(255,138,61,0.9)" },
      data: currentPoint.value ? [currentPoint.value] : [],
    },
  ],
}));

const stop = () => {
  playing.value = false;
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
};

const startTimer = () => {
  stop();
  playing.value = true;
  timer = setInterval(() => {
    if (currentIndex.value >= maxIndex.value) {
      stop();
      return;
    }
    currentIndex.value += 1;
  }, STEP_BASE / speed.value);
};

const toggle = () => {
  if (!props.trail.length) return;
  if (playing.value) {
    stop();
    return;
  }
  if (currentIndex.value >= maxIndex.value) currentIndex.value = 0;
  startTimer();
};

const reset = () => {
  stop();
  currentIndex.value = 0;
};

// 播放中切换倍速：以新速率重启计时器（保持当前进度）
watch(speed, () => {
  if (playing.value) startTimer();
});

const onSeek = (e: Event) => {
  stop();
  currentIndex.value = Number((e.target as HTMLInputElement).value);
};

onBeforeUnmount(stop);
</script>

<style scoped lang="scss">
.bs-track {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;

  &__bar {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;
  }

  &__name {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    font-family: var(--bs-font-num);
    font-size: 13px;
    color: var(--bs-cyan);
  }

  &__play {
    flex: none;
    padding: 2px 12px;
    font-size: 12px;
    color: var(--bs-cyan);
    background: rgba(0, 234, 255, 0.1);
    border: 1px solid var(--bs-panel-border);
    border-radius: 2px;
    cursor: pointer;
    transition: background 0.2s;

    &:hover:not(:disabled) {
      background: rgba(0, 234, 255, 0.22);
    }

    &:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }
  }

  &__speeds {
    display: flex;
    gap: 2px;
  }

  &__speed {
    flex: none;
    padding: 2px 6px;
    font-size: 12px;
    line-height: 1.4;
    color: var(--bs-text-dim);
    background: transparent;
    border: 1px solid var(--bs-panel-border);
    border-radius: 2px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover:not(:disabled) {
      color: var(--bs-cyan);
    }

    &.is-active {
      color: #04101f;
      background: var(--bs-cyan);
      border-color: var(--bs-cyan);
      font-weight: 600;
    }

    &:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }
  }

  &__reset {
    flex: none;
    padding: 2px 10px;
    font-size: 12px;
    color: var(--bs-text-dim);
    background: transparent;
    border: 1px solid var(--bs-panel-border);
    border-radius: 2px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover:not(:disabled) {
      color: var(--bs-cyan);
      border-color: var(--bs-cyan);
    }

    &:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }
  }

  &__map {
    flex: 1;
    min-height: 0;
  }

  &__ctrl {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 4px;
  }

  &__time {
    flex: none;
    font-family: var(--bs-font-num);
    font-size: 12px;
    color: var(--bs-text-dim);
  }

  &__range {
    flex: 1;
    accent-color: var(--bs-cyan);
    height: 4px;
  }

  &__idx {
    flex: none;
    font-family: var(--bs-font-num);
    font-size: 12px;
    color: var(--bs-text-dim);
  }
}
</style>
