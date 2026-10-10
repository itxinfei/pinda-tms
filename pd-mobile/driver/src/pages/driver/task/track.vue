<template>
  <view class="page track-page">
    <view class="map-wrap">
      <map
        class="map"
        :latitude="center.latitude"
        :longitude="center.longitude"
        :markers="markers"
        :polyline="polylines"
        :scale="12"
        show-location
      ></map>
    </view>

    <view class="card control-card">
      <u-button
        type="primary"
        size="mini"
        :text="polling ? '停止刷新' : '实时刷新'"
        :plain="polling"
        @click="togglePolling"
      ></u-button>
      <u-button
        type="warning"
        size="mini"
        text="轨迹回放"
        plain
        @click="onReplay"
      ></u-button>
    </view>

    <view class="card">
      <view class="card-title">当前位置</view>
      <text class="loc-text" v-if="latest">{{ latest.address || (latest.lng + ',' + latest.lat) }}</text>
      <text class="loc-text" v-else>等待定位…</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onUnmounted } from 'vue'
import { traceLatest, traceReplay } from '@/common/api/driver'
import { wgs84ToGcj02 } from '@/common/utils/coord'

const truckId = ref('')
const latest = ref<any>(null)
const polling = ref(false)
let timer: any = null

// 地图中心（GCJ-02）
const center = reactive({ latitude: 39.909, longitude: 116.397 })
const markers = ref<any[]>([])
const polylines = ref<any[]>([])

// 从轨迹点取 WGS84 经纬度（容错 lat/lng 与 latitude/longitude）
function pickLngLat(p: any): [number, number] | null {
  if (!p) return null
  const lng = p.lng ?? p.longitude
  const lat = p.lat ?? p.latitude
  if (lng == null || lat == null) return null
  return [lng, lat]
}

// 修改点：WGS84 -> GCJ-02，地图展示用火星坐标
function toGcj02(p: any): { latitude: number; longitude: number } | null {
  const lw = pickLngLat(p)
  if (!lw) return null
  const [glng, glat] = wgs84ToGcj02(lw[0], lw[1])
  return { longitude: glng, latitude: glat }
}

async function refreshLatest() {
  if (!truckId.value) return
  try {
    const res = await traceLatest(truckId.value, 'truck')
    latest.value = res
    const g = toGcj02(res)
    if (g) {
      center.latitude = g.latitude
      center.longitude = g.longitude
      markers.value = [
        {
          id: 1,
          latitude: g.latitude,
          longitude: g.longitude,
          title: '车辆',
          iconPath: '/static/logo.png',
          width: 24,
          height: 24,
        },
      ]
    }
  } catch (e) {
    latest.value = null
  }
}

async function onReplay() {
  if (!truckId.value) return
  try {
    const res = await traceReplay(truckId.value, 'truck')
    const arr = Array.isArray(res) ? res : res?.points || res?.items || []
    const pts: any[] = []
    for (const p of arr) {
      const g = toGcj02(p)
      if (g) pts.push(g)
    }
    polylines.value = [
      {
        points: pts,
        color: '#2B6CFF',
        width: 6,
        dottedLine: false,
      },
    ]
    if (pts.length > 0) {
      center.latitude = pts[pts.length - 1].latitude
      center.longitude = pts[pts.length - 1].longitude
    }
    uni.showToast({ title: '已加载回放轨迹', icon: 'none' })
  } catch (e) {
    uni.showToast({ title: '回放失败', icon: 'none' })
  }
}

function togglePolling() {
  polling.value = !polling.value
  if (polling.value) {
    refreshLatest()
    timer = setInterval(refreshLatest, 10000)
  } else if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onLoad((opt: any) => {
  truckId.value = opt.truckId || ''
  refreshLatest()
  // 进入即开启实时刷新
  polling.value = true
  timer = setInterval(refreshLatest, 10000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss">
.track-page {
  padding: 0;
}
.map-wrap {
  width: 100%;
  height: 60vh;
}
.map {
  width: 100%;
  height: 100%;
}
.control-card {
  display: flex;
  justify-content: space-around;
  margin: var(--s-3) var(--s-4);
}
.card-title {
  font-size: var(--f-sub);
  color: var(--c-text-1);
  font-weight: bold;
  margin-bottom: var(--s-2);
}
.loc-text {
  font-size: var(--f-body);
  color: var(--c-text-1);
}
</style>
