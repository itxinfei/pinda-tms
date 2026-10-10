<template>
  <view class="page">
    <view class="card">
      <view class="card-title">异常类型</view>
      <!-- 修改点：司机侧仅 4 类异常 -->
      <u-radio-group v-model="form.exceptionType" placement="column">
        <u-radio
          v-for="t in types"
          :key="t.value"
          :label="t.text"
          :name="t.value"
          activeColor="#2B6CFF"
        ></u-radio>
      </u-radio-group>
    </view>

    <view class="card">
      <view class="card-title">异常说明（必填）</view>
      <u--textarea
        v-model="form.remark"
        placeholder="请描述异常详情"
        height="120"
      ></u--textarea>
    </view>

    <!-- 修改点：附件 >=1 张 -->
    <view class="card upload-card">
      <view class="card-title">现场照片（至少 1 张）</view>
      <u-upload
        :fileList="fileList"
        @afterRead="onAfterRead"
        @delete="onDelete"
        :maxCount="6"
        name="file"
        multiple
      ></u-upload>
    </view>

    <view class="action-bar">
      <u-button
        type="error"
        text="提交上报"
        shape="circle"
        :loading="submitting"
        @click="onSubmit"
      ></u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import {
  exceptionReport,
  attachmentUpload,
} from '@/common/api/driver'
import { DRIVER_EXCEPTION_TYPES } from '@/common/constants'
import { onLoad } from '@dcloudio/uni-app'

const types = DRIVER_EXCEPTION_TYPES
const id = ref('')
const transportTaskId = ref('')
const fileList = ref<any[]>([])
const attachmentIds = ref<string[]>([])
const submitting = ref(false)
const form = reactive<{ exceptionType: string; remark: string }>({
  exceptionType: '',
  remark: '',
})

async function onAfterRead(event: any) {
  const files = event.file
  const list = Array.isArray(files) ? files : [files]
  for (const f of list) {
    try {
      const res = await attachmentUpload(f.url, 'driver')
      const attId = res && (res.id || res.url) ? res.id || res.url : ''
      attachmentIds.value.push(attId)
      fileList.value.push({ ...f, status: 'success', message: '' })
    } catch (e) {
      uni.showToast({ title: '上传失败', icon: 'none' })
    }
  }
}
function onDelete(event: any) {
  const idx = event.index
  fileList.value.splice(idx, 1)
  attachmentIds.value.splice(idx, 1)
}

async function onSubmit() {
  if (!form.exceptionType) {
    uni.showToast({ title: '请选择异常类型', icon: 'none' })
    return
  }
  if (!form.remark) {
    uni.showToast({ title: '请填写异常说明', icon: 'none' })
    return
  }
  if (attachmentIds.value.length < 1) {
    uni.showToast({ title: '请至少上传 1 张照片', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    // 修改点：reporterId 由 token 取，前端不传；transportTaskId 可选
    await exceptionReport({
      exceptionType: form.exceptionType,
      attachmentIds: attachmentIds.value,
      remark: form.remark,
      transportTaskId: transportTaskId.value || undefined,
    })
    uni.showToast({ title: '上报成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

onLoad((opt: any) => {
  id.value = opt.id || ''
  transportTaskId.value = opt.transportTaskId || ''
})
</script>

<style lang="scss">
.card-title {
  font-size: var(--f-sub);
  color: var(--c-text-1);
  font-weight: bold;
  margin-bottom: var(--s-3);
}
.upload-card {
  padding: var(--s-4);
}
</style>
