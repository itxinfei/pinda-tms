<template>
  <view class="page">
    <u-navbar :title="'我要寄件'" :back="true" :borderBottom="false" bgColor="#2B6CFF" placeholder />

    <!-- 地址选择 -->
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

    <!-- 取件信息 -->
    <view class="card">
      <view class="card-title">取件信息</view>
      <view class="form-row">
        <text class="form-label">取件方式</text>
        <u-radio-group v-model="form.pickupType" :activeColor="'#2B6CFF'" shape="circle">
          <u-radio :name="1" label="网点自寄" />
          <u-radio :name="2" label="上门取件" />
        </u-radio-group>
      </view>
      <view class="form-row">
        <text class="form-label">取件时间</text>
        <view class="time-picker" @click="showTimePicker = true">
          <text class="time-value">{{ form.pickUpTime || '选择时间段（如 09:00-18:00）' }}</text>
          <u-icon name="clock" color="#86909C" size="18" />
        </view>
      </view>
    </view>

    <!-- 货物信息 -->
    <view class="card">
      <view class="card-title">货物信息</view>
      <view class="form-row">
        <text class="form-label">物品类型</text>
        <view class="time-picker" @click="showGoodsPicker = true">
          <text class="time-value">{{ goodsTypeName || '选择物品类型' }}</text>
          <u-icon name="arrow-down" color="#86909C" size="18" />
        </view>
      </view>
      <view class="form-row">
        <text class="form-label">物品名称</text>
        <u-input v-model="form.goodsName" placeholder="如：文件、衣物" border="none" />
      </view>
      <view class="form-row">
        <text class="form-label">重量(kg)</text>
        <u-input v-model="form.goodsWeight" placeholder="请输入重量" border="none" type="digit" @change="onFormChange" />
      </view>
    </view>

    <!-- 费用 -->
    <view class="card">
      <view class="card-title">费用</view>
      <view class="form-row">
        <text class="form-label">付款方式</text>
        <u-radio-group v-model="form.payMethod" :activeColor="'#2B6CFF'" shape="circle">
          <u-radio :name="1" label="预结" />
          <u-radio :name="2" label="到付" />
        </u-radio-group>
      </view>
      <view class="price-row">
        <text class="price-label">预估运费</text>
        <text class="price-value">¥{{ priceText }}</text>
      </view>
    </view>

    <!-- 提交 -->
    <view class="action-bar">
      <u-button type="primary" shape="circle" :loading="submitting" class="submit-btn" @click="handleSubmit">
        提交订单
      </u-button>
    </view>
    <view style="height: 140rpx"></view>

    <!-- 取件时间段选择 -->
    <u-picker :show="showTimePicker" :columns="timeColumns" @confirm="onTimeConfirm" @cancel="showTimePicker = false" />
    <!-- 物品类型选择 -->
    <u-picker :show="showGoodsPicker" :columns="goodsColumns" @confirm="onGoodsConfirm" @cancel="showGoodsPicker = false" />
  </view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { totalPrice, createOrder } from '../../../common/api/mailing.js';
import { goodsTypeAll } from '../../../common/api/index.js';
import { maskName, maskPhone } from '../../../common/utils/mask.js';

const sendAddr = ref(null);
const recvAddr = ref(null);
const goodsTypes = ref([]);
const goodsTypeName = ref('');
const price = ref(null);
const submitting = ref(false);
const showTimePicker = ref(false);
const showGoodsPicker = ref(false);

const form = reactive({
  sendAddress: '',
  receiptAddress: '',
  pickUpTime: '',
  pickupType: 2, // 1网点自寄 2上门取件
  payMethod: 1, // 1预结 2到付
  goodsType: '',
  goodsName: '',
  goodsWeight: '',
});

const priceText = computed(() => (price.value !== null && price.value !== undefined ? price.value : '--'));

const timeColumns = computed(() => {
  const hours = [];
  for (let h = 8; h <= 20; h += 1) {
    hours.push(h < 10 ? `0${h}:00` : `${h}:00`);
  }
  const start = hours.slice(0, hours.length - 1);
  const end = hours.slice(1);
  return [start, end];
});

const goodsColumns = computed(() => [goodsTypes.value.map((g) => g.name || g.id || '未知类型')]);

const pickAddress = (role) => {
  uni.navigateTo({ url: `/pages/customer/address/list?selectMode=${role}` });
};

const onTimeConfirm = (e) => {
  const values = e.value || [];
  if (values.length === 2 && values[0] && values[1]) {
    form.pickUpTime = `${values[0]}-${values[1]}`;
  }
  showTimePicker.value = false;
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

let timer = null;
const onFormChange = () => {
  clearTimeout(timer);
  timer = setTimeout(() => {
    calcPrice();
  }, 300);
};

const calcPrice = async () => {
  if (!form.sendAddress || !form.receiptAddress || !form.goodsWeight) {
    price.value = null;
    return;
  }
  try {
    const data = await totalPrice({ ...form });
    price.value = data && data.amount !== undefined ? data.amount : null;
  } catch (e) {
    price.value = null;
  }
};

const handleSubmit = async () => {
  if (!form.sendAddress) {
    uni.showToast({ title: '请选择发件地址', icon: 'none' });
    return;
  }
  if (!form.receiptAddress) {
    uni.showToast({ title: '请选择收件地址', icon: 'none' });
    return;
  }
  if (!form.pickUpTime) {
    uni.showToast({ title: '请选择取件时间', icon: 'none' });
    return;
  }
  if (!form.goodsType) {
    uni.showToast({ title: '请选择物品类型', icon: 'none' });
    return;
  }
  if (!form.goodsName.trim()) {
    uni.showToast({ title: '请填写物品名称', icon: 'none' });
    return;
  }
  if (!form.goodsWeight) {
    uni.showToast({ title: '请填写物品重量', icon: 'none' });
    return;
  }
  submitting.value = true;
  try {
    const data = await createOrder({ ...form });
    const orderId = data && data.amount !== undefined ? null : null;
    // 下单成功：跳详情（新订单在列表第一条，可直接回首页列表查看）
    uni.showToast({ title: '下单成功', icon: 'success' });
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/customer/mailing/list' });
    }, 600);
  } catch (e) {
    /* 错误已提示 */
  } finally {
    submitting.value = false;
  }
};

const loadGoodsTypes = async () => {
  try {
    const data = await goodsTypeAll();
    goodsTypes.value = data || [];
  } catch (e) {
    goodsTypes.value = [];
  }
};

onShow(() => {
  // 地址簿选择回传
  const pages = getCurrentPages();
  const cur = pages[pages.length - 1];
  if (cur && cur.$page && cur.$page.options) {
    // 由地址簿返回时通过 uni.$emit 传递，见 address/list.vue
  }
  loadGoodsTypes();
});

uni.$on('addressSelected', (payload) => {
  if (payload && payload.role === 'send') {
    sendAddr.value = payload.item;
    form.sendAddress = payload.item.id;
  } else if (payload && payload.role === 'receipt') {
    recvAddr.value = payload.item;
    form.receiptAddress = payload.item.id;
  }
  calcPrice();
});
</script>

<style lang="scss" scoped>
.card-title {
  font-size: var(--f-sub);
  font-weight: 600;
  color: var(--c-text-1);
  margin-bottom: 16rpx;
}
.addr-row {
  display: flex;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid var(--c-border);
}
.addr-row:last-child { border-bottom: none; }
.addr-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: var(--r-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 600;
  color: #fff;
  margin-right: 16rpx;
  flex-shrink: 0;
}
.addr-icon.send { background: var(--c-success); }
.addr-icon.recv { background: var(--c-primary); }
.addr-body { flex: 1; }
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
  justify-content: space-between;
  padding: 16rpx 0;
  border-bottom: 1rpx solid var(--c-border);
}
.form-row:last-child { border-bottom: none; }
.form-label {
  font-size: var(--f-body);
  color: var(--c-text-2);
  width: 160rpx;
}
.time-picker {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--c-text-1);
}
.time-value {
  font-size: var(--f-body);
}
.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 0 0;
}
.price-label {
  font-size: var(--f-body);
  color: var(--c-text-2);
}
.price-value {
  font-size: 44rpx;
  font-weight: 700;
  color: var(--c-error);
}
.submit-btn {
  height: 88rpx;
}
</style>
