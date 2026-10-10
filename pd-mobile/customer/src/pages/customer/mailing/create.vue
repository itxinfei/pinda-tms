<template>
  <view class="page">
    <u-navbar title="寄件" autoBack></u-navbar>
    <u-form :model="form" ref="uForm">
      <view class="card">
        <view class="form-title">寄件人</view>
        <u-form-item label="寄件地址" borderBottom @click="pickAddress('send')">
          <text v-if="sendAddr">{{ sendAddr.name }} {{ sendAddr.address }}</text>
          <text v-else class="placeholder">请选择寄件地址</text>
          <u-icon name="arrow-right" slot="right"></u-icon>
        </u-form-item>
        <u-form-item label="取件方式" borderBottom>
          <u-radio-group v-model="form.pickupType">
            <u-radio label="上门取件" :name="2" />
            <u-radio label="网点自寄" :name="1" />
          </u-radio-group>
        </u-form-item>
        <u-form-item label="取件时间" borderBottom @click="showTime = true">
          <text v-if="form.pickUpTime">{{ form.pickUpTime }}</text>
          <text v-else class="placeholder">请选择</text>
        </u-form-item>
      </view>

      <view class="card">
        <view class="form-title">收件人</view>
        <u-form-item label="收件地址" borderBottom @click="pickAddress('receipt')">
          <text v-if="receiptAddr">{{ receiptAddr.name }} {{ receiptAddr.address }}</text>
          <text v-else class="placeholder">请选择收件地址</text>
          <u-icon name="arrow-right" slot="right"></u-icon>
        </u-form-item>
      </view>

      <view class="card">
        <view class="form-title">物品信息</view>
        <u-form-item label="物品类型" borderBottom @click="showGoods = true">
          <text v-if="goodsTypeName">{{ goodsTypeName }}</text>
          <text v-else class="placeholder">请选择</text>
        </u-form-item>
        <u-form-item label="物品名称" borderBottom>
          <u--input v-model="form.goodsName" placeholder="如：文件/服装" />
        </u-form-item>
        <u-form-item label="重量(kg)" borderBottom>
          <u--input v-model="form.goodsWeight" type="number" placeholder="请输入重量" />
        </u-form-item>
        <u-form-item label="支付方式" borderBottom>
          <u-radio-group v-model="form.payMethod">
            <u-radio label="预结" :name="1" />
            <u-radio label="到付" :name="2" />
          </u-radio-group>
        </u-form-item>
      </view>

      <view class="card price-card" v-if="amount">
        <text class="price-label">预估运费</text>
        <text class="price-num">¥{{ amount }}</text>
      </view>
    </u-form>

    <u-gap height="80"></u-gap>
    <view class="action-bar">
      <u-button
        type="primary"
        text="提交订单"
        shape="circle"
        :loading="submitting"
        @click="onSubmit"
      />
    </view>

    <u-datetime-picker
      v-if="showTime"
      :show="showTime"
      mode="datetime"
      @confirm="onTimeConfirm"
      @cancel="showTime = false"
    />
    <u-picker
      v-if="showGoods"
      :show="showGoods"
      :columns="goodsColumns"
      @confirm="onGoodsConfirm"
      @cancel="showGoods = false"
    />
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import {
  goodsTypeAll,
  addressPage,
  mailingTotalPrice,
  createMailing,
} from '@/common/api/customer'
import { onShow } from '@dcloudio/uni-app'

const form = reactive({
  sendAddress: '',
  receiptAddress: '',
  pickupType: 2,
  pickUpTime: '',
  goodsType: '',
  goodsName: '',
  goodsWeight: '',
  payMethod: 1,
})
const sendAddr = ref<any>(null)
const receiptAddr = ref<any>(null)
const goodsTypeName = ref('')
const goodsColumns = ref<any[][]>([[]])
const goodsList = ref<any[]>([])
const showTime = ref(false)
const showGoods = ref(false)
const amount = ref('')
const submitting = ref(false)

onShow(() => {
  const s = uni.getStorageSync('sel_addr_send')
  if (s) {
    sendAddr.value = s
    form.sendAddress = s.id
  }
  const r = uni.getStorageSync('sel_addr_receipt')
  if (r) {
    receiptAddr.value = r
    form.receiptAddress = r.id
  }
  loadGoods()
})

async function loadGoods() {
  try {
    const res = await goodsTypeAll()
    goodsList.value = Array.isArray(res) ? res : res?.data || []
    goodsColumns.value = [goodsList.value.map((g: any) => g.name)]
  } catch (e) {}
}

function pickAddress(type: string) {
  uni.navigateTo({ url: '/pages/customer/address/list?select=' + type })
}

function onTimeConfirm(e: any) {
  form.pickUpTime = e.value || ''
  showTime.value = false
}
function onGoodsConfirm(e: any) {
  const idx = e.indexs ? e.indexs[0] : 0
  const g = goodsList.value[idx]
  if (g) {
    form.goodsType = g.id
    goodsTypeName.value = g.name
  }
  showGoods.value = false
}

async function calcPrice() {
  if (!form.sendAddress || !form.receiptAddress || !form.goodsType || !form.goodsWeight) return
  try {
    const res = await mailingTotalPrice({ ...form, goodsWeight: Number(form.goodsWeight) })
    amount.value = res?.amount ?? res?.data?.amount ?? ''
  } catch (e) {}
}

let timer: any
function schedulePrice() {
  if (timer) clearTimeout(timer)
  timer = setTimeout(calcPrice, 300)
}

async function onSubmit() {
  if (!form.sendAddress || !form.receiptAddress || !form.goodsName || !form.goodsWeight) {
    uni.showToast({ title: '请完善寄件信息', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await createMailing({ ...form, goodsWeight: Number(form.goodsWeight) })
    uni.showToast({ title: '下单成功', icon: 'success' })
    setTimeout(() => uni.redirectTo({ url: '/pages/customer/mailing/list' }), 600)
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

// 监听关键字段变化触发运费试算（防抖）
watch(
  () => [
    form.sendAddress,
    form.receiptAddress,
    form.goodsType,
    form.goodsWeight,
    form.pickupType,
    form.payMethod,
  ],
  schedulePrice
)
</script>

<style lang="scss">
.form-title {
  font-size: var(--f-sub);
  color: var(--c-text-1);
  font-weight: bold;
  margin-bottom: var(--s-2);
}
.placeholder {
  color: var(--c-text-4);
  font-size: var(--f-body);
}
.price-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.price-label {
  font-size: var(--f-body);
  color: var(--c-text-2);
}
.price-num {
  font-size: var(--f-title);
  color: var(--c-error);
  font-weight: bold;
}
</style>
