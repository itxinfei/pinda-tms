<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <el-form class="page-search" :model="query" inline>
        <el-form-item label="任务类型" prop="taskType">
          <el-select v-model="query.taskType" placeholder="全部类型" clearable style="width: 130px">
            <el-option label="取件任务" :value="PickupDispatchTaskType.PICKUP" />
            <el-option label="派件任务" :value="PickupDispatchTaskType.DISPATCH" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务状态" prop="status">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option
              v-for="item in STATUS_OPTIONS"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="快递员" prop="courierName">
          <el-input v-model="query.courierName" placeholder="快递员姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="寄件人" prop="senderName">
          <el-input v-model="query.senderName" placeholder="寄件人姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="收件人" prop="receiverName">
          <el-input v-model="query.receiverName" placeholder="收件人姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="id" label="任务号" min-width="160" show-overflow-tooltip />
          <el-table-column prop="taskType" label="类型" width="90" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.taskType === PickupDispatchTaskType.PICKUP ? 'primary' : 'warning'" size="small">
                {{ taskTypeLabel(scope.row.taskType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="寄件人 → 收件人" min-width="220" show-overflow-tooltip>
            <template #default="scope">
              {{ scope.row.order?.senderName || "-" }} → {{ scope.row.order?.receiverName || "-" }}
            </template>
          </el-table-column>
          <el-table-column label="快递员" min-width="120">
            <template #default="scope">{{ scope.row.courier?.name || "-" }}</template>
          </el-table-column>
          <el-table-column label="所属网点" min-width="130" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.agency?.name || "-" }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="预计时间" min-width="170">
            <template #default="scope">{{ estimatedRange(scope.row as PickupDispatchRecord) }}</template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="90" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openDetail(scope.row as PickupDispatchRecord)">详情</el-button>
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

    <!-- 取派件任务详情（直接展示该行，无详情接口） -->
    <el-drawer v-model="detailVisible" title="取派件任务详情" size="640px">
      <template v-if="detail">
        <el-descriptions title="基本信息" :column="2" border>
          <el-descriptions-item label="任务号" :span="2">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="任务类型">
            <el-tag :type="detail.taskType === PickupDispatchTaskType.PICKUP ? 'primary' : 'warning'" size="small">
              {{ taskTypeLabel(detail.taskType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="任务状态">
            <el-tag :type="statusTagType(detail.status)" size="small">{{ statusLabel(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="签收情况">{{ signStatusLabel(detail.signStatus) }}</el-descriptions-item>
          <el-descriptions-item label="分配状态">{{ assignedStatusLabel(detail.assignedStatus) }}</el-descriptions-item>
          <el-descriptions-item label="关联运单号">{{ detail.transportOrder?.id || "-" }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.createTime || "-" }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="寄件信息" :column="2" border class="pickup-dispatch-detail__block">
          <el-descriptions-item label="寄件人">{{ detail.order?.senderName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ detail.order?.senderPhone || "-" }}</el-descriptions-item>
          <el-descriptions-item label="所在地区" :span="2">{{ senderArea }}</el-descriptions-item>
          <el-descriptions-item label="详细地址" :span="2">{{ detail.order?.senderAddress || "-" }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="收件信息" :column="2" border class="pickup-dispatch-detail__block">
          <el-descriptions-item label="收件人">{{ detail.order?.receiverName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ detail.order?.receiverPhone || "-" }}</el-descriptions-item>
          <el-descriptions-item label="所在地区" :span="2">{{ receiverArea }}</el-descriptions-item>
          <el-descriptions-item label="详细地址" :span="2">{{ detail.order?.receiverAddress || "-" }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="快递员与网点" :column="2" border class="pickup-dispatch-detail__block">
          <el-descriptions-item label="快递员">{{ detail.courier?.name || "-" }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.courier?.mobile || "-" }}</el-descriptions-item>
          <el-descriptions-item label="所属网点" :span="2">{{ detail.agency?.name || "-" }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="时间节点" :column="2" border class="pickup-dispatch-detail__block">
          <el-descriptions-item label="预计开始">{{ detail.estimatedStartTime || "-" }}</el-descriptions-item>
          <el-descriptions-item label="实际开始">{{ detail.actualStartTime || "-" }}</el-descriptions-item>
          <el-descriptions-item label="预计完成">{{ detail.estimatedEndTime || "-" }}</el-descriptions-item>
          <el-descriptions-item label="实际完成">{{ detail.actualEndTime || "-" }}</el-descriptions-item>
          <el-descriptions-item label="确认时间">{{ detail.confirmTime || "-" }}</el-descriptions-item>
          <el-descriptions-item label="取消时间">{{ detail.cancelTime || "-" }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.mark || "-" }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import PickupDispatchAPI from "@/api/pickup-dispatch";
import {
  PickupDispatchTaskType,
  PickupDispatchStatus,
  type PickupDispatchRecord,
  type PickupDispatchPageQuery,
} from "@/api/pickup-dispatch";

type TagType = "primary" | "success" | "info" | "warning" | "danger";

/** 任务状态选项（搜索下拉 + 标签；状态 2 为保留态，不列出） */
const STATUS_OPTIONS: { value: PickupDispatchStatus; label: string; type: TagType }[] = [
  { value: PickupDispatchStatus.PENDING, label: "待执行", type: "info" },
  { value: PickupDispatchStatus.TO_CONFIRM, label: "待确认", type: "warning" },
  { value: PickupDispatchStatus.FINISHED, label: "已完成", type: "success" },
  { value: PickupDispatchStatus.CANCELLED, label: "已取消", type: "info" },
];

const taskTypeLabel = (type?: number) =>
  type === PickupDispatchTaskType.PICKUP ? "取件" : type === PickupDispatchTaskType.DISPATCH ? "派件" : "-";
const statusLabel = (status?: number) =>
  STATUS_OPTIONS.find((item) => item.value === status)?.label ?? "未知";
const statusTagType = (status?: number): TagType =>
  STATUS_OPTIONS.find((item) => item.value === status)?.type ?? "info";

const signStatusLabel = (status?: number) =>
  ({ 1: "已签收", 2: "拒收" } as Record<number, string>)[status ?? -1] ?? "-";
const assignedStatusLabel = (status?: number) =>
  ({ 1: "未分配", 2: "已分配", 3: "待人工分配" } as Record<number, string>)[status ?? -1] ?? "-";

/** 预计时间区间 */
const estimatedRange = (row: PickupDispatchRecord) => {
  const start = row.estimatedStartTime;
  const end = row.estimatedEndTime;
  if (!start && !end) return "-";
  return `${start ?? ""} ~ ${end ?? ""}`;
};

/** 表单查询条件（扁平，提交时组装进后端子对象） */
const query = ref<{
  page: number;
  pageSize: number;
  taskType?: number;
  status?: number;
  courierName?: string;
  senderName?: string;
  receiverName?: string;
}>({ page: 1, pageSize: 10 });

/** 组装请求体：快递员/寄收件人放进对应子对象，空值剔除 */
function buildPayload(): PickupDispatchPageQuery {
  const payload: PickupDispatchPageQuery = {
    page: query.value.page,
    pageSize: query.value.pageSize,
  };
  if (query.value.taskType != null) payload.taskType = query.value.taskType;
  if (query.value.status != null) payload.status = query.value.status;
  if (query.value.courierName) payload.courier = { name: query.value.courierName };
  if (query.value.senderName || query.value.receiverName) {
    payload.order = {
      senderName: query.value.senderName || undefined,
      receiverName: query.value.receiverName || undefined,
    };
  }
  return payload;
}

/** 查询与列表状态 */
const loading = ref(false);
const list = ref<PickupDispatchRecord[]>([]);
const total = ref(0);

async function loadList() {
  loading.value = true;
  try {
    const res = await PickupDispatchAPI.page(buildPayload());
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

/** 详情（直接取该行数据，无独立详情接口） */
const detailVisible = ref(false);
const detail = ref<PickupDispatchRecord>();

function openDetail(row: PickupDispatchRecord) {
  detail.value = row;
  detailVisible.value = true;
}

const joinArea = (...areas: ({ name?: string } | undefined)[]) =>
  areas.map((area) => area?.name).filter(Boolean).join(" / ") || "-";
const senderArea = computed(() =>
  joinArea(detail.value?.order?.senderProvince, detail.value?.order?.senderCity, detail.value?.order?.senderCounty)
);
const receiverArea = computed(() =>
  joinArea(detail.value?.order?.receiverProvince, detail.value?.order?.receiverCity, detail.value?.order?.receiverCounty)
);

onMounted(loadList);
</script>
