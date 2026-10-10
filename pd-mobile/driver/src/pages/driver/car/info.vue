<template>
  <view class="page">
    <u-empty v-if="!loading && !car" mode="list" text="未获取到车辆信息"></u-empty>

    <view v-if="car" class="card">
      <view class="card-title">车辆信息</view>
      <view class="row">
        <text class="label">车牌号</text>
        <!-- 修改点：车牌脱敏展示 -->
        <text class="value">{{ maskPlate(car.licensePlate) }}</text>
      </view>
      <view class="row">
        <text class="label">车型</text>
        <text class="value">{{ car.model || '—' }}</text>
      </view>
      <view class="row">
        <text class="label">载重</text>
        <text class="value">{{ car.load != null ? car.load + ' kg' : '—' }}</text>
      </view>
      <view class="row">
        <text class="label">监控状态</text>
        <!-- 修改点：在线/离线/故障，离线故障明显告警 -->
        <u-tag
          v-if="monitorView.type !== 'offline'"
          :text="monitorView.text"
          :type="monitorView.type"
          size="mini"
        />
        <u-tag v-else :text="monitorView.text" type="error" size="mini" />
      </view>
    </view>

    <!-- 修改点：离线或故障时明显告警并提示禁任务 -->
    <u-alert
      v-if="car && (monitorView.type === 'offline' || monitorView.type === 'fault')"
      type="error"
      :description="monitorView.type === 'fault' ? '车辆故障，禁止执行运输任务' : '监控离线，禁止执行运输任务'"
      showIcon
    ></u-alert>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { carInfo } from '@/common/api/driver'

const id = ref('')
const car = ref<any>(null)
const loading = ref(false)

// 车牌脱敏：粤B****9
function maskPlate(plate?: string): string {
  if (!plate) return ''
  const s = String(plate)
  if (s.length <= 4) return s
  return s.slice(0, 2) + '****' + s.slice(-1)
}

// 监控状态：在线 success / 离线 offline / 故障 fault
const monitorView = computed(() => {
  const v = car.value?.monitorStatus ?? car.value?.status
  // 容错：后端可能返回字符串或数字
  if (v === 'ONLINE' || v === 1 || v === 'online') return { text: '在线', type: 'success' }
  if (v === 'FAULT' || v === 3 || v === 'fault') return { text: '故障', type: 'fault' }
  return { text: '离线', type: 'offline' }
})

async function loadInfo() {
  if (!id.value) return
  loading.value = true
  try {
    car.value = await carInfo(id.value)
  } catch (e) {
    car.value = null
  } finally {
    loading.value = false
  }
}

onLoad((opt: any) => {
  // 修改点：id 为车辆 id；若未传则从当前在途任务取（此处仅消费传入值）
  id.value = opt.id || opt.carId || ''
  loadInfo()
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
</style>
