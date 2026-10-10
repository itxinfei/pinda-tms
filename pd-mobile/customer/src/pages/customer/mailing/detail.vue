<template>
  <view class="page">
    <u-navbar :title="'运单详情'" :back="true" :borderBottom="false" bgColor="#2B6CFF" placeholder />

    <!-- 加载/空态 -->
    <view v-if="loading && !detail" class="state-wrap">
      <u-loading-page :loading="true" :bgColor="'#F5F6F8'" />
    </view>
    <view v-else-if="!detail" class="state-wrap">
      <u-empty mode="data" text="运单不存在" />
    </view>
    <template v-else>
      <!-- 状态条 -->
      <view class="status-hero">
        <u-tag :text="statusInfo(detail.status).label" :type="statusInfo(detail.status).type" :plain="false" size="large" />
        <view class="status-time">{{ fmtTime(detail.planPickUpTime || detail.actualDispathedTime) }}</view>
      </view>

      <!-- 基本信息 -->
      <view class="card">
        <view class="card-title">订单信息</view>
        <view class="info-row"><text class="info-label">订单号</text><text class="info-value">{{ detail.id }}</text></view>
        <view class="info-row"><text class="info-label">运单号</text><text class="info-value">{{ detail.tranOrderId || '-' }}</text></view>
        <view class="info-row"><text class="info-label">计划取件</text><text class="info-value">{{ fmtTime(detail.planPickUpTime) }}</text></view>
        <view class="info-row"><text class="info-label">派送完成</text><text class="info-value">{{ fmtTime(detail.actualDispathedTime) }}</text></view>
        <view class="info-row"><text class="info-label">取消时间</text><text class="info-value">{{ fmtTime(detail.cancelTime) }}</text></view>
        <view v-if="detail.name" class="info-row">
          <text class="info-label">快递员</text>
          <text class="info-value">{{ maskName(detail.name) }} {{ maskPhone(detail.mobile) }}</text>
        </view>
      </view>

      <!-- 物流时间轴 -->
      <view class="card">
        <view class="card-title">物流动态</view>
        <view v-if="routeList.length === 0" class="route-empty">暂无物流动态</view>
        <view v-else class="route-timeline">
          <view v-for="(r, i) in routeList" :key="i" class="route-node">
            <view class="node-left">
              <view class="node-dot" :class="{ last: i === 0 }"></view>
              <view v-if="i !== routeList.length - 1" class="node-line"></view>
            </view>
            <view class="node-body">
              <view class="node-msg">{{ r.msg || '节点更新' }}</view>
              <view class="node-time">{{ fmtTime(r.time) }}</view>
            </view>
          </view>
        </view>
      </view>

      <!-- 底部操作 -->
      <view class="action-bar detail-actions">
        <u-button v-if="canPay" type="primary" shape="circle" text="去支付" class="action-btn" @click="handlePay" />
        <u-button v-if="canCancel" type="error" shape="circle" plain text="取消订单" class="action-btn" @click="handleCancel" />
        <u-button type="primary" shape="circle" plain text="查看实时轨迹" class="action-btn" @click="goTrack" />
      </view>
      <view style="height: 140rpx"></view>
    </template>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { detail as fetchDetail, route as fetchRoute, pay, cancel } from '../../../common/api/mailing.js';
import { statusInfo, fmtTime } from '../../../common/utils/order.js';
import { maskName, maskPhone } from '../../../common/utils/mask.js';

const id = ref('');
const detail = ref(null);
const routeList = ref([]);
const loading = ref(false);

// 仅"待取件(23000)"可取消、可支付
const canCancel = computed(() => detail.value && detail.value.status === 23000);
const canPay = computed(() => detail.value && detail.value.status === 23000);

const loadDetail = async () => {
  loading.value = true;
  try {
    detail.value = await fetchDetail(id.value);
  } catch (e) {
    detail.value = null;
  } finally {
    loading.value = false;
  }
};

const loadRoute = async () => {
  try {
    const data = await fetchRoute(id.value);
    const list = data || [];
    routeList.value = list.slice().sort((a, b) => (a.time > b.time ? 1 : -1));
  } catch (e) {
    routeList.value = [];
  }
};

const handlePay = async () => {
  uni.showModal({
    title: '确认支付',
    content: '确定支付该订单运费吗？',
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await pay(id.value);
        uni.showToast({ title: '支付成功', icon: 'success' });
        loadDetail();
        loadRoute();
      } catch (e) { /* 错误已提示 */ }
    },
  });
};

const handleCancel = async () => {
  uni.showModal({
    title: '取消订单',
    content: '取消后订单将关闭并全额退款，确定取消吗？',
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await cancel(id.value);
        uni.showToast({ title: '已取消', icon: 'success' });
        loadDetail();
        loadRoute();
      } catch (e) { /* 错误已提示 */ }
    },
  });
};

const goTrack = () => uni.navigateTo({ url: `/pages/customer/mailing/track?id=${id.value}` });

onLoad((options) => {
  id.value = options.id || '';
  loadDetail();
  loadRoute();
});
</script>

<style lang="scss" scoped>
.state-wrap { padding-top: 120rpx; }
.status-hero {
  margin: 24rpx;
  padding: 32rpx;
  background: var(--c-surface);
  border-radius: var(--r-md);
  box-shadow: var(--shadow-card);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.status-time {
  font-size: var(--f-tip);
  color: var(--c-text-3);
}
.card-title {
  font-size: var(--f-sub);
  font-weight: 600;
  color: var(--c-text-1);
  margin-bottom: 16rpx;
}
.info-row {
  display: flex;
  justify-content: space-between;
  padding: 10rpx 0;
  font-size: var(--f-body);
}
.info-label { color: var(--c-text-3); }
.info-value { color: var(--c-text-1); }
.route-empty {
  padding: 32rpx 0;
  text-align: center;
  color: var(--c-text-3);
  font-size: var(--f-aux);
}
.route-timeline { padding: 8rpx 0; }
.route-node { display: flex; }
.node-left {
  width: 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 12rpx;
}
.node-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: var(--c-primary);
  margin-top: 8rpx;
  flex-shrink: 0;
}
.node-dot.last { background: var(--c-success); }
.node-line {
  flex: 1;
  width: 2rpx;
  background: var(--c-border);
  min-height: 40rpx;
}
.node-body { padding-bottom: 28rpx; }
.node-msg {
  font-size: var(--f-body);
  color: var(--c-text-1);
}
.node-time {
  margin-top: 4rpx;
  font-size: var(--f-tip);
  color: var(--c-text-3);
}
.detail-actions {
  display: flex;
  gap: 16rpx;
  padding-bottom: 24rpx;
}
.action-btn { flex: 1; }
</style>
