<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <el-form class="page-search" :model="query" inline>
        <el-form-item label="作业单号" prop="id">
          <el-input v-model="query.id" placeholder="请输入作业单号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="司机姓名" prop="driverName">
          <el-input v-model="query.driverName" placeholder="司机姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="作业状态" prop="status">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option
              v-for="item in STATUS_OPTIONS"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="id" label="作业单号" min-width="160" show-overflow-tooltip />
          <el-table-column label="司机" min-width="120">
            <template #default="scope">
              <div>{{ scope.row.driver?.name || "-" }}</div>
              <div class="page-text-sub">{{ scope.row.driver?.mobile }}</div>
            </template>
          </el-table-column>
          <el-table-column label="起始 → 目的机构" min-width="230" show-overflow-tooltip>
            <template #default="scope">
              {{ scope.row.startAgency?.name || "-" }} → {{ scope.row.endAgency?.name || "-" }}
            </template>
          </el-table-column>
          <el-table-column label="运输任务" min-width="150" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.taskTransport?.id || "-" }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="planDepartureTime" label="计划发车" width="170" />
          <el-table-column label="操作" fixed="right" width="90" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openDetail(scope.row as DriverJobRecord)">详情</el-button>
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

    <!-- 司机作业单详情（直接展示该行，无详情接口） -->
    <el-drawer v-model="detailVisible" title="司机作业单详情" size="640px">
      <template v-if="detail">
        <el-descriptions title="基本信息" :column="2" border>
          <el-descriptions-item label="作业单号" :span="2">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="作业状态">
            <el-tag :type="statusTagType(detail.status)" size="small">{{ statusLabel(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.createTime || "-" }}</el-descriptions-item>
          <el-descriptions-item label="起始机构" :span="2">{{ detail.startAgency?.name || "-" }}</el-descriptions-item>
          <el-descriptions-item label="目的机构" :span="2">{{ detail.endAgency?.name || "-" }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="司机与运输任务" :column="2" border class="driver-job-detail__block">
          <el-descriptions-item label="司机">{{ detail.driver?.name || "-" }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.driver?.mobile || "-" }}</el-descriptions-item>
          <el-descriptions-item label="关联运输任务" :span="2">{{ detail.taskTransport?.id || "-" }}</el-descriptions-item>
          <el-descriptions-item label="车次">{{ detail.taskTransport?.transportTrips?.name || "-" }}</el-descriptions-item>
          <el-descriptions-item label="车牌">{{ detail.taskTransport?.truck?.licensePlate || "-" }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="对接人" :column="2" border class="driver-job-detail__block">
          <el-descriptions-item label="提货对接人">{{ detail.startHandover || "-" }}</el-descriptions-item>
          <el-descriptions-item label="交付对接人">{{ detail.finishHandover || "-" }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="时间节点" :column="2" border class="driver-job-detail__block">
          <el-descriptions-item label="计划发车">{{ detail.planDepartureTime || "-" }}</el-descriptions-item>
          <el-descriptions-item label="实际发车">{{ detail.actualDepartureTime || "-" }}</el-descriptions-item>
          <el-descriptions-item label="计划到达">{{ detail.planArrivalTime || "-" }}</el-descriptions-item>
          <el-descriptions-item label="实际到达">{{ detail.actualArrivalTime || "-" }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import DriverJobAPI from "@/api/driver-job";
import { DriverJobStatus, type DriverJobRecord, type DriverJobPageQuery } from "@/api/driver-job";

type TagType = "primary" | "success" | "info" | "warning" | "danger";

/** 作业状态选项（搜索下拉 + 标签） */
const STATUS_OPTIONS: { value: DriverJobStatus; label: string; type: TagType }[] = [
  { value: DriverJobStatus.PENDING, label: "待执行", type: "info" },
  { value: DriverJobStatus.PROCESSING, label: "进行中", type: "warning" },
  { value: DriverJobStatus.REASSIGNED, label: "改派", type: "warning" },
  { value: DriverJobStatus.FINISHED, label: "已完成", type: "success" },
  { value: DriverJobStatus.CANCELLED, label: "已作废", type: "info" },
];

const statusLabel = (status?: number) =>
  STATUS_OPTIONS.find((item) => item.value === status)?.label ?? "未知";
const statusTagType = (status?: number): TagType =>
  STATUS_OPTIONS.find((item) => item.value === status)?.type ?? "info";

/** 表单查询条件（扁平，提交时司机姓名组装进 driver 子对象） */
const query = ref<{
  page: number;
  pageSize: number;
  id?: string;
  driverName?: string;
  status?: number;
}>({ page: 1, pageSize: 10 });

/** 组装请求体 */
function buildPayload(): DriverJobPageQuery {
  const payload: DriverJobPageQuery = {
    page: query.value.page,
    pageSize: query.value.pageSize,
  };
  if (query.value.id) payload.id = query.value.id;
  if (query.value.status != null) payload.status = query.value.status;
  if (query.value.driverName) payload.driver = { name: query.value.driverName };
  return payload;
}

/** 查询与列表状态 */
const loading = ref(false);
const list = ref<DriverJobRecord[]>([]);
const total = ref(0);

async function loadList() {
  loading.value = true;
  try {
    const res = await DriverJobAPI.page(buildPayload());
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
const detail = ref<DriverJobRecord>();

function openDetail(row: DriverJobRecord) {
  detail.value = row;
  detailVisible.value = true;
}

onMounted(loadList);
</script>
