<template>
  <view class="page">
    <view class="card map-card">
      <map
        class="map"
        :latitude="center.lat"
        :longitude="center.lng"
        :markers="markers"
        :include-points="includePoints"
        scale="14"
      ></map>
    </view>

    <!-- 路由节点 -->
    <view class="card" v-if="routeList.length">
      <text class="title">路由节点</text>
      <view class="node" v-for="(n, idx) in routeList" :key="idx">
        <text class="node-name">{{ n.agencyName || '—' }}</text>
        <text class="node-time">{{ n.arrivalTime || '' }}</text>
      </view>
    </view>

    <u-gap height="12"></u-gap>
    <text class="tip">每 10 秒刷新快递员实时位置</text>
  </view>
</template>

<script setup lang="ts">
// 路由/轨迹：接任务 id 调 taskRoute 取节点；地图展示 + 每 10s 轮询 traceLatest 取快递员实时位置
import { ref } from 'vue'
import { taskRoute, traceLatest } from '@/common/api/courier'
import { wgs84ToGcj02 } from '@/common/utils/coord'
import { useUserStore } from '@/common/store/user'

const id = ref('')
const userStore = useUserStore()
const routeList = ref<any[]>([])
const center = ref({ lat: 39.9087, lng: 116.3975 }) // 默认北京，有数据后覆盖
const markers = ref<any[]>([])
const includePoints = ref<any[]>([])
let timer: any = null

// 解析轨迹点经纬度（兼容 lat/lng、latitude/longitude）
function pickLatLng(p: any): [number, number] | null {
  if (!p) return null
  const lng = Number(p.lng ?? p.longitude)
  const lat = Number(p.lat ?? p.latitude)
  if (!isFinite(lng) || !isFinite(lat) || (!lng && !lat)) return null
  return [lng, lat]
}

function normalizeRoute(r: any): any[] {
  if (!r) return []
  if (Array.isArray(r)) return r
  if (Array.isArray(r.items)) return r.items
  if (Array.isArray(r.routeList)) return r.routeList
  return []
}

// 构建地图标记：路由节点（蓝）+ 实时位置（红）
function buildMarkers() {
  const list: any[] = []
  routeList.value.forEach((n, i) => {
    const ll = pickLatLng(n)
    if (ll) {
      list.push({
        id: i,
        latitude: ll[1],
        longitude: ll[0],
        title: n.agencyName || '网点',
        // 修改点：默认红色标点即可，不强制自定义图标
      })
    }
  })
  // 实时位置置于末尾并高亮（iconPath 缺省时为系统红点）
  if (lastLatLng.value) {
    list.push({
      id: 999,
      latitude: lastLatLng.value[1],
      longitude: lastLatLng.value[0],
      title: '我的位置',
    })
  }
  markers.value = list
  if (list.length) {
    includePoints.value = list.map((m) => ({ latitude: m.latitude, longitude: m.longitude }))
  }
}

const lastLatLng = ref<[number, number] | null>(null)

async function pollTrace() {
  // businessId 取当前快递员 id，容错
  const businessId = userStore.userInfo?.id || userStore.userInfo?.userId || ''
  if (!businessId) return
  try {
    const res = await traceLatest(String(businessId), 'courier')
    const ll = pickLatLng(res)
    if (ll) {
      // 后端存 WGS84，展示前转 GCJ-02
      lastLatLng.value = wgs84ToGcj02(ll[0], ll[1])
      center.value = { lng: lastLatLng.value[0], lat: lastLatLng.value[1] }
      buildMarkers()
    }
  } catch (e) {
    // 轮询失败静默，不影响节点展示
  }
}

async function loadRoute() {
  try {
    const r = await taskRoute(id.value)
    routeList.value = normalizeRoute(r)
    // 用首个有坐标的节点作为初始中心
    for (const n of routeList.value) {
      const ll = pickLatLng(n)
      if (ll) {
        center.value = { lng: ll[0], lat: ll[1] }
        break
      }
    }
    buildMarkers()
  } catch (e) {
    routeList.value = []
  }
}

onLoad((options: any) => {
  id.value = options?.id || ''
  loadRoute()
  pollTrace()
  // 每 10 秒轮询一次实时位置
  timer = setInterval(pollTrace, 10000)
})

// 离开页面清除定时器
onUnload(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss">
.map-card {
  padding: 0;
  overflow: hidden;
}
.map {
  width: 100%;
  height: 480rpx;
  border-radius: var(--r-md);
}
.title {
  display: block;
  font-size: var(--f-sub);
  color: var(--c-text-1);
  font-weight: bold;
  margin-bottom: var(--s-2);
}
.node {
  display: flex;
  justify-content: space-between;
  font-size: var(--f-aux);
  color: var(--c-text-2);
  margin-top: var(--s-1);
}
.tip {
  display: block;
  text-align: center;
  font-size: var(--f-tip);
  color: var(--c-text-3);
}
</style>
