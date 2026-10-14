<template>
  <view class="page">
    <u-empty v-if="!loading && !hasData" mode="list" text="当前无在途任务"></u-empty>

    <!-- 修改点：在途为单对象 CargoTranTaskDTO，存在则展示一个卡片 -->
    <view
      v-if="isObject && single"
      class="card order-card"
      @click="goDetail(single.id)"
    >
      <view class="order-head">
        <text class="order-no">{{ single.taskNo || single.id }}</text>
        <u-tag
          v-if="single.status != null"
          :text="taskStatusView(single.status).text"
          :type="taskStatusView(single.status).type"
          size="mini"
        />
      </view>
      <view class="order-body">
        <text class="order-line">运单号：{{ single.tranOrderNum || '—' }}</text>
        <text class="order-line">
          起：{{ (single.startAgency && single.startAgency.name) || '—' }} → 终：{{
            (single.endAgency && single.endAgency.name) || '—'
          }}
        </text>
      </view>
      <view class="order-foot">
        <text class="order-tip">当前在途任务，点击查看详情</text>
      </view>
    </view>

    <!-- 修改点：容错，若后端返回列表则按列表渲染 -->
    <template v-if="isArray">
      <view
        v-for="item in list"
        :key="item.id"
        class="card order-card"
        @click="goDetail(item.id)"
      >
        <view class="order-head">
          <text class="order-no">{{ item.taskNo || item.id }}</text>
          <u-tag
            v-if="item.status != null"
            :text="taskStatusView(item.status).text"
            :type="taskStatusView(item.status).type"
            size="mini"
          />
        </view>
        <view class="order-body">
          <text class="order-line">运单号：{{ item.tranOrderNum || '—' }}</text>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { cargoOnTheWay } from '@/common/api/driver'
import { transportTaskStatusView } from '@/common/constants'
import { onShow } from '@dcloudio/uni-app'

const loading = ref(false)
const raw = ref<any>(null)
const list = ref<any[]>([])

const isObject = computed(() => raw.value && !Array.isArray(raw.value))
const isArray = computed(() => Array.isArray(raw.value))
const single = computed(() => (isObject.value ? raw.value : null))
const hasData = computed(() => isObject.value || (isArray.value && list.value.length > 0))

function taskStatusView(s?: number) {
  return transportTaskStatusView(s)
}

async function loadData() {
  loading.value = true
  try {
    const res = await cargoOnTheWay()
    raw.value = res
    list.value = Array.isArray(res) ? res : []
  } catch (e) {
    raw.value = null
    list.value = []
  } finally {
    loading.value = false
  }
}

function goDetail(id: string) {
  uni.navigateTo({ url: '/pages/driver/task/detail?id=' + id })
}

onShow(() => loadData())
</script>

<style lang="scss">
.order-card {
  padding: var(--s-4);
}
.order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.order-no {
  font-size: var(--f-body);
  color: var(--c-text-1);
  font-weight: bold;
}
.order-body {
  margin-top: var(--s-2);
}
.order-line {
  display: block;
  font-size: var(--f-aux);
  color: var(--c-text-2);
  margin-top: var(--s-1);
}
.order-foot {
  margin-top: var(--s-3);
}
.order-tip {
  font-size: var(--f-tip);
  color: var(--c-primary);
}
</style>
