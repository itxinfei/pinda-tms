<template>
  <view class="page">
    <u-navbar :title="'我的运单'" :back="true" :borderBottom="false" bgColor="#FFFFFF" title-color="#000000" left-icon-color="var(--c-text-1)" placeholder />

    <!-- 筛选 -->
    <view class="filter-bar">
      <u-search v-model="keyword" placeholder="搜索订单号" :showAction="false" bgColor="#FFFFFF" @change="onSearch" @clear="reload" />
      <view class="mail-type-tabs">
        <view
          v-for="t in mailTypes"
          :key="t.value"
          class="mail-type-tab"
          :class="{ active: mailType === t.value }"
          @click="switchType(t.value)"
        >{{ t.label }}</view>
      </view>
      <view class="mail-type-tabs">
        <view
          v-for="t in statusTabs"
          :key="t.key"
          class="mail-type-tab"
          :class="{ active: statusKey === t.key }"
          @click="switchStatus(t.key)"
        >{{ t.label }}</view>
      </view>
    </view>

    <!-- 列表 -->
    <u-skeleton v-if="loading && list.length === 0" class="sk" :rows="4" title animated />
    <view v-else-if="error" class="state-wrap">
      <u-empty mode="network" text="加载失败，请检查网络后重试" />
      <view class="retry-wrap">
        <u-button type="primary" size="small" shape="circle" text="重试" @click="reload" />
      </view>
    </view>
    <view v-else-if="list.length === 0" class="state-wrap">
      <u-empty mode="list" text="暂无运单" />
    </view>
    <view v-else class="order-list">
      <view v-for="item in list" :key="item.id" class="card order-card" hover-class="card-hover" @click="goDetail(item.id)">
        <view class="order-head">
          <view class="order-no">单号 {{ item.id }}</view>
          <u-tag :text="statusInfo(item.status).label" :type="statusInfo(item.status).type" :plain="false" />
        </view>
        <view class="order-route">
          <view class="route-item">
            <view class="route-dot send"></view>
            <view class="route-text">{{ item.senderFullAddress || item.senderAddress || '-' }}</view>
          </view>
          <view class="route-item">
            <view class="route-dot recv"></view>
            <view class="route-text">{{ item.receiverFullAddress || item.receiverAddress || '-' }}</view>
          </view>
        </view>
        <view class="order-foot">
          <view class="foot-left">
            <text class="amount">¥{{ item.amount ?? '-' }}</text>
            <text class="pay">{{ payMethodLabel(item.paymentMethod) }}</text>
            <text class="time">{{ fmtTime(item.createTime) }}</text>
          </view>
          <view v-if="item.routeDTO && item.routeDTO.msg" class="foot-route">{{ item.routeDTO.msg }}</view>
        </view>
      </view>
      <u-loadmore :status="loadStatus" />
    </view>

    <u-tabbar :value="1" :fixed="true" :placeholder="true" :safeAreaInsetBottom="true">
      <u-tabbar-item text="首页" icon="home" @click="goHome" />
      <u-tabbar-item text="我的运单" icon="list" @click="goList" />
      <u-tabbar-item text="我的" icon="account" @click="goProfile" />
    </u-tabbar>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app';
import { page, statusFilters } from '../../../common/api/mailing.js';
import { statusInfo, payMethodLabel, fmtTime } from '../../../common/utils/order.js';
import { useUserStore } from '../../../stores/user.js';

const userStore = useUserStore();
const keyword = ref('');
const mailType = ref(0); // 0 我寄的 / 1 我收的
const mailTypes = [
  { label: '我寄的', value: 0 },
  { label: '我收的', value: 1 },
];
const list = ref([]);
const loading = ref(false);
const error = ref(false);
const pageNum = ref(1);
const pageSize = 10;
const total = ref(0);
const loadStatus = ref('loadmore');

// 状态筛选档位（后端 status-filters 下发，禁前端硬编码；失败保底"全部"）
const statusTabs = ref([{ key: 'all', label: '全部', statusList: null }]);
const statusKey = ref('all');
let statusTabsLoaded = false;
const loadStatusTabs = async () => {
  if (statusTabsLoaded) return;
  statusTabsLoaded = true;
  try {
    const data = await statusFilters();
    if (Array.isArray(data) && data.length) {
      statusTabs.value = data;
    }
  } catch (e) {
    /* 保底全部档 */
  }
};
const switchStatus = (key) => {
  if (statusKey.value === key) return;
  statusKey.value = key;
  reload();
};

const loadList = async (reset = false) => {
  if (loading.value) return;
  if (reset) {
    pageNum.value = 1;
    list.value = [];
  }
  loading.value = true;
  error.value = false;
  try {
    const params = {
      keyword: keyword.value,
      page: pageNum.value,
      pagesize: pageSize,
      mailType: mailType.value,
    };
    const curStatus = statusTabs.value.find((t) => t.key === statusKey.value);
    if (curStatus && Array.isArray(curStatus.statusList) && curStatus.statusList.length) {
      params.statusList = curStatus.statusList.join(',');
    }
    const data = await page(params);
    const items = (data && data.items) || [];
    total.value = (data && data.counts) || 0;
    list.value = reset ? items : list.value.concat(items);
    loadStatus.value = list.value.length >= total.value ? 'nomore' : 'loadmore';
  } catch (e) {
    loadStatus.value = 'loadmore';
    if (list.value.length === 0) error.value = true;
  } finally {
    loading.value = false;
  }
};

const reload = () => {
  loadStatus.value = 'loadmore';
  loadList(true);
};
const switchType = (v) => {
  mailType.value = v;
  reload();
};
const onSearch = () => reload();
const goDetail = (id) => uni.navigateTo({ url: `/pages/customer/mailing/detail?id=${id}` });
const goHome = () => uni.reLaunch({ url: '/pages/customer/home/index' });
const goList = () => uni.reLaunch({ url: '/pages/customer/mailing/list' });
const goProfile = () => uni.reLaunch({ url: '/pages/customer/profile/index' });

onPullDownRefresh(async () => {
  await loadList(true);
  uni.stopPullDownRefresh();
});

onReachBottom(() => {
  if (loadStatus.value === 'nomore' || loading.value) return;
  pageNum.value += 1;
  loadList(false);
});

onShow(() => {
  if (!userStore.isLogin) {
    uni.reLaunch({ url: '/pages/customer/login/index' });
    return;
  }
  loadStatusTabs();
  loadList(true);
});
</script>

<style lang="scss" scoped>
.filter-bar {
  padding: 16rpx 24rpx 0;
}
.mail-type-tabs {
  display: flex;
  margin-top: 16rpx;
}
.mail-type-tab {
  padding: 8rpx 32rpx;
  margin-right: 16rpx;
  border-radius: var(--r-round);
  background: var(--c-surface);
  color: var(--c-text-2);
  font-size: var(--f-aux);
}
.mail-type-tab.active {
  background: var(--c-primary);
  color: #fff;
}
.state-wrap {
  padding-top: 120rpx;
}
.retry-wrap {
  display: flex;
  justify-content: center;
  padding: 32rpx 0;
}
.sk {
  padding: 16rpx 24rpx;
}
.card-hover {
  background-color: rgba(255, 0, 51, 0.04);
}
.order-list {
  padding: 16rpx 24rpx;
}
.order-card {
  padding: 24rpx;
}
.order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.order-no {
  font-size: var(--f-sub);
  color: var(--c-text-1);
  font-weight: 600;
}
.order-route {
  margin-top: 16rpx;
}
.route-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 8rpx;
}
.route-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  margin: 10rpx 12rpx 0 0;
  flex-shrink: 0;
}
.route-dot.send { background: var(--c-success); }
.route-dot.recv { background: var(--c-primary); }
.route-text {
  font-size: var(--f-aux);
  color: var(--c-text-2);
  line-height: 1.5;
}
.order-foot {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid var(--c-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.foot-left {
  font-size: var(--f-tip);
  color: var(--c-text-3);
}
.amount {
  font-size: var(--f-sub);
  color: var(--c-error);
  font-weight: 600;
  margin-right: 12rpx;
}
.pay { margin-right: 12rpx; }
.foot-route {
  max-width: 320rpx;
  font-size: var(--f-tip);
  color: var(--c-primary);
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
