<template>
  <view class="page">
    <view class="card info-card">
      <text class="info-no">运单号：{{ orderNumber || tranOrderId }}</text>
    </view>

    <!-- 拍照回单 -->
    <view class="card">
      <text class="block-title">现场照片</text>
      <view class="photo-list">
        <view class="photo-item" v-for="(p, idx) in photos" :key="idx">
          <image :src="p.url" class="photo-img" mode="aspectFill"></image>
          <u-icon name="close-circle" class="photo-del" @click="removePhoto(idx)"></u-icon>
          <!-- GPS 水印（仅端上展示，不回传 location） -->
          <text class="photo-wm" v-if="gpsText">{{ gpsText }}</text>
        </view>
        <view class="photo-add" @click="onChoosePhoto">
          <u-icon name="camera" size="44" color="var(--c-text-3)"></u-icon>
          <text class="photo-add-text">拍照</text>
        </view>
      </view>
    </view>

    <!-- 手写签名 -->
    <view class="card">
      <text class="block-title">客户签名</text>
      <view class="sign-wrap">
        <canvas
          type="2d"
          id="signCanvas"
          class="sign-canvas"
          @touchstart="onTouchStart"
          @touchmove="onTouchMove"
          @touchend="onTouchEnd"
        ></canvas>
        <!-- 签名 GPS 水印 -->
        <text class="sign-wm" v-if="gpsText">{{ gpsText }}</text>
      </view>
      <view class="sign-actions">
        <u-button text="清除" shape="circle" @click="clearSign"></u-button>
        <u-button type="primary" text="保存签名" shape="circle" @click="saveSign"></u-button>
      </view>
    </view>

    <!-- 底部操作：签收 / 拒收 -->
    <view class="action-bar">
      <u-button type="error" text="拒收" shape="circle" @click="onDeliver(DELIVERED_REJECT)"></u-button>
      <u-button type="primary" text="确认签收" shape="circle" @click="onDeliver(DELIVERED_SIGN)"></u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
// 电子签收回单：拍照（attachmentUpload 拿 id）+ canvas 手写签名 + GPS 水印 + 签收/拒收
import { ref } from 'vue'
import {
  attachmentUpload,
  delivered,
  DELIVERED_SIGN,
  DELIVERED_REJECT,
} from '@/common/api/courier'
import { wgs84ToGcj02 } from '@/common/utils/coord'
import { onLoad, onUnload } from '@dcloudio/uni-app'

const tranOrderId = ref('')
const orderNumber = ref('')
const photos = ref<any[]>([]) // {url, id}
const gpsText = ref('')
const submitting = ref(false)

// ===== 画布签名相关 =====
let ctx: any = null
let canvasNode: any = null
let drawing = false
let hasSign = false
let lastPoint = { x: 0, y: 0 }

onLoad((options: any) => {
  tranOrderId.value = options?.tranOrderId || ''
  orderNumber.value = options?.orderNumber || ''
  // 取端上 GPS 用于照片/签名水印（不回传 location）
  getGps()
})

// 初始化 2d 画布
onReady(() => {
  const query = uni.createSelectorQuery()
  query
    .select('#signCanvas')
    .fields({ node: true, size: true })
    .exec((res: any) => {
      if (!res || !res[0]) return
      canvasNode = res[0].node
      ctx = canvasNode.getContext('2d')
      ctx.strokeStyle = '#1D2129'
      ctx.lineWidth = 4
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'
    })
})

onUnload(() => {
  ctx = null
  canvasNode = null
})

// 获取 GPS（仅端上水印）
function getGps() {
  uni.getLocation({
    type: 'wgs84',
    success: (res) => {
      const [lng, lat] = wgs84ToGcj02(res.longitude, res.latitude)
      gpsText.value = `经度 ${lng.toFixed(5)} 纬度 ${lat.toFixed(5)}`
    },
    fail: () => {
      gpsText.value = ''
    },
  })
}

// ===== 拍照上传 =====
function onChoosePhoto() {
  uni.chooseImage({
    count: 3,
    sourceType: ['camera', 'album'],
    success: async (res) => {
      for (const filePath of res.tempFilePaths) {
        try {
          const att = await attachmentUpload(filePath)
          photos.value.push({ url: filePath, id: att?.id })
        } catch (e) {
          // 单个失败不影响其余
        }
      }
    },
  })
}
function removePhoto(idx: number) {
  photos.value.splice(idx, 1)
}

// ===== 手写签名 =====
function getPos(e: any) {
  const t = e.touches[0] || e.changedTouches[0]
  return { x: t.x, y: t.y }
}
function onTouchStart(e: any) {
  if (!ctx) return
  drawing = true
  lastPoint = getPos(e)
}
function onTouchMove(e: any) {
  if (!ctx || !drawing) return
  const p = getPos(e)
  ctx.beginPath()
  ctx.moveTo(lastPoint.x, lastPoint.y)
  ctx.lineTo(p.x, p.y)
  ctx.stroke()
  lastPoint = p
  hasSign = true
}
function onTouchEnd() {
  drawing = false
}
function clearSign() {
  if (!ctx || !canvasNode) return
  ctx.clearRect(0, 0, canvasNode.width, canvasNode.height)
  hasSign = false
}
// 签名导出为临时图片并上传拿 id
async function saveSign() {
  if (!hasSign) {
    uni.showToast({ title: '请先手写签名', icon: 'none' })
    return
  }
  return new Promise<void>((resolve) => {
    uni.canvasToTempFilePath({
      canvas: canvasNode,
      success: async (r) => {
        try {
          const att = await attachmentUpload(r.tempFilePath)
          photos.value.push({ url: r.tempFilePath, id: att?.id, isSign: true })
          uni.showToast({ title: '签名已保存', icon: 'success' })
        } catch (e) {}
        resolve()
      },
      fail: () => resolve(),
    })
  })
}

// ===== 签收 / 拒收 =====
async function onDeliver(status: string) {
  if (!tranOrderId.value) {
    uni.showToast({ title: '缺少运单标识', icon: 'none' })
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await delivered(tranOrderId.value, status)
    uni.showToast({ title: status === DELIVERED_SIGN ? '签收成功' : '已拒收', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (e) {
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss">
.info-card {
  padding: var(--s-4);
}
.info-no {
  font-size: var(--f-body);
  color: var(--c-text-1);
  font-weight: bold;
}
.block-title {
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
  width: 180rpx;
  height: 180rpx;
}
.photo-img {
  width: 180rpx;
  height: 180rpx;
  border-radius: var(--r-sm);
}
.photo-del {
  position: absolute;
  top: -10rpx;
  right: -10rpx;
  background: var(--c-surface);
  border-radius: var(--r-round);
}
.photo-wm {
  position: absolute;
  left: 4rpx;
  bottom: 4rpx;
  font-size: 18rpx;
  color: #fff;
  background: rgba(0, 0, 0, 0.4);
  padding: 2rpx 6rpx;
  border-radius: var(--r-sm);
}
.photo-add {
  width: 180rpx;
  height: 180rpx;
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
.sign-wrap {
  position: relative;
  width: 100%;
  height: 360rpx;
  border: 2rpx dashed var(--c-border);
  border-radius: var(--r-sm);
  background: var(--c-surface);
}
.sign-canvas {
  width: 100%;
  height: 360rpx;
}
.sign-wm {
  position: absolute;
  left: 8rpx;
  bottom: 8rpx;
  font-size: 18rpx;
  color: var(--c-text-3);
}
.sign-actions {
  display: flex;
  gap: var(--s-3);
  margin-top: var(--s-3);
}
.action-bar {
  display: flex;
  gap: var(--s-3);
}
</style>
