<template>
  <view class="page">
    <u-navbar :title="'网点查询'" :back="true" :borderBottom="false" bgColor="#2B6CFF" placeholder />

    <view class="filter-bar">
      <u-search v-model="keyword" placeholder="搜索网点名称/地址" :showAction="false" bgColor="#FFFFFF" @change="reload" @clear="reload" />
    </view>

    <view v-if="loading && list.length === 0" class="state-wrap">
      <u-loading-page :loading="true" :bgColor="'#F5F6F8'" />
    </view>
    <view v-else-if="list.length === 0" class="state-wrap">
      <u-empty mode="list" text="未找到网点" />
    </view>
    <view v-else class="agency-list">
      <view v-for="item in list" :key="item.id" class="card agency-card">
        <view class="agency-head">
          <text class="agency-name">{{ item.name || '-' }}</text>
          <u-tag v-if="item.orgType === 1" text="营业网点" type="primary" :plain="true" size="mini" />
          <u-tag v-else text="分拨/转运" type="info" :plain="true" size="mini" />
        </view>
        <view class="agency-row">
          <u-icon name="map" color="#86909C" size="16" />
          <text class="agency-text">{{ item.fullAddress || '-' }}</text>
        </view>
        <view class="agency-row">
          <u-icon name="phone" color="#86909C" size="16" />
          <text class="agency-text">{{ maskPhone(item.contractNumber) }}</text>
        </view>
        <view v-if="item.manager" class="agency-row">
          <u-icon name="account" color="#86909C" size="16" />
          <text class="agency-text">负责人：{{ maskName(item.manager) }}</text>
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
.agency-list {
  padding: 16rpx 24rpx;
}
.agency-card { padding: 24rpx; }
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
