<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <el-form class="page-search" :model="query" inline>
        <el-form-item label="运单号" prop="id">
          <el-input v-model="query.id" placeholder="请输入运单号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="运单状态" prop="status">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 160px">
            <el-option
              v-for="item in ORDER_STATUS_OPTIONS"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="寄件人" prop="senderName">
          <el-input v-model="query.senderName" placeholder="寄件人姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="寄件电话" prop="senderPhone">
          <el-input v-model="query.senderPhone" placeholder="寄件人电话" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="id" label="运单号" min-width="170" show-overflow-tooltip />
          <el-table-column label="寄件人" min-width="160">
            <template #default="scope">
              <div>{{ scope.row.order?.senderName || "-" }}</div>
              <div class="page-text-sub">{{ scope.row.order?.senderPhone }}</div>
            </template>
          </el-table-column>
          <el-table-column label="收件人" min-width="160">
            <template #default="scope">
              <div>{{ scope.row.order?.receiverName || "-" }}</div>
              <div class="page-text-sub">{{ scope.row.order?.receiverPhone }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="运单状态" width="120" align="center">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="schedulingStatus" label="调度状态" width="110" align="center">
            <template #default="scope">
              <el-tag :type="schedulingTagType(scope.row.schedulingStatus)" effect="plain">
                {{ schedulingStatusLabel(scope.row.schedulingStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" fixed="right" width="90" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openDetail(scope.row as TransportOrderRecord)">详情</el-button>
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

    <!-- 运单详情 -->
    <el-drawer v-model="detailVisible" title="运单详情" size="660px">
      <div v-loading="detailLoading" class="transport-detail">
        <template v-if="detail">
          <el-descriptions title="基本信息" :column="2" border>
            <el-descriptions-item label="运单号" :span="2">{{ detail.id }}</el-descriptions-item>
            <el-descriptions-item label="运单状态">
              <el-tag :type="statusTagType(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="调度状态">
              <el-tag :type="schedulingTagType(detail.schedulingStatus)" effect="plain">
                {{ schedulingStatusLabel(detail.schedulingStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间" :span="2">{{ detail.createTime || "-" }}</el-descriptions-item>
          </el-descriptions>

          <el-descriptions title="寄件信息" :column="2" border class="transport-detail__block">
            <el-descriptions-item label="寄件人">{{ detail.order?.senderName || "-" }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ detail.order?.senderPhone || "-" }}</el-descriptions-item>
            <el-descriptions-item label="所在地区" :span="2">{{ senderArea }}</el-descriptions-item>
            <el-descriptions-item label="详细地址" :span="2">{{ detail.order?.senderAddress || "-" }}</el-descriptions-item>
          </el-descriptions>

          <el-descriptions title="收件信息" :column="2" border class="transport-detail__block">
            <el-descriptions-item label="收件人">{{ detail.order?.receiverName || "-" }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ detail.order?.receiverPhone || "-" }}</el-descriptions-item>
            <el-descriptions-item label="所在地区" :span="2">{{ receiverArea }}</el-descriptions-item>
            <el-descriptions-item label="详细地址" :span="2">{{ detail.order?.receiverAddress || "-" }}</el-descriptions-item>
          </el-descriptions>

          <div
            v-for="item in pickupDispatchBlocks"
            :key="item.title"
            class="transport-detail__block"
          >
            <div class="transport-detail__title">{{ item.title }}</div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="任务编号">{{ item.task.id }}</el-descriptions-item>
              <el-descriptions-item label="任务状态">{{ pickupTaskStatusLabel(item.task.status) }}</el-descriptions-item>
              <el-descriptions-item label="所属网点">{{ item.task.agency?.name || "-" }}</el-descriptions-item>
              <el-descriptions-item label="分配状态">{{ assignedStatusLabel(item.task.assignedStatus) }}</el-descriptions-item>
              <el-descriptions-item label="快递员">{{ item.task.courier?.name || "-" }}</el-descriptions-item>
              <el-descriptions-item label="快递员电话">{{ item.task.courier?.mobile || "-" }}</el-descriptions-item>
              <el-descriptions-item label="预计开始">{{ item.task.estimatedStartTime || "-" }}</el-descriptions-item>
              <el-descriptions-item label="实际开始">{{ item.task.actualStartTime || "-" }}</el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ item.task.mark || "-" }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <div v-if="detail.taskTransports?.length" class="transport-detail__block">
            <div class="transport-detail__title">运输信息（{{ detail.taskTransports.length }}）</div>
            <el-table :data="detail.taskTransports" border size="small">
              <el-table-column label="任务号" prop="id" min-width="150" show-overflow-tooltip />
              <el-table-column label="车次" min-width="120" show-overflow-tooltip>
                <template #default="scope">{{ scope.row.transportTrips?.name || "-" }}</template>
              </el-table-column>
              <el-table-column label="车牌" min-width="100">
                <template #default="scope">{{ scope.row.truck?.licensePlate || "-" }}</template>
              </el-table-column>
              <el-table-column label="状态" width="90" align="center">
                <template #default="scope">
                  <el-tag :type="taskStatusTagType(scope.row.status)" size="small">
                    {{ taskStatusLabel(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="计划发车" prop="planDepartureTime" width="160" />
            </el-table>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import TransportOrderAPI from "@/api/transport-order";
import {
  TransportOrderStatus,
  SchedulingStatus,
  type TransportOrderRecord,
  type TransportOrderDetail,
  type TransportOrderPageQuery,
  type TaskPickupDispatch,
} from "@/api/transport-order";

type TagType = "primary" | "success" | "info" | "warning" | "danger";

/** 运单状态选项（搜索下拉 + 标签） */
const ORDER_STATUS_OPTIONS: { value: TransportOrderStatus; label: string; type: TagType }[] = [
  { value: TransportOrderStatus.CREATED, label: "新建", type: "info" },
  { value: TransportOrderStatus.LOADED, label: "已装车", type: "warning" },
  { value: TransportOrderStatus.ARRIVED, label: "到达", type: "primary" },
  { value: TransportOrderStatus.AT_TERMINAL, label: "到达终端网点", type: "primary" },
];

const statusLabel = (status?: number) =>
  ORDER_STATUS_OPTIONS.find((item) => item.value === status)?.label ?? "未知";
const statusTagType = (status?: number): TagType =>
  ORDER_STATUS_OPTIONS.find((item) => item.value === status)?.type ?? "info";

/** 调度状态仅展示，不作为查询条件（后端 TransportOrderQueryDTO 不支持） */
const schedulingStatusLabel = (status?: number) =>
  ({ [SchedulingStatus.PENDING]: "待调度", [SchedulingStatus.NO_LINE]: "未匹配线路", [SchedulingStatus.SCHEDULED]: "已调度" } as Record<number, string>)[
    status ?? -1
  ] ?? "-";
const schedulingTagType = (status?: number): TagType =>
  status === SchedulingStatus.SCHEDULED ? "success" : status === SchedulingStatus.NO_LINE ? "danger" : "info";

/** 取派件任务相关文案 */
const pickupTaskStatusLabel = (status?: number) =>
  ({ 1: "待执行", 2: "进行中", 3: "待确认", 4: "已完成", 5: "已取消" } as Record<number, string>)[status ?? -1] ?? "-";
const assignedStatusLabel = (status?: number) =>
  ({ 1: "未分配", 2: "已分配", 3: "待人工分配" } as Record<number, string>)[status ?? -1] ?? "-";

/** 运输任务状态文案（运输信息表） */
const taskStatusLabel = (status?: number) =>
  ({ 1: "待执行", 2: "进行中", 3: "待确认", 4: "已完成", 5: "已取消" } as Record<number, string>)[status ?? -1] ?? "-";
const taskStatusTagType = (status?: number): TagType =>
  ({ 1: "info", 2: "warning", 3: "warning", 4: "success", 5: "info" } as Record<number, TagType>)[status ?? -1] ?? "info";

/** 表单查询条件（扁平，提交时寄件字段组装进 order 子对象） */
const query = ref<{
  page: number;
  pageSize: number;
  id?: string;
  status?: number;
  senderName?: string;
  senderPhone?: string;
}>({ page: 1, pageSize: 10 });

/** 组装请求体：寄件人/电话放进 order，空值剔除 */
function buildPayload(): TransportOrderPageQuery {
  const payload: TransportOrderPageQuery = {
    page: query.value.page,
    pageSize: query.value.pageSize,
  };
  if (query.value.id) payload.id = query.value.id;
  if (query.value.status != null) payload.status = query.value.status;
  if (query.value.senderName || query.value.senderPhone) {
    payload.order = {
      senderName: query.value.senderName || undefined,
      senderPhone: query.value.senderPhone || undefined,
    };
  }
  return payload;
}

/** 查询与列表状态 */
const loading = ref(false);
const list = ref<TransportOrderRecord[]>([]);
const total = ref(0);

async function loadList() {
  loading.value = true;
  try {
    const res = await TransportOrderAPI.page(buildPayload());
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
const detail = ref<TransportOrderDetail>();

async function openDetail(row: TransportOrderRecord) {
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = undefined;
  try {
    detail.value = await TransportOrderAPI.getDetail(row.id);
  } finally {
    detailLoading.value = false;
  }
}

const joinArea = (...areas: ({ name?: string } | undefined)[]) =>
  areas.map((area) => area?.name).filter(Boolean).join(" / ") || "-";
const senderArea = computed(() =>
  joinArea(detail.value?.order?.senderProvince, detail.value?.order?.senderCity, detail.value?.order?.senderCounty)
);
const receiverArea = computed(() =>
  joinArea(detail.value?.order?.receiverProvince, detail.value?.order?.receiverCity, detail.value?.order?.receiverCounty)
);

/** 详情中的取件/派件任务块 */
const pickupDispatchBlocks = computed(() => {
  const blocks: { title: string; task: TaskPickupDispatch }[] = [];
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
