<template>
  <view class="page">
    <u-navbar :title="'网点查询'" :back="true" :borderBottom="false" bgColor="#FFFFFF" title-color="#000000" left-icon-color="var(--c-text-1)" placeholder />

    <view class="filter-bar">
      <u-search v-model="keyword" placeholder="搜索网点名称/地址" :showAction="false" bgColor="#FFFFFF" @change="reload" @clear="reload" />
    </view>

    <u-skeleton v-if="loading && list.length === 0" class="sk" :rows="4" title animated />
    <view v-else-if="list.length === 0" class="state-wrap">
      <u-empty mode="list" text="未找到网点" />
    </view>
    <view v-else class="agency-list">
      <view v-for="item in list" :key="item.id" class="card agency-card" hover-class="card-hover">
        <view class="agency-head">
          <text class="agency-name">{{ item.name || '-' }}</text>
          <u-tag v-if="item.orgType === 1" text="营业网点" type="primary" :plain="true" size="mini" />
          <u-tag v-else text="分拨/转运" type="info" :plain="true" size="mini" />
        </view>
        <view class="agency-row">
          <u-icon name="map" color="var(--c-text-3)" size="16" />
          <text class="agency-text">{{ item.fullAddress || '-' }}</text>
        </view>
        <view class="agency-row">
          <u-icon name="phone" color="var(--c-text-3)" size="16" />
          <text class="agency-text">{{ maskPhone(item.contractNumber) }}</text>
        </view>
        <view v-if="item.manager" class="agency-row">
          <u-icon name="account" color="var(--c-text-3)" size="16" />
          <text class="agency-text">负责人：{{ maskName(item.manager) }}</text>
        </view>
        <view class="agency-foot" hover-class="nav-hover" @click.stop="navToAgency(item)">
          <u-icon name="arrow-right" color="var(--c-primary)" size="14" />
          <text class="nav-text">地图导航</text>
        </view>
      </view>
      <u-loadmore :status="loadStatus" />
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app';
import { agencyPage } from '../../../common/api/index.js';
import { maskName, maskPhone } from '../../../common/utils/mask.js';

const keyword = ref('');
const list = ref([]);
const loading = ref(false);
const pageNum = ref(1);
const pageSize = 10;
const total = ref(0);
const loadStatus = ref('loadmore');

const loadList = async (reset = false) => {
  if (loading.value) return;
  if (reset) {
    pageNum.value = 1;
    list.value = [];
  }
  loading.value = true;
  try {
    const data = await agencyPage({
      keyword: keyword.value,
      page: pageNum.value,
      pagesize: pageSize,
    });
    const items = (data && data.items) || [];
    total.value = (data && data.counts) || 0;
    list.value = reset ? items : list.value.concat(items);
    loadStatus.value = list.value.length >= total.value ? 'nomore' : 'loadmore';
  } catch (e) {
    loadStatus.value = 'loadmore';
  } finally {
    loading.value = false;
  }
};

const reload = () => {
  loadStatus.value = 'loadmore';
  loadList(true);
};

// 网点卡片→地图导航（按地址文本检索；网点无坐标契约，《70》§3.1②）
const navToAgency = (item) => {
  const addr = (item && (item.fullAddress || item.address)) || '';
  if (!addr) return;
  // #ifdef H5
  window.open(`https://uri.amap.com/search?keyword=${encodeURIComponent(addr)}`, '_blank');
  // #endif
  // #ifndef H5
  uni.setClipboardData({
    data: addr,
    success: () => uni.showToast({ title: '地址已复制', icon: 'none' }),
  });
  // #endif
};

onPullDownRefresh(async () => {
  await loadList(true);
  uni.stopPullDownRefresh();
});

onReachBottom(() => {
  if (loadStatus.value === 'nomore' || loading.value) return;
  pageNum.value += 1;
  loadList(false);
});

reload();
</script>

<style lang="scss" scoped>
.filter-bar {
  padding: 16rpx 24rpx 0;
}
.state-wrap { padding-top: 120rpx; }
.sk { padding: 16rpx 24rpx; }
.agency-list {
  padding: 16rpx 24rpx;
}
.agency-card { padding: 24rpx; }
.card-hover {
  background-color: rgba(255, 0, 51, 0.04);
}
.agency-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4rpx;
  margin-top: 16rpx;
  padding-top: 12rpx;
  border-top: 1rpx solid var(--c-border);
}
.nav-hover {
  background-color: rgba(255, 0, 51, 0.04);
}
.nav-text {
  font-size: var(--f-tip);
  color: var(--c-primary);
}
.agency-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}
.agency-name {
  font-size: var(--f-sub);
  font-weight: 600;
  color: var(--c-text-1);
}
.agency-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 8rpx;
  font-size: var(--f-aux);
  color: var(--c-text-2);
}
.agency-text {
  line-height: 1.4;
}
</style>
