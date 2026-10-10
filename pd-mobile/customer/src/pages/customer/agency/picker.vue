<template>
  <view class="page">
    <u-navbar title="选择网点" autoBack></u-navbar>
    <u-search
      v-model="keyword"
      placeholder="搜索网点名称"
      @search="loadData"
      @custom="loadData"
    ></u-search>
    <u-empty v-if="!loading && list.length === 0" mode="list" text="暂无网点"></u-empty>
    <view v-for="a in list" :key="a.id" class="card ag-card" @click="onPick(a)">
      <text class="ag-name">{{ a.name }}</text>
      <text class="ag-addr">{{ a.address || '' }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { agencyPage } from '@/common/api/customer'

const list = ref<any[]>([])
const loading = ref(false)
const keyword = ref('')

onShow(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await agencyPage({ page: 1, pagesize: 50, keyword: keyword.value })
    list.value = (res && res.items) || []
  } catch (e) {
  } finally {
    loading.value = false
  }
}

function onPick(a: any) {
  uni.setStorageSync('sel_agency', a)
  uni.navigateBack()
}
</script>

<style lang="scss">
.ag-card {
  padding: var(--s-4);
}
.ag-name {
  display: block;
  font-size: var(--f-body);
  color: var(--c-text-1);
  font-weight: bold;
}
.ag-addr {
  display: block;
  font-size: var(--f-aux);
  color: var(--c-text-3);
  margin-top: var(--s-1);
}
</style>
