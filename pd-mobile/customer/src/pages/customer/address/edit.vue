<template>
  <view class="page">
    <u-navbar :title="id ? '编辑地址' : '新增地址'" :back="true" :borderBottom="false" bgColor="#E15536" placeholder />

    <view class="card">
      <u-form :model="form" :rules="rules" ref="formRef">
        <u-form-item label="姓名" prop="name" borderBottom>
          <u-input v-model="form.name" placeholder="收件人/寄件人姓名" border="none" />
        </u-form-item>
        <u-form-item label="电话" prop="phoneNumber" borderBottom>
          <u-input v-model="form.phoneNumber" placeholder="手机号或座机" border="none" type="number" :maxlength="11" />
        </u-form-item>
        <u-form-item label="所在地区" prop="area" borderBottom>
          <view class="area-picker" @click="showAreaPicker = true">
            <text class="area-text">{{ areaText || '选择省 / 市 / 区' }}</text>
            <u-icon name="arrow-down" color="#86909C" size="18" />
          </view>
        </u-form-item>
        <u-form-item label="详细地址" prop="address" borderBottom>
          <u-input v-model="form.address" placeholder="街道、门牌号等" border="none" />
        </u-form-item>
        <u-form-item label="公司" prop="companyName" borderBottom>
          <u-input v-model="form.companyName" placeholder="选填" border="none" />
        </u-form-item>
        <u-form-item label="设为默认" borderBottom>
          <u-switch v-model="form.isDefault" :activeValue="1" :inactiveValue="0" activeColor="#E15536" />
        </u-form-item>
      </u-form>
    </view>

    <view class="action-bar">
      <u-button type="primary" shape="circle" :loading="saving" class="save-btn" @click="handleSave">
        保存地址
      </u-button>
    </view>
    <view style="height: 140rpx"></view>

    <!-- 省市区级联 -->
    <u-picker :show="showAreaPicker" :columns="areaColumns" :loading="areaLoading" @confirm="onAreaConfirm" @cancel="showAreaPicker = false" />
  </view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { save, update, detail } from '../../../common/api/address.js';
import { areaSimple } from '../../../common/api/index.js';

const id = ref('');
const formRef = ref(null);
const saving = ref(false);
const showAreaPicker = ref(false);
const areaLoading = ref(false);

const form = reactive({
  name: '',
  phoneNumber: '',
  provinceId: '',
  cityId: '',
  countyId: '',
  address: '',
  companyName: '',
  isDefault: 0,
});

const rules = {
  name: [{ required: true, message: '请输入姓名', type: 'string' }],
  phoneNumber: [
    { required: true, message: '请输入电话', type: 'string' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', type: 'string' },
  ],
  address: [{ required: true, message: '请输入详细地址', type: 'string' }],
};

// 级联数据：省 / 市 / 区
const provinces = ref([]);
const cities = ref([]);
const counties = ref([]);
const areaText = ref('');

const areaColumns = computed(() => [provinces.value.map((p) => p.name), cities.value.map((c) => c.name), counties.value.map((c) => c.name)]);

const loadArea = async (parentId) => {
  const data = await areaSimple(parentId);
  return data || [];
};

const refreshCities = async (provinceId) => {
  cities.value = await loadArea(provinceId);
  form.cityId = '';
  form.countyId = '';
  counties.value = [];
  return cities.value;
};

const refreshCounties = async (cityId) => {
  counties.value = await loadArea(cityId);
  form.countyId = '';
  return counties.value;
};

const onAreaConfirm = async (e) => {
  const values = e.value || [];
  const idx = e.indexs || [];
  showAreaPicker.value = false;
  if (values.length < 3) return;
  const pName = values[0];
  const cName = values[1];
  const ctName = values[2];
  const p = provinces.value.find((x) => x.name === pName);
  if (p) {
    form.provinceId = p.id;
    areaText.value = pName;
    const cs = await refreshCities(p.id);
    const c = cs.find((x) => x.name === cName);
    if (c) {
      form.cityId = c.id;
      areaText.value = `${pName} ${cName}`;
      const cts = await refreshCounties(c.id);
      const ct = cts.find((x) => x.name === ctName);
      if (ct) {
        form.countyId = ct.id;
        areaText.value = `${pName} ${cName} ${ctName}`;
      }
    }
  }
};

const loadDetail = async (editId) => {
  try {
    const data = await detail(editId);
    form.name = data.name || '';
    form.phoneNumber = data.phoneNumber || '';
    form.provinceId = data.provinceId || '';
    form.cityId = data.cityId || '';
    form.countyId = data.countyId || '';
    form.address = data.address || '';
    form.companyName = data.companyName || '';
    form.isDefault = data.isDefault === 1 ? 1 : 0;
    // 回显地区名
    if (form.provinceId) {
      provinces.value = await loadArea(0);
      const p = provinces.value.find((x) => String(x.id) === String(form.provinceId));
      if (p) {
        areaText.value = p.name;
        cities.value = await refreshCities(p.id);
        const c = cities.value.find((x) => String(x.id) === String(form.cityId));
        if (c) {
          areaText.value = `${p.name} ${c.name}`;
          counties.value = await refreshCounties(c.id);
          const ct = counties.value.find((x) => String(x.id) === String(form.countyId));
          if (ct) {
            areaText.value = `${p.name} ${c.name} ${ct.name}`;
          }
        }
      }
    }
  } catch (e) {
    /* 错误已提示 */
  }
};

const handleSave = async () => {
  try {
    await formRef.value.validate();
  } catch (e) {
    return;
  }
  if (!form.provinceId || !form.cityId || !form.countyId) {
    uni.showToast({ title: '请选择完整省市区', icon: 'none' });
    return;
  }
  saving.value = true;
  try {
    if (id.value) {
      await update(id.value, { ...form });
    } else {
      await save({ ...form });
    }
    uni.showToast({ title: '保存成功', icon: 'success' });
    setTimeout(() => uni.navigateBack({ delta: 1 }), 500);
  } catch (e) {
    /* 错误已提示 */
  } finally {
    saving.value = false;
  }
};

onLoad(async (options) => {
  id.value = options.id || '';
  areaLoading.value = true;
  try {
    provinces.value = await loadArea(0);
    if (id.value) {
      await loadDetail(id.value);
    }
  } catch (e) {
    /* 错误已提示 */
  } finally {
    areaLoading.value = false;
  }
});
</script>

<style lang="scss" scoped>
.area-picker {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.area-text {
  font-size: var(--f-body);
  color: var(--c-text-1);
}
.save-btn { height: 88rpx; }
</style>
