<template>
  <view class="page">
    <!-- 单条确认：从任务详情进入时 -->
    <view v-if="tranOrderId" class="card">
      <text class="title">入库确认</text>
      <text class="desc">确认后将运单标记为已入库，请核对运单信息无误。</text>
      <view class="order-no">运单号：{{ tranOrderId }}</view>
      <u-button
        type="primary"
        text="确认入库"
        shape="circle"
        :loading="loading"
        customStyle="margin-top: 24rpx;"
        @click="onConfirm"
      ></u-button>
    </view>

    <!-- 连续扫码批量入库（《70》§3.3）：无需退出本页，逐条成功提示 + 批次计数 -->
    <view class="card">
      <view class="scan-head">
        <text class="title">扫码批量入库</text>
        <text class="count">已入库 <text class="count-num">{{ doneCount }}</text> 件</text>
      </view>
      <text class="desc">连续扫码逐条入库，扫完自动记录，无需退出本页。</text>
      <u-button
        type="primary"
        :text="batch.length ? '继续扫码入库' : '扫码入库'"
        shape="circle"
        :loading="scanning"
        customStyle="margin-top: 24rpx;"
        @click="onScan"
      ></u-button>

      <view v-if="batch.length" class="batch-list">
        <view v-for="(it, i) in batch" :key="i" class="batch-row">
          <u-icon
            :name="it.ok ? 'checkmark-circle-fill' : 'close-circle-fill'"
            :color="it.ok ? 'var(--c-success)' : 'var(--c-error)'"
            size="16"
          ></u-icon>
          <text class="batch-no">{{ it.code }}</text>
          <text class="batch-msg" :class="{ err: !it.ok }">{{ it.msg }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
// 入库：单条（带 tranOrderId 从任务详情进入）+ 连续扫码批量（无 param 时扫码逐条入库）
// 后端仅单条 PUT /courier/warehousing/{id}，批量由前端循环调用实现，不新增后端契约。
import { ref } from 'vue'
import { warehousing } from '@/common/api/courier'
import { onLoad } from '@dcloudio/uni-app'

const tranOrderId = ref('')
const loading = ref(false)
const scanning = ref(false)
const doneCount = ref(0)
const batch = ref<{ code: string; ok: boolean; msg: string }[]>([])

onLoad((options: any) => {
  tranOrderId.value = options?.tranOrderId || ''
})

async function onConfirm() {
  if (!tranOrderId.value) {
    uni.showToast({ title: '缺少运单标识', icon: 'none' })
    return
  }
  if (loading.value) return
  loading.value = true
  try {
    await warehousing(tranOrderId.value)
    uni.showToast({ title: '入库成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (e) {
  } finally {
    loading.value = false
  }
}

// 扫码入库：系统扫码一次 → 立即提交，结果追加到批次列表，可继续扫（无需退出本页）
function onScan() {
  if (scanning.value) return
  scanning.value = true
  uni.scanCode({
    success: async (res) => {
      const code = (res.result || '').trim()
      if (code) await submitOne(code)
      scanning.value = false
    },
    fail: () => {
      scanning.value = false
    },
  })
}

async function submitOne(code: string) {
  if (batch.value.some((b) => b.code === code)) {
    pushBatch(code, false, '重复扫描')
    uni.showToast({ title: '该运单已扫过', icon: 'none' })
    return
  }
  try {
    await warehousing(code)
    doneCount.value++
    pushBatch(code, true, '入库成功')
    uni.showToast({ title: '入库成功', icon: 'success' })
  } catch (e) {
    pushBatch(code, false, '入库失败')
  }
}

function pushBatch(code: string, ok: boolean, msg: string) {
  batch.value.unshift({ code, ok, msg })
  if (batch.value.length > 20) batch.value.pop()
}
</script>

<style lang="scss">
.title {
  display: block;
  font-size: var(--f-sub);
  color: var(--c-text-1);
  font-weight: bold;
}
.desc {
  display: block;
  font-size: var(--f-aux);
  color: var(--c-text-2);
  margin-top: var(--s-2);
}
.order-no {
  display: block;
  font-size: var(--f-body);
  color: var(--c-text-1);
  margin-top: var(--s-3);
  font-weight: bold;
}
.scan-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.count {
  font-size: var(--f-aux);
  color: var(--c-text-3);
}
.count-num {
  font-size: var(--f-sub);
  color: var(--c-primary);
  font-weight: bold;
}
.batch-list {
  margin-top: var(--s-3);
  border-top: 2rpx solid var(--c-border);
  padding-top: var(--s-2);
}
.batch-row {
  display: flex;
  align-items: center;
  padding: var(--s-2) 0;
}
.batch-no {
  flex: 1;
  margin-left: 12rpx;
  font-size: var(--f-aux);
  color: var(--c-text-1);
  word-break: break-all;
}
.batch-msg {
  font-size: var(--f-tip);
  color: var(--c-success);
  flex-shrink: 0;
}
.batch-msg.err {
  color: var(--c-error);
}
</style>
