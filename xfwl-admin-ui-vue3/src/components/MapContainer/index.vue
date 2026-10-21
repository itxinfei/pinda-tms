<!--
 * 地图容器组件：收敛厂商 SDK 加载 / AK 拉取 / 初始化 / 错误重试 / 卸载清理样板。
 * 页面只消费 ready 事件拿 { provider, map }，之后的一切覆盖物操作走 MapProvider 接口；
 * 业务轮询与 keepAlive 状态仍归页面（涉及业务语义）。
-->
<template>
  <div class="map-container">
    <div ref="mapRef" class="map-container__map" />
    <div v-if="error" class="map-container__error">
      <el-empty :description="error" :image-size="80">
        <el-button type="primary" size="small" @click="init">重试</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { onBeforeUnmount, onMounted, ref } from "vue";
import MapConfigAPI from "@/api/map-config";
import { createMapProvider, type LngLat, type MapProvider } from "@/utils/map";

defineOptions({ name: "MapContainer" });

const props = withDefaults(
  defineProps<{
    /** 初始中心（缺省全国视角） */
    center?: LngLat;
    /** 初始级别（缺省 5） */
    zoom?: number;
    /** 是否开启滚轮缩放（缺省 true） */
    scrollWheelZoom?: boolean;
  }>(),
  {
    center: () => ({ lng: 104.19, lat: 35.86 }),
    zoom: 5,
    scrollWheelZoom: true,
  }
);

const emit = defineEmits<{
  /** 初始化（含重试后）成功，页面自持 provider/map 引用 */
  ready: [payload: { provider: MapProvider; map: any }];
}>();

const mapRef = ref<HTMLElement>();
const error = ref("");
let provider: MapProvider | null = null;
let map: any = null;

async function init() {
  error.value = "";
  try {
    const { ak, vendor } = await MapConfigAPI.get();
    provider = createMapProvider(vendor);
    await provider.load(ak);
    if (!mapRef.value) {
      return;
    }
    map = provider.createMap(mapRef.value, {
      center: props.center,
      zoom: props.zoom,
      scrollWheelZoom: props.scrollWheelZoom,
    });
    emit("ready", { provider, map });
  } catch (e: any) {
    error.value = e?.message || "地图初始化失败";
  }
}

onMounted(init);

onBeforeUnmount(() => {
  // GL SDK 无官方 dispose 入口，解除引用交由 GC 回收 WebGL 上下文（全站既有口径）
  map = null;
  provider = null;
});
</script>

<style scoped lang="scss">
.map-container,
.map-container__map {
  position: relative;
  width: 100%;
  height: 100%;
}

.map-container__error {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--content-bg);
}
</style>
