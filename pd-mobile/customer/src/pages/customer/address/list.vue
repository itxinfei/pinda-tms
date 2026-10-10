<template>
  <view class="page">
    <u-navbar :title="'地址簿'" :back="true" :borderBottom="false" bgColor="#2B6CFF" placeholder />

    <!-- 搜索 -->
    <view class="filter-bar">
      <u-search v-model="keyword" placeholder="搜索姓名/电话/地址" :showAction="false" bgColor="#FFFFFF" @change="reload" @clear="reload" />
    </view>

    <!-- 三态 -->
    <view v-if="loading && list.length === 0" class="state-wrap">
      <u-loading-page :loading="true" :bgColor="'#F5F6F8'" />
    </view>
    <view v-else-if="list.length === 0" class="state-wrap">
      <u-empty mode="list" text="暂无地址，点下方按钮添加" />
    </view>
    <view v-else class="addr-list">
      <view v-for="item in list" :key="item.id" class="card addr-card" @click="onItemClick(item)">
        <view class="addr-head">
          <view class="addr-person">
            <text class="addr-name">{{ maskName(item.name) }}</text>
            <text class="addr-phone">{{ maskPhone(item.phoneNumber) }}</text>
            <u-tag v-if="item.isDefault === 1" text="默认" type="success" :plain="true" size="mini" />
          </view>
          <view v-if="!selectMode" class="addr-ops">
            <u-icon name="edit-pen" color="#2B6CFF" size="20" @click.stop="goEdit(item.id)" />
            <u-icon name="trash" color="#F53F3F" size="20" @click.stop="handleDelete(item)" />
          </view>
        </view>
        <view class="addr-detail">{{ item.fullAddress || item.address || '-' }}</view>
        <view v-if="item.companyName" class="addr-company">{{ item.companyName }}</view>
      </view>
      <u-loadmore :status="loadStatus" />
    </view>

    <!-- 底部新增 -->
    <view v-if="!selectMode" class="action-bar">
      <u-button type="primary" shape="circle" text="新增地址" class="add-btn" @click="goEdit('')" />
    </view>
    <view style="height: 140rpx"></view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onShow, onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app';
import { page, remove } from '../../../common/api/address.js';
import { maskName, maskPhone } from '../../../common/utils/mask.js';

const keyword = ref('');
const list = ref([]);
const loading = ref(false);
const pageNum = ref(1);
const pageSize = 10;
const total = ref(0);
const loadStatus = ref('loadmore');
const selectMode = ref(''); // '' 普通模式；'send'/'receipt' 选择回传

const loadList = async (reset = false) => {
  if (loading.value) return;
  if (reset) {
    pageNum.value = 1;
    list.value = [];
  }
  loading.value = true;
  try {
    // 注意：地址簿分页参数是 pageSize（驼峰大写S）
    const data = await page({
      keyword: keyword.value,
      page: pageNum.value,
      pageSize: pageSize,
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

const onItemClick = (item) => {
  if (selectMode.value) {
    uni.$emit('addressSelected', { role: selectMode.value, item });
    uni.navigateBack({ delta: 1 });
    return;
  }
  goEdit(item.id);
};

const goEdit = (id) => {
  uni.navigateTo({ url: id ? `/pages/customer/address/edit?id=${id}` : '/pages/customer/address/edit' });
};

const handleDelete = (item) => {
  uni.showModal({
    title: '删除地址',
    content: `确定删除「${item.name}」的地址吗？`,
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await remove(item.id);
        uni.showToast({ title: '已删除', icon: 'success' });
        reload();
      } catch (e) { /* 错误已提示 */ }
    },
  });
};

onLoad((options) => {
  selectMode.value = options.selectMode || '';
});

onShow(() => {
  reload();
});

onPullDownRefresh(async () => {
  await loadList(true);
  uni.stopPullDownRefresh();
});

onReachBottom(() => {
  if (loadStatus.value === 'nomore' || loading.value) return;
  pageNum.value += 1;
  loadList(false);
});
</script>

<style lang="scss" scoped>
.filter-bar {
  padding: 16rpx 24rpx 0;
}
.state-wrap { padding-top: 120rpx; }
.addr-list {
  padding: 16rpx 24rpx;
}
.addr-card { padding: 24rpx; }
.addr-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.addr-person {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.addr-name {
  font-size: var(--f-sub);
  font-weight: 600;
  color: var(--c-text-1);
}
.addr-phone {
  font-size: var(--f-aux);
  color: var(--c-text-3);
}
.addr-ops {
  display: flex;
  gap: 24rpx;
}
.addr-detail {
  margin-top: 12rpx;
  font-size: var(--f-body);
  color: var(--c-text-2);
  line-height: 1.5;
}
.addr-company {
  margin-top: 6rpx;
  font-size: var(--f-tip);
  color: var(--c-text-3);
}
.add-btn { height: 88rpx; }
</style>
