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
          activeColor="var(--c-primary)"
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

    <!-- 修改点：附件 >=1 张（与快递员端统一的手写照片格） -->
    <view class="card">
      <view class="card-title">现场照片（至少 1 张）</view>
      <view class="photo-list">
        <view class="photo-item" v-for="(p, idx) in photos" :key="idx">
          <image :src="p.url" class="photo-img" mode="aspectFill"></image>
          <u-icon name="close-circle" class="photo-del" @click="removePhoto(idx)"></u-icon>
        </view>
        <view class="photo-add" @click="onChoosePhoto">
          <u-icon name="camera" size="44" color="var(--c-text-3)"></u-icon>
          <text class="photo-add-text">添加</text>
        </view>
      </view>
    </view>

    <view class="action-bar">
      <u-button
        type="primary"
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
const photos = ref<any[]>([]) // {url, id}
const submitting = ref(false)
const form = reactive<{ exceptionType: string; remark: string }>({
  exceptionType: '',
  remark: '',
})

function onChoosePhoto() {
  uni.chooseImage({
    count: 6,
    sourceType: ['camera', 'album'],
    success: async (res) => {
      for (const filePath of res.tempFilePaths) {
        try {
          const att = await attachmentUpload(filePath, 'driver')
          const attId = att && (att.id || att.url) ? att.id || att.url : ''
          photos.value.push({ url: filePath, id: attId })
        } catch (e) {
          uni.showToast({ title: '上传失败', icon: 'none' })
        }
      }
    },
  })
}
function removePhoto(idx: number) {
  photos.value.splice(idx, 1)
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
  if (photos.value.length < 1) {
    uni.showToast({ title: '请至少上传 1 张照片', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    // 修改点：reporterId 由 token 取，前端不传；transportTaskId 可选
    await exceptionReport({
      exceptionType: form.exceptionType,
      attachmentIds: photos.value.map((p) => p.id).filter(Boolean),
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
.photo-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--s-3);
}
.photo-item {
  position: relative;
  width: 160rpx;
  height: 160rpx;
}
.photo-img {
  width: 160rpx;
  height: 160rpx;
  border-radius: var(--r-sm);
}
.photo-del {
  position: absolute;
  top: -10rpx;
  right: -10rpx;
  background: var(--c-surface);
  border-radius: var(--r-round);
}
.photo-add {
  width: 160rpx;
  height: 160rpx;
  border: 2rpx dashed var(--c-border);
  border-radius: var(--r-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.photo-add-text {
  font-size: var(--f-tip);
  color: var(--c-text-3);
  margin-top: var(--s-1);
}
</style>
