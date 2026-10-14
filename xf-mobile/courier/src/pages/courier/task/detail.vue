<template>
  <view class="page" v-if="detail">
    <!-- 头部：运单号 + 状态 + 任务类型 -->
    <view class="card head-card">
      <view class="head-no">
        <text class="head-label">运单号</text>
        <text class="head-value">{{ detail.orderNumber || detail.id }}</text>
      </view>
      <view class="head-tags">
        <u-tag :text="statusView(detail.status).text" :type="statusView(detail.status).type" size="mini" />
        <u-tag :text="typeView(detail.taskType).text" type="primary" plain size="mini" />
      </view>
    </view>

    <!-- 寄件人 -->
    <view class="card addr-card">
      <text class="addr-title">寄件人</text>
      <view class="addr-row"><text class="addr-key">姓名</text><text class="addr-val">{{ maskName(sender?.name) }}</text></view>
      <view class="addr-row"><text class="addr-key">电话</text><text class="addr-val">{{ maskPhone(sender?.phoneNumber) }}</text></view>
      <view class="addr-row"><text class="addr-key">单位</text><text class="addr-val">{{ sender?.company || '—' }}</text></view>
      <view class="addr-row"><text class="addr-key">地址</text><text class="addr-val">{{ sender?.address || '—' }}</text></view>
    </view>

    <!-- 收件人 -->
    <view class="card addr-card">
      <text class="addr-title">收件人</text>
      <view class="addr-row"><text class="addr-key">姓名</text><text class="addr-val">{{ maskName(receiver?.name) }}</text></view>
      <view class="addr-row"><text class="addr-key">电话</text><text class="addr-val">{{ maskPhone(receiver?.phoneNumber) }}</text></view>
      <view class="addr-row"><text class="addr-key">单位</text><text class="addr-val">{{ receiver?.company || '—' }}</text></view>
      <view class="addr-row"><text class="addr-key">地址</text><text class="addr-val">{{ receiver?.address || '—' }}</text></view>
    </view>

    <!-- 货物信息（详情才有） -->
    <view class="card goods-card" v-if="hasGoods">
      <text class="addr-title">货物信息</text>
      <view class="addr-row"><text class="addr-key">货物类型</text><text class="addr-val">{{ detail.goodsTypeName || '—' }}</text></view>
      <view class="addr-row"><text class="addr-key">重量</text><text class="addr-val">{{ detail.weight != null ? detail.weight + ' kg' : '—' }}</text></view>
      <view class="addr-row"><text class="addr-key">数量</text><text class="addr-val">{{ detail.quantity != null ? detail.quantity : '—' }}</text></view>
      <view class="addr-row"><text class="addr-key">支付方式</text><text class="addr-val">{{ detail.paymentMethod || '—' }}</text></view>
      <view class="addr-row"><text class="addr-key">金额</text><text class="addr-val amount">{{ detail.amount != null ? '¥' + detail.amount : '—' }}</text></view>
    </view>

    <!-- 路由时间轴 -->
    <view class="card route-card" v-if="routeList.length">
      <text class="addr-title">路由轨迹</text>
      <!-- 修改点：uview-plus 无 u-time-line 组件，改用普通 view + CSS 时间轴 -->
      <view class="tl">
        <view class="tl-row" v-for="(node, idx) in routeList" :key="idx">
          <view class="tl-dot"></view>
          <view class="route-node">
            <text class="route-agency">{{ node.agencyName || '—' }}</text>
            <text class="route-time">{{ node.arrivalTime || '' }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>

  <!-- 操作按钮（按状态显示：未完成时可操作） -->
  <view class="action-bar" v-if="canOperate">
    <u-button type="primary" text="去签收" shape="circle" @click="goSign"></u-button>
    <u-button type="success" text="入库" shape="circle" @click="goWarehousing"></u-button>
    <u-button type="warning" text="交接" shape="circle" @click="goHandover"></u-button>
    <u-button type="info" text="核验" shape="circle" @click="goVerify"></u-button>
  </view>
</template>

<script setup lang="ts">
// 任务详情：接 id，调 taskDetail + taskRoute，展示收寄件人 / 货物 / 路由，按状态显示操作按钮
import { ref, computed } from 'vue'
import { taskDetail, taskRoute } from '@/common/api/courier'
import { taskStatusView, taskTypeView } from '@/common/constants'
import { maskName, maskPhone } from '@/common/utils/desensitive'
import { onLoad } from '@dcloudio/uni-app'

const id = ref('')
const detail = ref<any>(null)
const routeList = ref<any[]>([])

const sender = computed(() => detail.value?.sender || null)
const receiver = computed(() => detail.value?.receiver || null)
const hasGoods = computed(
  () =>
    detail.value &&
    (detail.value.goodsTypeName != null ||
      detail.value.weight != null ||
      detail.value.amount != null)
)
// 未完成（非已完成 4 / 已取消 5）时显示操作按钮
const canOperate = computed(
  () => detail.value && detail.value.status !== 4 && detail.value.status !== 5
)

function statusView(s?: number) {
  return taskStatusView(s)
}
function typeView(t?: number) {
  return taskTypeView(t)
}

// route 返回可能是数组，或 {items:[...]} / {routeList:[...]}，统一兜底
function normalizeRoute(r: any): any[] {
  if (!r) return []
  if (Array.isArray(r)) return r
  if (Array.isArray(r.items)) return r.items
  if (Array.isArray(r.routeList)) return r.routeList
  return []
}

async function loadDetail() {
  try {
    const [d, r] = await Promise.all([taskDetail(id.value), taskRoute(id.value)])
    detail.value = d || null
    routeList.value = normalizeRoute(r)
  } catch (e) {
    detail.value = null
  }
}

function goSign() {
  uni.navigateTo({
    url:
      '/pages/courier/pod/sign?tranOrderId=' +
      (detail.value?.tranOrderId || id.value) +
      '&orderNumber=' +
      (detail.value?.orderNumber || ''),
  })
}
function goWarehousing() {
  uni.navigateTo({
    url: '/pages/courier/task/warehousing?tranOrderId=' + (detail.value?.tranOrderId || id.value),
  })
}
function goHandover() {
  uni.navigateTo({
    url: '/pages/courier/task/handover?tranOrderId=' + (detail.value?.tranOrderId || id.value),
  })
}
function goVerify() {
  uni.navigateTo({
    url: '/pages/courier/task/verify?orderNumber=' + (detail.value?.orderNumber || ''),
  })
}

// onLoad 直接接收页面参数（uni-app 自动注入，不能从 'vue' 导入）
onLoad((options: any) => {
  id.value = options?.id || ''
  loadDetail()
})
</script>

<style lang="scss">
.head-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.head-no {
  display: flex;
  flex-direction: column;
}
.head-label {
  font-size: var(--f-tip);
  color: var(--c-text-3);
}
.head-value {
  font-size: var(--f-sub);
  color: var(--c-text-1);
  font-weight: bold;
  margin-top: var(--s-1);
}
.head-tags {
  display: flex;
  gap: var(--s-2);
}
.addr-title {
  display: block;
  font-size: var(--f-sub);
  color: var(--c-text-1);
  font-weight: bold;
  margin-bottom: var(--s-2);
}
.addr-row {
  display: flex;
  font-size: var(--f-aux);
  margin-top: var(--s-1);
}
.addr-key {
  width: 140rpx;
  color: var(--c-text-3);
}
.addr-val {
  flex: 1;
  color: var(--c-text-2);
}
.addr-val.amount {
  color: var(--c-error);
  font-weight: bold;
}
.route-node {
  display: flex;
  justify-content: space-between;
  flex: 1;
  padding-bottom: var(--s-3);
  border-bottom: 1rpx solid var(--c-border);
}
.tl-row {
  display: flex;
  align-items: flex-start;
}
.tl-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: var(--c-primary);
  margin: 8rpx var(--s-3) 0 0;
  flex-shrink: 0;
}
.route-agency {
  font-size: var(--f-aux);
  color: var(--c-text-1);
}
.route-time {
  font-size: var(--f-tip);
  color: var(--c-text-3);
}
.action-bar {
  display: flex;
  gap: var(--s-2);
  flex-wrap: wrap;
}
</style>
