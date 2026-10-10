<template>
  <view class="page">
    <view class="card">
      <view class="card-title">提货信息</view>
      <view class="row">
        <text class="label">作业单号</text>
        <text class="value">{{ id || '—' }}</text>
      </view>
      <text class="tip">请上传提货照片作为提货凭证</text>
    </view>

    <!-- 修改点：拍照上传，禁 base64 直塞业务请求体 -->
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
        type="primary"
        text="确认提货"
        shape="circle"
        :loading="submitting"
        @click="onConfirm"
      ></u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { cargoPickUp, attachmentUpload } from '@/common/api/driver'
import { onLoad } from '@dcloudio/uni-app'

// 修改点：作业单 id（容错：taskTransportId 实为 DriverJobId）
const id = ref('')
const fileList = ref<any[]>([])
const picture = ref('')
const submitting = ref(false)

async function onAfterRead(event: any) {
  const file = event.file
  const filePath = Array.isArray(file) ? file[0].url : file.url
  try {
    const res = await attachmentUpload(filePath, 'driver')
    // 修改点：取附件 id 作为提货照片标识
    const attId = res && (res.id || res.url) ? res.id || res.url : ''
    picture.value = attId
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
  if (!picture.value) {
    uni.showToast({ title: '请先上传提货照片', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    // 修改点：不传 status，仅传 id + 提货照片
    await cargoPickUp({ id: id.value, cargoPickUpPicture: picture.value })
    uni.showToast({ title: '提货成功', icon: 'success' })
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
}
.upload-card {
  padding: var(--s-4);
}
</style>
