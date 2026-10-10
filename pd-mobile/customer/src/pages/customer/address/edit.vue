<template>
  <view class="page">
    <u-navbar :title="id ? '编辑地址' : '新增地址'" autoBack></u-navbar>
    <u-form :model="form" ref="uForm">
      <view class="card">
        <u-form-item label="联系人" borderBottom>
          <u--input v-model="form.name" placeholder="收货人姓名" />
        </u-form-item>
        <u-form-item label="手机号" borderBottom>
          <u--input v-model="form.phone" type="number" placeholder="11位手机号" />
        </u-form-item>
        <u-form-item label="所在地区" borderBottom @click="showArea = true">
          <text v-if="areaText">{{ areaText }}</text>
          <text v-else class="placeholder">请选择省/市/区</text>
        </u-form-item>
        <u-form-item label="详细地址" borderBottom>
          <u--textarea v-model="form.address" placeholder="街道、门牌号" />
        </u-form-item>
        <u-form-item label="设为默认" borderBottom>
          <u-switch v-model="form.isDefault" />
        </u-form-item>
      </view>
    </u-form>
    <u-gap height="80"></u-gap>
    <view class="action-bar">
      <u-button type="primary" text="保存" shape="circle" :loading="saving" @click="onSave" />
    </view>
    <u-picker
      v-if="showArea"
      :show="showArea"
      :columns="areaColumns"
      @change="onAreaChange"
      @confirm="onAreaConfirm"
      @cancel="showArea = false"
    ></u-picker>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { areaSimple, createAddress, updateAddress, addressDetail } from '@/common/api/customer'
import { onLoad } from '@dcloudio/uni-app'

const id = ref('')
const form = reactive({
  name: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  address: '',
  isDefault: false,
})
const provinceId = ref('')
const cityId = ref('')
const districtId = ref('')
const provinceList = ref<any[]>([])
const cityList = ref<any[]>([])
const districtList = ref<any[]>([])
const areaColumns = ref<any[][]>([[], [], []])
const showArea = ref(false)
const areaText = ref('')
const saving = ref(false)

function normalize(res: any): any[] {
  return Array.isArray(res) ? res : res?.data || []
}

onLoad(async (opt: any) => {
  await loadProvinces()
  if (opt.id) {
    id.value = opt.id
    await loadDetail(opt.id)
  }
})

async function loadProvinces() {
  provinceList.value = normalize(await areaSimple(0))
  areaColumns.value = [provinceList.value.map((x: any) => x.name), [], []]
}

async function loadDetail(d: string) {
  const a = await addressDetail(d)
  form.name = a.name
  form.phone = a.mobile || a.phone
  form.address = a.address
  form.isDefault = !!a.isDefault
  form.province = a.province || ''
  form.city = a.city || ''
  form.district = a.district || ''
  provinceId.value = a.provinceId || ''
  cityId.value = a.cityId || ''
  districtId.value = a.districtId || ''
  areaText.value = `${form.province}${form.city}${form.district}`
}

async function onAreaChange(e: any) {
  const col = e.columnIndex ?? e.column
  const idx = e.index
  if (col === 0) {
    const p = provinceList.value[idx]
    if (!p) return
    provinceId.value = p.id
    cityList.value = normalize(await areaSimple(p.id))
    districtList.value = []
    areaColumns.value = [
      provinceList.value.map((x: any) => x.name),
      cityList.value.map((x: any) => x.name),
      [],
    ]
    cityId.value = ''
    districtId.value = ''
  } else if (col === 1) {
    const c = cityList.value[idx]
    if (!c) return
    cityId.value = c.id
    districtList.value = normalize(await areaSimple(c.id))
    areaColumns.value = [
      provinceList.value.map((x: any) => x.name),
      cityList.value.map((x: any) => x.name),
      districtList.value.map((x: any) => x.name),
    ]
    districtId.value = ''
  }
}

function onAreaConfirm(e: any) {
  const idxs = e.indexs || []
  const p = provinceList.value[idxs[0]]
  const c = cityList.value[idxs[1]]
  const d = districtList.value[idxs[2]]
  provinceId.value = p?.id || ''
  cityId.value = c?.id || ''
  districtId.value = d?.id || ''
  form.province = p?.name || ''
  form.city = c?.name || ''
  form.district = d?.name || ''
  areaText.value = `${form.province}${form.city}${form.district}`
  showArea.value = false
}

async function onSave() {
  if (!form.name || !form.phone || !form.address || !districtId.value) {
    uni.showToast({ title: '请完善地址信息', icon: 'none' })
    return
  }
  saving.value = true
  const payload = {
    name: form.name,
    phone: form.phone,
    mobile: form.phone,
    province: form.province,
    city: form.city,
    district: form.district,
    address: form.address,
    provinceId: provinceId.value,
    cityId: cityId.value,
    districtId: districtId.value,
    isDefault: form.isDefault,
  }
  try {
    if (id.value) await updateAddress(id.value, payload)
    else await createAddress(payload)
    uni.showToast({ title: '已保存', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (e) {
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss">
.placeholder {
  color: var(--c-text-4);
  font-size: var(--f-body);
}
</style>
