<template>
  <view class="page">
    <view class="card">
      <view class="order-no">运单号：{{ orderNumber || tranOrderId }}</view>
    </view>

    <!-- 异常类型：三选一 -->
    <view class="card">
      <text class="label">异常类型</text>
      <u-radio-group v-model="exceptionType" placement="column">
        <u-radio
          v-for="t in exceptionTypes"
          :key="t.value"
          :label="t.text"
          :name="t.value"
          activeColor="var(--c-primary)"
        ></u-radio>
      </u-radio-group>
    </view>

    <!-- 现场照片：至少 1 张 -->
    <view class="card">
      <text class="label">现场照片（至少 1 张）</text>
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

    <!-- 备注：必填 -->
    <view class="card">
      <text class="label">异常说明</text>
      <u--textarea v-model="remark" placeholder="请描述异常情况（必填）" :maxlength="200"></u--textarea>
    </view>

    <view class="action-bar">
      <u-button
        type="primary"
        text="提交上报"
        shape="circle"
        :loading="loading"
        @click="onSubmit"
      ></u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
// 异常上报：接 tranOrderId/orderNumber/transportTaskId（容错）；
// 异常类型用 COURIER_EXCEPTION_TYPES 三选一；附件至少 1 张；remark 必填；reporterId 由 token 取，前端不传
import { ref } from 'vue'
import { attachmentUpload, exceptionReport, COURIER_EXCEPTION_TYPES } from '@/common/api/courier'

const tranOrderId = ref('')
const orderNumber = ref('')
const transportTaskId = ref('')
const exceptionTypes = COURIER_EXCEPTION_TYPES
const exceptionType = ref('')
const photos = ref<any[]>([]) // {url, id}
const remark = ref('')
const loading = ref(false)

onLoad((options: any) => {
  tranOrderId.value = options?.tranOrderId || ''
  orderNumber.value = options?.orderNumber || ''
  transportTaskId.value = options?.transportTaskId || ''
})

function onChoosePhoto() {
  uni.chooseImage({
    count: 6,
    sourceType: ['camera', 'album'],
    success: async (res) => {
      for (const filePath of res.tempFilePaths) {
        try {
          const att = await attachmentUpload(filePath)
          photos.value.push({ url: filePath, id: att?.id })
        } catch (e) {
          // 单张失败不影响其余
        }
      }
    },
  })
}
function removePhoto(idx: number) {
  photos.value.splice(idx, 1)
}

// 校验三要素：异常类型 + 附件(>=1) + 备注
async function onSubmit() {
  if (!exceptionType.value) {
    uni.showToast({ title: '请选择异常类型', icon: 'none' })
    return
  }
  if (photos.value.length < 1) {
    uni.showToast({ title: '请至少上传 1 张照片', icon: 'none' })
    return
  }
  if (!remark.value.trim()) {
    uni.showToast({ title: '请填写异常说明', icon: 'none' })
    return
  }
  if (loading.value) return
  loading.value = true
  try {
    await exceptionReport({
      exceptionType: exceptionType.value,
      attachmentIds: photos.value.map((p) => p.id).filter(Boolean),
      remark: remark.value.trim(),
      transportTaskId: transportTaskId.value || undefined,
      // 修改点：reporterId / courier 由后端依据 token 解析，前端不传
    })
    uni.showToast({ title: '上报成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (e) {
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss">
.order-no {
  font-size: var(--f-body);
  color: var(--c-text-1);
  font-weight: bold;
}
.label {
  display: block;
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
