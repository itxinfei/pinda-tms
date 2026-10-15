<template>
  <view class="page">
    <u-navbar title="运费查询" :back="true" :borderBottom="false" bgColor="var(--c-primary)" placeholder />

    <!-- 地址信息：复用地址簿选择回传（与寄件页同一交互） -->
    <view class="card">
      <view class="card-title">地址信息</view>
      <view class="addr-row" @click="pickAddress('send')">
        <view class="addr-icon send">寄</view>
        <view class="addr-body">
          <view class="addr-name">{{ sendAddr ? sendAddr.fullAddress || '已选择' : '选择发件地址' }}</view>
          <view v-if="sendAddr" class="addr-contact">{{ maskName(sendAddr.name) }} {{ maskPhone(sendAddr.phoneNumber) }}</view>
        </view>
        <u-icon name="arrow-right" color="#86909C" size="20" />
      </view>
      <view class="addr-row" @click="pickAddress('receipt')">
        <view class="addr-icon recv">收</view>
        <view class="addr-body">
          <view class="addr-name">{{ recvAddr ? recvAddr.fullAddress || '已选择' : '选择收件地址' }}</view>
          <view v-if="recvAddr" class="addr-contact">{{ maskName(recvAddr.name) }} {{ maskPhone(recvAddr.phoneNumber) }}</view>
        </view>
        <u-icon name="arrow-right" color="#86909C" size="20" />
      </view>
    </view>

    <!-- 货物信息 -->
    <view class="card">
      <view class="card-title">货物信息</view>
      <view class="form-row">
        <text class="form-label">物品类型</text>
        <view class="picker-cell" @click="showGoodsPicker = true">
          <text class="picker-value" :class="{ placeholder: !goodsTypeName }">{{ goodsTypeName || '选择物品类型' }}</text>
          <u-icon name="arrow-down" color="#86909C" size="18" />
        </view>
      </view>
      <view class="form-row">
        <text class="form-label">重量(kg)</text>
        <u-input v-model="form.goodsWeight" placeholder="请输入重量" border="none" type="digit" />
      </view>
    </view>

    <!-- 查询按钮 -->
    <view class="action-bar">
      <u-button type="primary" shape="circle" :loading="loading" @click="handleQuery">查询运费</u-button>
    </view>
    <view style="height: 40rpx"></view>

    <!-- 试算结果 -->
    <view v-if="queried" class="card price-card">
      <view v-if="errorMsg" class="price-error">{{ errorMsg }}</view>
      <template v-else>
        <view class="price-line">
          <text class="price-label">预估运费</text>
          <text class="price-value">¥{{ priceText }}</text>
        </view>
        <view class="price-tip">试算结果仅供参考，实际运费以下单计费为准</view>
      </template>
    </view>

    <!-- 物品类型选择 -->
    <u-picker :show="showGoodsPicker" :columns="goodsColumns" @confirm="onGoodsConfirm" @cancel="showGoodsPicker = false" />
  </view>
</template>

<script setup>
// CST-03 运费查询：独立于寄件流程的快速试算页，仅调 totalPrice 试算，不产生订单数据
import { ref, reactive, computed, onUnmounted } from 'vue';
import { totalPrice } from '../../../common/api/mailing.js';
import { goodsTypeAll } from '../../../common/api/index.js';
import { maskName, maskPhone } from '../../../common/utils/mask.js';

const sendAddr = ref(null);
const recvAddr = ref(null);
const goodsTypes = ref([]);
const goodsTypeName = ref('');
const loading = ref(false);
const queried = ref(false);
const showGoodsPicker = ref(false);

const form = reactive({
  sendAddress: '', // 地址簿 id（与寄件页口径一致，禁传文本地址）
  receiptAddress: '',
  goodsName: '',
  goodsType: '',
  goodsWeight: '',
});

const price = ref(null);
const errorMsg = ref('');
const priceText = computed(() => (price.value !== null && price.value !== undefined ? price.value : '--'));

const goodsColumns = computed(() => [goodsTypes.value.map((g) => g.name || g.id || '未知类型')]);

const pickAddress = (role) => {
  uni.navigateTo({ url: `/pages/customer/address/list?selectMode=${role}` });
};

const onGoodsConfirm = (e) => {
  const name = (e.value || [])[0];
  const found = goodsTypes.value.find((g) => (g.name || g.id) === name);
  if (found) {
    form.goodsType = found.id;
    goodsTypeName.value = found.name || found.id;
  }
  showGoodsPicker.value = false;
};

const handleQuery = async () => {
  if (!form.sendAddress) {
    uni.showToast({ title: '请选择发件地址', icon: 'none' });
    return;
  }
  if (!form.receiptAddress) {
    uni.showToast({ title: '请选择收件地址', icon: 'none' });
    return;
  }
  if (!form.goodsWeight || Number(form.goodsWeight) <= 0) {
    uni.showToast({ title: '请输入有效重量', icon: 'none' });
    return;
  }
  loading.value = true;
  try {
    // 与寄件页同参结构：pickupType/payMethod 传默认值（仅参与计费口径，不产生订单）
    const data = await totalPrice({
      ...form,
      goodsWeight: Number(form.goodsWeight),
      pickUpTime: '',
      pickupType: 2, // 2 上门取件
      payMethod: 1, // 1 预结
    });
    queried.value = true;
    // 后端口径：参数不全/地址解析失败时 amount="0" 且带 errorMsg；正常返回字符串金额
    if (data && data.amount !== undefined && data.amount !== '0') {
      price.value = data.amount;
      errorMsg.value = '';
    } else {
      price.value = null;
      errorMsg.value = (data && data.errorMsg) || '暂时无法试算，请检查地址与货物信息';
    }
  } catch (e) {
    queried.value = true;
    price.value = null;
    errorMsg.value = '试算失败，请稍后重试';
  } finally {
    loading.value = false;
  }
};

// 地址簿选择回传（uni.$emit('addressSelected')），页面销毁时解除监听防重复回调
const onAddressSelected = (payload) => {
  if (payload && payload.role === 'send') {
    sendAddr.value = payload.item;
    form.sendAddress = payload.item.id;
  } else if (payload && payload.role === 'receipt') {
    recvAddr.value = payload.item;
    form.receiptAddress = payload.item.id;
  }
};
uni.$on('addressSelected', onAddressSelected);
onUnmounted(() => {
  uni.$off('addressSelected', onAddressSelected);
});

// 物品类型全量加载（失败静默，查询时后端仍会校验）
goodsTypeAll()
  .then((data) => {
    goodsTypes.value = Array.isArray(data) ? data : (data && data.items) || [];
  })
  .catch(() => {});
</script>

<style lang="scss" scoped>
.card-title {
  font-size: var(--f-sub);
  font-weight: 600;
  color: var(--c-text-1);
  margin-bottom: var(--s-3);
}
.addr-row {
  display: flex;
  align-items: center;
  padding: var(--s-3) 0;
}
.addr-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  color: #fff;
  font-size: var(--f-body);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: var(--s-3);
}
.addr-icon.send {
  background: var(--c-primary);
}
.addr-icon.recv {
  background: var(--c-success);
}
.addr-body {
  flex: 1;
  min-width: 0;
}
.addr-name {
  font-size: var(--f-body);
  color: var(--c-text-1);
}
.addr-contact {
  margin-top: 4rpx;
  font-size: var(--f-tip);
  color: var(--c-text-3);
}
.form-row {
  display: flex;
  align-items: center;
  padding: var(--s-3) 0;
}
.form-label {
  width: 160rpx;
  font-size: var(--f-body);
  color: var(--c-text-2);
}
.picker-cell {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.picker-value {
  font-size: var(--f-body);
  color: var(--c-text-1);
}
.picker-value.placeholder {
  color: var(--c-text-4);
}
.price-card {
  padding: var(--s-5) var(--s-4);
}
.price-line {
  display: flex;
  align-items: baseline;
  justify-content: center;
}
.price-label {
  font-size: var(--f-body);
  color: var(--c-text-2);
  margin-right: var(--s-3);
}
.price-value {
  font-size: 56rpx;
  font-weight: 700;
  color: var(--c-primary);
}
.price-error {
  text-align: center;
  font-size: var(--f-body);
  color: var(--c-error);
}
.price-tip {
  margin-top: var(--s-2);
  text-align: center;
  font-size: var(--f-tip);
  color: var(--c-text-3);
}
</style>
