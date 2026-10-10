<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <el-form class="page-search" :model="query" inline>
        <el-form-item label="订单号" prop="id">
          <el-input v-model="query.id" placeholder="请输入订单号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="订单状态" prop="status">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option
              v-for="item in ORDER_STATUS_OPTIONS"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="订单类型" prop="orderType">
          <el-select v-model="query.orderType" placeholder="全部类型" clearable style="width: 140px">
            <el-option label="同城订单" :value="1" />
            <el-option label="城际订单" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="收件人" prop="receiverName">
          <el-input v-model="query.receiverName" placeholder="收件人姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="收件电话" prop="receiverPhone">
          <el-input v-model="query.receiverPhone" placeholder="收件人电话" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="付款方式" prop="paymentMethod">
          <el-select v-model="query.paymentMethod" placeholder="全部" clearable style="width: 130px">
            <el-option label="预结" :value="1" />
            <el-option label="到付" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="id" label="订单号" min-width="180" show-overflow-tooltip />
          <el-table-column prop="orderType" label="订单类型" width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.orderType === 1 ? 'primary' : 'warning'">
                {{ orderTypeLabel(scope.row.orderType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="收件人" min-width="160">
            <template #default="scope">
              <div>{{ scope.row.receiverName || "-" }}</div>
              <div class="page-text-sub">{{ scope.row.receiverPhone }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="receiverAddress" label="收件地址" min-width="200" show-overflow-tooltip />
          <el-table-column label="发件人" min-width="150">
            <template #default="scope">
              <div>{{ scope.row.senderName || "-" }}</div>
              <div class="page-text-sub">{{ scope.row.senderPhone }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="paymentMethod" label="付款方式" width="100" align="center">
            <template #default="scope">{{ paymentMethodLabel(scope.row.paymentMethod) }}</template>
          </el-table-column>
          <el-table-column prop="amount" label="金额(元)" width="100" align="right" />
          <el-table-column prop="status" label="订单状态" width="110" align="center">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="下单时间" width="170" />
          <el-table-column label="操作" fixed="right" width="90" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openDetail(scope.row as OrderRecord)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="page-pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- 订单详情 -->
    <el-drawer v-model="detailVisible" title="订单详情" size="620px">
      <div v-loading="detailLoading" class="order-detail">
        <template v-if="detail">
          <el-descriptions title="基本信息" :column="2" border>
            <el-descriptions-item label="订单号" :span="2">{{ detail.id }}</el-descriptions-item>
            <el-descriptions-item label="订单类型">{{ orderTypeLabel(detail.orderType) }}</el-descriptions-item>
            <el-descriptions-item label="取件类型">{{ pickupTypeLabel(detail.pickupType) }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">
              <el-tag :type="statusTagType(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="下单时间">{{ detail.createTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="付款方式">{{ paymentMethodLabel(detail.paymentMethod) }}</el-descriptions-item>
            <el-descriptions-item label="付款状态">{{ paymentStatusLabel(detail.paymentStatus) }}</el-descriptions-item>
            <el-descriptions-item label="金额(元)">{{ detail.amount ?? "-" }}</el-descriptions-item>
            <el-descriptions-item label="距离(公里)">{{ detail.distance ?? "-" }}</el-descriptions-item>
            <el-descriptions-item label="预计到达">{{ detail.estimatedArrivalTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="客户ID">{{ detail.memberId || "-" }}</el-descriptions-item>
          </el-descriptions>

          <el-descriptions title="收件信息" :column="2" border class="order-detail__block">
            <el-descriptions-item label="收件人">{{ detail.receiverName || "-" }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ detail.receiverPhone || "-" }}</el-descriptions-item>
            <el-descriptions-item label="所在地区" :span="2">{{ receiverArea }}</el-descriptions-item>
            <el-descriptions-item label="详细地址" :span="2">{{ detail.receiverAddress || "-" }}</el-descriptions-item>
          </el-descriptions>

          <el-descriptions title="发件信息" :column="2" border class="order-detail__block">
            <el-descriptions-item label="发件人">{{ detail.senderName || "-" }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ detail.senderPhone || "-" }}</el-descriptions-item>
            <el-descriptions-item label="所在地区" :span="2">{{ senderArea }}</el-descriptions-item>
            <el-descriptions-item label="详细地址" :span="2">{{ detail.senderAddress || "-" }}</el-descriptions-item>
          </el-descriptions>

          <div
            v-for="item in pickupDispatchBlocks"
            :key="item.title"
            class="order-detail__block"
          >
            <div class="order-detail__title">{{ item.title }}</div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="任务编号">{{ item.task.id }}</el-descriptions-item>
              <el-descriptions-item label="任务状态">{{ taskStatusLabel(item.task.status) }}</el-descriptions-item>
              <el-descriptions-item label="所属网点">{{ item.task.agency?.name || "-" }}</el-descriptions-item>
              <el-descriptions-item label="快递员">{{ item.task.courier?.name || "-" }}</el-descriptions-item>
              <el-descriptions-item label="快递员电话">{{ item.task.courier?.phone || "-" }}</el-descriptions-item>
              <el-descriptions-item label="分配状态">{{ assignedStatusLabel(item.task.assignedStatus) }}</el-descriptions-item>
              <el-descriptions-item label="预计开始">{{ item.task.estimatedStartTime || "-" }}</el-descriptions-item>
              <el-descriptions-item label="实际开始">{{ item.task.actualStartTime || "-" }}</el-descriptions-item>
              <el-descriptions-item label="预计完成">{{ item.task.estimatedEndTime || "-" }}</el-descriptions-item>
              <el-descriptions-item label="实际完成">{{ item.task.actualEndTime || "-" }}</el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ item.task.mark || "-" }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <el-descriptions
            v-if="detail.transportOrder"
            title="运单信息"
            :column="2"
            border
            class="order-detail__block"
          >
            <el-descriptions-item label="运单号" :span="2">{{ detail.transportOrder.id }}</el-descriptions-item>
            <el-descriptions-item label="运单状态">{{ transportStatusLabel(detail.transportOrder.status) }}</el-descriptions-item>
            <el-descriptions-item label="调度状态">{{ schedulingStatusLabel(detail.transportOrder.schedulingStatus) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间" :span="2">{{ detail.transportOrder.createTime || "-" }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import OrderAPI from "@/api/order";
import { OrderStatus, type OrderRecord, type OrderDetail, type OrderPageQuery, type PickupDispatchTask } from "@/api/order";

/** 订单状态选项（搜索下拉 + 标签） */
const ORDER_STATUS_OPTIONS: { value: OrderStatus; label: string; type: TagType }[] = [
  { value: OrderStatus.PENDING_PICKUP, label: "待取件", type: "info" },
  { value: OrderStatus.PICKED_UP, label: "已取件", type: "primary" },
  { value: OrderStatus.SELF_SHIPPED, label: "网点自寄", type: "primary" },
  { value: OrderStatus.INBOUND, label: "网点入库", type: "primary" },
  { value: OrderStatus.PENDING_LOADING, label: "待装车", type: "warning" },
  { value: OrderStatus.TRANSPORTING, label: "运输中", type: "warning" },
  { value: OrderStatus.OUTBOUND, label: "网点出库", type: "warning" },
  { value: OrderStatus.PENDING_DELIVERY, label: "待派送", type: "warning" },
  { value: OrderStatus.DELIVERING, label: "派送中", type: "warning" },
  { value: OrderStatus.SIGNED, label: "已签收", type: "success" },
  { value: OrderStatus.REJECTED, label: "拒收", type: "danger" },
  { value: OrderStatus.CANCELLED, label: "已取消", type: "info" },
];

type TagType = "primary" | "success" | "info" | "warning" | "danger";

const statusLabel = (status?: number) =>
  ORDER_STATUS_OPTIONS.find((item) => item.value === status)?.label ?? "未知";
const statusTagType = (status?: number): TagType =>
  ORDER_STATUS_OPTIONS.find((item) => item.value === status)?.type ?? "info";

const orderTypeLabel = (type?: number) => (type === 1 ? "同城" : type === 2 ? "城际" : "-");
const pickupTypeLabel = (type?: number) => (type === 1 ? "网点自寄" : type === 2 ? "上门取件" : "-");
const paymentMethodLabel = (type?: number) => (type === 1 ? "预结" : type === 2 ? "到付" : "-");
const paymentStatusLabel = (type?: number) => (type === 1 ? "未付" : type === 2 ? "已付" : "-");
const transportStatusLabel = (type?: number) =>
  ({ 1: "新建", 2: "已装车", 3: "到达", 4: "到达终端网点" } as Record<number, string>)[type ?? -1] ?? "-";
const schedulingStatusLabel = (type?: number) =>
  ({ 1: "待调度", 2: "未匹配线路", 3: "已调度" } as Record<number, string>)[type ?? -1] ?? "-";

/** 查询与列表状态 */
const loading = ref(false);
const list = ref<OrderRecord[]>([]);
const total = ref(0);
const query = ref<OrderPageQuery>({ page: 1, pageSize: 10 });

/** 组装请求体：剔除空字符串/空值，仅提交有效过滤条件 */
function buildPayload(): OrderPageQuery {
  const { page, pageSize, ...filters } = query.value;
  const payload: OrderPageQuery = { page, pageSize };
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== "" && value !== null && value !== undefined) {
      (payload as unknown as Record<string, unknown>)[key] = value;
    }
  });
  return payload;
}

async function loadList() {
  loading.value = true;
  try {
    const res = await OrderAPI.page(buildPayload());
    list.value = res?.items ?? [];
    total.value = res?.counts ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.value.page = 1;
  loadList();
}

function handleReset() {
  query.value = { page: 1, pageSize: 10 };
  loadList();
}

/** 详情 */
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<OrderDetail>();

async function openDetail(row: OrderRecord) {
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = undefined;
  try {
    detail.value = await OrderAPI.getDetail(row.id);
  } finally {
    detailLoading.value = false;
  }
}

const joinArea = (...areas: ({ name?: string } | undefined)[]) =>
  areas.map((area) => area?.name).filter(Boolean).join(" / ") || "-";
const receiverArea = computed(() =>
  joinArea(detail.value?.receiverProvince, detail.value?.receiverCity, detail.value?.receiverCounty)
);
const senderArea = computed(() =>
  joinArea(detail.value?.senderProvince, detail.value?.senderCity, detail.value?.senderCounty)
);

/** 取派件任务状态/分配状态文案 */
const taskStatusLabel = (status?: number) =>
  ({ 1: "待执行", 2: "进行中", 3: "待确认", 4: "已完成", 5: "已取消" } as Record<number, string>)[status ?? -1] ?? "-";
const assignedStatusLabel = (status?: number) =>
  ({ 1: "未分配", 2: "已分配", 3: "待人工分配" } as Record<number, string>)[status ?? -1] ?? "-";

/** 详情中的取件/派件任务块 */
const pickupDispatchBlocks = computed(() => {
  const blocks: { title: string; task: PickupDispatchTask }[] = [];
  if (detail.value?.taskPickup) {
    blocks.push({ title: "取件任务", task: detail.value.taskPickup });
  }
  if (detail.value?.taskDispatch) {
    blocks.push({ title: "派件任务", task: detail.value.taskDispatch });
  }
  return blocks;
});

onMounted(loadList);
</script>
