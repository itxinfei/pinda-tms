<template>
  <view class="page track-page">
    <u-navbar title="实时轨迹" autoBack></u-navbar>
    <map
      class="track-map"
      :latitude="center.lat"
      :longitude="center.lng"
      :markers="markers"
      v-if="center.lat"
    ></map>
    <view class="card" v-if="trace">
      <text class="section-title">轨迹节点</text>
      <u-time-line v-if="trace.tracks && trace.tracks.length">
        <u-time-line-item v-for="(t, i) in trace.tracks" :key="i">
          <template #content>
            <view class="tl-item">
              <text class="tl-msg">{{ t.msg || t.status || '' }}</text>
              <text class="tl-time">{{ t.time || '' }}</text>
            </view>
          </template>
        </u-time-line-item>
      </u-time-line>
      <u-empty v-else mode="list" text="暂无轨迹节点"></u-empty>
    </view>
    <u-empty v-if="!trace" mode="list" text="暂无轨迹"></u-empty>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { orderTrace } from '@/common/api/customer'
import { wgs84ToGcj02 } from '@/common/utils/coord'

const orderId = ref('')
const trace = ref<any>(null)
const center = ref({ lat: 0, lng: 0 })
const markers = ref<any[]>([])

onLoad((opt: any) => {
  orderId.value = opt.orderId || opt.id
  load()
})

async function load() {
  try {
    const res = await orderTrace(orderId.value)
    trace.value = res
    const tracks = (res && res.tracks) || []
    if (tracks.length) {
      const last = tracks[tracks.length - 1]
      if (last && last.lng != null && last.lat != null) {
        const [lng, lat] = wgs84ToGcj02(Number(last.lng), Number(last.lat))
        center.value = { lat, lng }
        markers.value = [
          { id: 1, latitude: lat, longitude: lng, title: '当前位置', width: 28, height: 28 },
        ]
      }
    }
  } catch (e) {}
}
</script>

<style lang="scss">
.track-page {
  padding: 0;
  display: flex;
  flex-direction: column;
}
.track-map {
  width: 100%;
  height: 50vh;
}
.section-title {
  display: block;
  font-size: var(--f-sub);
  color: var(--c-text-1);
  font-weight: bold;
  margin-bottom: var(--s-3);
}
.tl-item {
  display: flex;
  flex-direction: column;
}
.tl-msg {
  font-size: var(--f-body);
  color: var(--c-text-2);
}
.tl-time {
  font-size: var(--f-tip);
  color: var(--c-text-3);
  margin-top: var(--s-1);
}
</style>
