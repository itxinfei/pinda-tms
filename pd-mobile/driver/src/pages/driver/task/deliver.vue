<template>
  <view class="page">
    <view class="card">
      <view class="card-title">送达信息</view>
      <view class="row">
        <text class="label">作业单号</text>
        <text class="value">{{ id || '—' }}</text>
      </view>
      <view class="row">
        <text class="label">送达经纬度</text>
        <text class="value">
          {{ lat != null && lng != null ? lat + ', ' + lng : '未获取' }}
        </text>
      </view>
      <text class="tip">经纬度由定位自动获取，禁止手填</text>
      <u-button
        type="primary"
        size="mini"
        :plain="locReady"
        :disabled="locReady"
        :text="locReady ? '定位已获取' : '获取定位'"
        @click="getLocation"
      ></u-button>
    </view>

    <!-- 签收照片 -->
    <view class="card upload-card">
      <u-upload
        :fileList="fileList"
        @afterRead="onAfterRead"
        @delete="onDelete"
        :maxCount="1"
        name="file"
        multiple
      ></u-upload>
    </view>

    <view class="action-bar">
      <u-button
        type="success"
        text="确认送达"
        shape="circle"
        :loading="submitting"
        :disabled="!locReady || !picture"
        @click="onConfirm"
      ></u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { cargoFinish, attachmentUpload } from '@/common/api/driver'

const id = ref('')
const fileList = ref<any[]>([])
const picture = ref('')
const lng = ref<number | null>(null)
const lat = ref<number | null>(null)
const locReady = ref(false)
const submitting = ref(false)

// 修改点：经纬度必须来自定位，禁止手填
function getLocation() {
  uni.getLocation({
    type: 'wgs84',
    success: (res) => {
      lng.value = res.longitude
      lat.value = res.latitude
      locReady.value = true
    },
    fail: () => {
      uni.showToast({ title: '定位失败，请重试', icon: 'none' })
      locReady.value = false
    },
  })
}

async function onAfterRead(event: any) {
  const file = event.file
  const filePath = Array.isArray(file) ? file[0].url : file.url
  try {
    const res = await attachmentUpload(filePath, 'driver')
    picture.value = res && (res.id || res.url) ? res.id || res.url : ''
    if (Array.isArray(file)) {
      fileList.value.push(...file.map((f: any) => ({ ...f, status: 'success', message: '' })))
    } else {
      fileList.value.push({ ...file, status: 'success', message: '' })
    }
    uni.showToast({ title: '上传成功', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: '上传失败', icon: 'none' })
  }
}
function onDelete() {
  fileList.value = []
  picture.value = ''
}

async function onConfirm() {
  if (!id.value) {
    uni.showToast({ title: '缺少作业单 id', icon: 'none' })
    return
  }
  if (!locReady.value || lng.value == null || lat.value == null) {
    uni.showToast({ title: '请先获取定位', icon: 'none' })
    return
  }
  if (!picture.value) {
    uni.showToast({ title: '请先上传签收照片', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    // 修改点：不传 status，仅传 id + 经纬度 + 签收照片
    await cargoFinish({
      id: id.value,
      deliverLatitude: lat.value,
      deliverLongitude: lng.value,
      deliverPicture: picture.value,
    })
    uni.showToast({ title: '送达成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

onLoad((opt: any) => {
  id.value = opt.taskTransportId || opt.id || ''
})
</script>

<style lang="scss">
.card-title {
  font-size: var(--f-sub);
  color: var(--c-text-1);
  font-weight: bold;
  margin-bottom: var(--s-3);
}
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--s-2) 0;
}
.label {
  font-size: var(--f-body);
  color: var(--c-text-3);
}
.value {
  font-size: var(--f-body);
  color: var(--c-text-1);
}
.tip {
  font-size: var(--f-aux);
  color: var(--c-text-3);
  margin-top: var(--s-2);
  display: block;
}
.upload-card {
  padding: var(--s-4);
}
</style>
