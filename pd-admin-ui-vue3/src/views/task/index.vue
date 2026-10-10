<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <el-form class="page-search" :model="query" inline>
        <el-form-item label="任务号" prop="id">
          <el-input v-model="query.id" placeholder="请输入任务号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="任务状态" prop="status">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option
              v-for="item in TASK_STATUS_OPTIONS"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="司机姓名" prop="driverName">
          <el-input v-model="query.driverName" placeholder="司机姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="id" label="任务号" min-width="160" show-overflow-tooltip />
          <el-table-column label="司机" min-width="140">
            <template #default="scope">{{ driverNames(scope.row as TaskTransportRecord) }}</template>
          </el-table-column>
          <el-table-column label="车次" min-width="140" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.transportTrips?.name || "-" }}</template>
          </el-table-column>
          <el-table-column label="运输线路" min-width="220" show-overflow-tooltip>
            <template #default="scope">{{ routeText(scope.row as TaskTransportRecord) }}</template>
          </el-table-column>
          <el-table-column label="车牌" min-width="110">
            <template #default="scope">{{ scope.row.truck?.licensePlate || "-" }}</template>
          </el-table-column>
          <el-table-column prop="status" label="任务状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="assignedStatus" label="分配状态" width="100" align="center">
            <template #default="scope">{{ assignedStatusLabel(scope.row.assignedStatus) }}</template>
          </el-table-column>
          <el-table-column prop="planDepartureTime" label="计划发车" width="170" />
          <el-table-column label="操作" fixed="right" width="90" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openDetail(scope.row as TaskTransportRecord)">详情</el-button>
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

    <!-- 运输任务详情 -->
    <el-drawer v-model="detailVisible" title="运输任务详情" size="640px">
      <div v-loading="detailLoading" class="task-detail">
        <template v-if="detail">
          <el-descriptions title="基本信息" :column="2" border>
            <el-descriptions-item label="任务号" :span="2">{{ detail.id }}</el-descriptions-item>
            <el-descriptions-item label="任务状态">
              <el-tag :type="statusTagType(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="分配状态">{{ assignedStatusLabel(detail.assignedStatus) }}</el-descriptions-item>
            <el-descriptions-item label="满载状态">{{ loadingStatusLabel(detail.loadingStatus) }}</el-descriptions-item>
            <el-descriptions-item label="运单数量">{{ detail.transportOrderCount ?? "-" }}</el-descriptions-item>
            <el-descriptions-item label="创建时间" :span="2">{{ detail.createTime || "-" }}</el-descriptions-item>
          </el-descriptions>

          <el-descriptions title="车辆信息" :column="2" border class="task-detail__block">
            <el-descriptions-item label="车牌号">{{ detail.truck?.licensePlate || "-" }}</el-descriptions-item>
            <el-descriptions-item label="品牌">{{ detail.truck?.brand || "-" }}</el-descriptions-item>
            <el-descriptions-item label="准载重量(kg)">{{ detail.truck?.allowableLoad ?? "-" }}</el-descriptions-item>
            <el-descriptions-item label="准载体积(m³)">{{ detail.truck?.allowableVolume ?? "-" }}</el-descriptions-item>
          </el-descriptions>

          <div class="task-detail__block">
            <div class="task-detail__title">司机信息</div>
            <el-table :data="detail.drivers ?? []" border size="small">
              <el-table-column label="姓名" prop="name" min-width="100" />
              <el-table-column label="手机号" prop="mobile" min-width="130" />
              <el-table-column label="岗位" min-width="90">
                <template #default="scope">{{ scope.row.stationName || "-" }}</template>
              </el-table-column>
            </el-table>
          </div>

          <el-descriptions title="车次信息" :column="2" border class="task-detail__block">
            <el-descriptions-item label="车次名称">{{ detail.transportTrips?.name || "-" }}</el-descriptions-item>
            <el-descriptions-item label="周期">{{ detail.transportTrips?.periodName || "-" }}</el-descriptions-item>
            <el-descriptions-item label="计划发车">{{ detail.transportTrips?.departureTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="计划到达">{{ detail.transportTrips?.arrivalTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="起始机构">{{ detail.startAgency?.name || "-" }}</el-descriptions-item>
            <el-descriptions-item label="目的机构">{{ detail.endAgency?.name || "-" }}</el-descriptions-item>
          </el-descriptions>

          <el-descriptions title="时间节点" :column="2" border class="task-detail__block">
            <el-descriptions-item label="计划发车">{{ detail.planDepartureTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="实际发车">{{ detail.actualDepartureTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="计划到达">{{ detail.planArrivalTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="实际到达">{{ detail.actualArrivalTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="计划提货">{{ detail.planPickUpGoodsTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="实际提货">{{ detail.actualPickUpGoodsTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="计划交付">{{ detail.planDeliveryTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="实际交付">{{ detail.actualDeliveryTime || "-" }}</el-descriptions-item>
          </el-descriptions>

          <div v-if="detail.transportOrders?.length" class="task-detail__block">
            <div class="task-detail__title">关联运单（{{ detail.transportOrders.length }}）</div>
            <div class="task-detail__orders">
              <el-tag
                v-for="item in detail.transportOrders"
                :key="item.id"
                class="task-detail__order-tag"
                type="info"
              >
                {{ item.id }}
              </el-tag>
            </div>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import TransportTaskAPI from "@/api/transport-task";
import {
  TransportTaskStatus,
  type TaskTransportRecord,
  type TaskTransportDetail,
  type TaskTransportPageQuery,
} from "@/api/transport-task";

type TagType = "primary" | "success" | "info" | "warning" | "danger";

/** 任务状态选项（搜索下拉 + 标签） */
const TASK_STATUS_OPTIONS: { value: TransportTaskStatus; label: string; type: TagType }[] = [
  { value: TransportTaskStatus.PENDING, label: "待执行", type: "info" },
  { value: TransportTaskStatus.PROCESSING, label: "进行中", type: "warning" },
  { value: TransportTaskStatus.TO_CONFIRM, label: "待确认", type: "warning" },
  { value: TransportTaskStatus.FINISHED, label: "已完成", type: "success" },
  { value: TransportTaskStatus.CANCELLED, label: "已取消", type: "info" },
];

const statusLabel = (status?: number) =>
  TASK_STATUS_OPTIONS.find((item) => item.value === status)?.label ?? "未知";
const statusTagType = (status?: number): TagType =>
  TASK_STATUS_OPTIONS.find((item) => item.value === status)?.type ?? "info";

const assignedStatusLabel = (status?: number) =>
  ({ 1: "未分配", 2: "已分配", 3: "待人工分配" } as Record<number, string>)[status ?? -1] ?? "-";
const loadingStatusLabel = (status?: number) =>
  ({ 1: "半载", 2: "满载", 3: "空载" } as Record<number, string>)[status ?? -1] ?? "-";

/** 司机姓名拼接（任务可绑定多个司机） */
const driverNames = (row: TaskTransportRecord) =>
  (row.drivers ?? []).map((item) => item.name).filter(Boolean).join("、") || "-";
/** 起始→目的机构 */
const routeText = (row: TaskTransportRecord) => {
  const start = row.startAgency?.name;
  const end = row.endAgency?.name;
  if (!start && !end) return "-";
  return `${start ?? ""} → ${end ?? ""}`;
};

/** 查询与列表状态 */
const loading = ref(false);
const list = ref<TaskTransportRecord[]>([]);
const total = ref(0);
const query = ref<TaskTransportPageQuery>({ page: 1, pageSize: 10 });

/** 组装请求体：剔除空字符串/空值，仅提交有效过滤条件 */
function buildPayload(): TaskTransportPageQuery {
  const { page, pageSize, ...filters } = query.value;
  const payload: TaskTransportPageQuery = { page, pageSize };
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
    const res = await TransportTaskAPI.page(buildPayload());
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
const detail = ref<TaskTransportDetail>();

async function openDetail(row: TaskTransportRecord) {
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = undefined;
  try {
    detail.value = await TransportTaskAPI.getDetail(row.id);
  } finally {
    detailLoading.value = false;
  }
}

onMounted(loadList);
</script>
