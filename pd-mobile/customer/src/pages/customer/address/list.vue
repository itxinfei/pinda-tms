<template>
  <view class="page">
    <u-navbar :title="selectMode ? '选择地址' : '地址簿'" autoBack></u-navbar>
    <u-loading-page :loading="loading"></u-loading-page>
    <u-empty v-if="!loading && list.length === 0" mode="list" text="暂无地址"></u-empty>
    <view
      v-for="a in list"
      :key="a.id"
      class="card addr-card"
      @click="onPick(a)"
    >
      <view class="addr-head">
        <text class="addr-name">{{ a.name }}</text>
        <text class="addr-phone">{{ maskPhone(a.phone) }}</text>
        <u-tag v-if="a.isDefault" text="默认" type="primary" size="mini" />
      </view>
      <text class="addr-detail">{{ a.province }}{{ a.city }}{{ a.district }}{{ a.address }}</text>
      <view class="addr-ops" v-if="!selectMode">
        <u-button text="编辑" size="mini" plain @click.stop="onEdit(a)" />
        <u-button text="删除" size="mini" plain type="error" @click.stop="onDelete(a)" />
      </view>
    </view>
    <view class="action-bar">
      <u-button type="primary" text="新增地址" shape="circle" @click="onAdd"></u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { addressList, deleteAddress } from '@/common/api/customer'
import { maskPhone } from '@/common/utils/desensitive'
import { onShow, onLoad } from '@dcloudio/uni-app'

const list = ref<any[]>([])
const loading = ref(false)
const selectMode = ref('')

onLoad((opt: any) => {
  selectMode.value = opt.select || ''
})
onShow(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await addressList({ page: 1, pageSize: 50 })
    list.value = (res && res.items) || []
  } catch (e) {
  } finally {
    loading.value = false
  }
}

function onPick(a: any) {
  if (!selectMode.value) return
  uni.setStorageSync('sel_addr_' + selectMode.value, a)
  uni.navigateBack()
}
function onEdit(a: any) {
  uni.navigateTo({ url: '/pages/customer/address/edit?id=' + a.id })
}
function onAdd() {
  uni.navigateTo({ url: '/pages/customer/address/edit' })
}
async function onDelete(a: any) {
  uni.showModal({
    title: '提示',
    content: '确定删除该地址？',
    success: async (r) => {
      if (r.confirm) {
        await deleteAddress(a.id)
        uni.showToast({ title: '已删除', icon: 'success' })
        loadData()
      }
    },
  })
}
</script>

<style lang="scss">
.addr-card {
  padding: var(--s-4);
}
.addr-head {
  display: flex;
  align-items: center;
  margin-bottom: var(--s-2);
}
.addr-name {
  font-size: var(--f-body);
  color: var(--c-text-1);
  font-weight: bold;
  margin-right: var(--s-2);
}
.addr-phone {
  font-size: var(--f-aux);
  color: var(--c-text-2);
  margin-right: var(--s-2);
}
.addr-detail {
  display: block;
  font-size: var(--f-aux);
  color: var(--c-text-2);
}
.addr-ops {
  display: flex;
  justify-content: flex-end;
  gap: var(--s-2);
  margin-top: var(--s-3);
}
</style>
