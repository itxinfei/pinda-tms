<template>
  <view class="page track-page">
    <u-navbar :title="'实时轨迹'" :back="true" :borderBottom="false" bgColor="#2B6CFF" />

    <view v-if="loading" class="state-wrap">
      <u-loading-page :loading="true" :bgColor="'#F5F6F8'" />
    </view>
    <view v-else-if="!hasTrack" class="state-wrap">
      <u-empty mode="map" text="暂无轨迹数据" />
      <view class="track-tip">订单调度后将展示实时轨迹</view>
    </view>
    <template v-else>
      <!-- 地图（H5 端 uni-app 原生 map；坐标已转 GCJ-02） -->
      <map
        class="track-map"
        :latitude="center.lat"
        :longitude="center.lng"
        :markers="markers"
        :polyline="polyline"
        :show-location="true"
        scale="14"
      />
      <view class="track-panel">
        <view class="panel-row">
          <text class="panel-label">最新位置</text>
          <text class="panel-value">{{ latestMsg }}</text>
        </view>
        <view class="panel-row">
          <text class="panel-label">订单状态</text>
          <text class="panel-value">{{ statusInfo(orderStatus).label }}</text>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { trace } from '../../../common/api/index.js';
import { safeToGcj02 } from '../../../common/utils/coord.js';
import { statusInfo } from '../../../common/utils/order.js';

const id = ref('');
const loading = ref(false);
const orderStatus = ref(null);
const tracks = ref([]);
const latestMsg = ref('加载中...');

const hasTrack = computed(() => tracks.value && tracks.value.length > 0);

// 轨迹点（后端 WGS84）→ 地图点（GCJ-02）
const points = computed(() => {
  if (!hasTrack.value) return [];
  return tracks.value
    .map((t) => safeToGcj02(t.lng ?? t.longitude, t.lat ?? t.latitude))
    .filter((p) => p !== null);
});

const center = computed(() => {
  const p = points.value[points.value.length - 1] || { lng: 114.3055, lat: 30.5928 };
  return p;
});

const markers = computed(() => {
  if (!hasTrack.value) return [];
  const arr = [];
  const ps = points.value;
  if (ps.length > 0) {
    // 起点（默认标记 + label）
    arr.push({
      id: 1,
      latitude: ps[0].lat,
      longitude: ps[0].lng,
      width: 20,
      height: 20,
      label: { content: '起点', color: '#00B42A', fontSize: 12, anchorX: -8, anchorY: -28 },
    });
    // 最新点
    const last = ps[ps.length - 1];
    arr.push({
      id: 2,
      latitude: last.lat,
      longitude: last.lng,
      width: 20,
      height: 20,
      label: { content: '当前位置', color: '#2B6CFF', fontSize: 12, anchorX: -20, anchorY: -28 },
    });
  }
  return arr;
});

const polyline = computed(() => {
  if (points.value.length < 2) return [];
  return [
    {
      points: points.value,
      color: '#2B6CFF',
      width: 4,
      dottedLine: false,
    },
  ];
});

const load = async () => {
  loading.value = true;
  try {
    const data = await trace(id.value);
    orderStatus.value = data ? data.orderStatus : null;
    tracks.value = (data && data.tracks) || [];
    if (tracks.value.length > 0) {
      const last = tracks.value[tracks.value.length - 1];
      latestMsg.value = `更新于 ${last.time || ''}`;
    } else {
      latestMsg.value = '暂无轨迹数据';
    }
  } catch (e) {
    tracks.value = [];
  } finally {
    loading.value = false;
  }
};

onLoad((options) => {
  id.value = options.id || '';
  load();
});
</script>

<style lang="scss" scoped>
.track-page { padding: 0; }
.state-wrap { padding-top: 120rpx; }
.track-tip {
  text-align: center;
  color: var(--c-text-3);
  font-size: var(--f-tip);
  margin-top: 16rpx;
}
.track-map {
  width: 100%;
  height: 70vh;
}
.track-panel {
  margin: 16rpx 24rpx 32rpx;
  padding: 24rpx;
  background: var(--c-surface);
  border-radius: var(--r-md);
  box-shadow: var(--shadow-card);
}
.panel-row {
  display: flex;
  justify-content: space-between;
  padding: 8rpx 0;
  font-size: var(--f-body);
}
.panel-label { color: var(--c-text-3); }
.panel-value { color: var(--c-text-1); }
</style>
