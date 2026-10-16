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
          <el-table-column label="操作" fixed="right" width="140" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openDetail(scope.row as DriverJobRecord)">详情</el-button>
              <el-button
                type="primary"
                link
                size="small"
                v-hasPerm="['driverJob:update']"
                @click="openFix(scope.row as DriverJobRecord)"
              >干预</el-button>
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

    <!-- 作业单干预（状态白名单：复位待执行/作废 + 对接人纠错） -->
    <el-dialog v-model="fixVisible" title="作业单干预" width="520px" :close-on-click-modal="false">
      <el-form ref="fixFormRef" :model="fixForm" :rules="fixRules" label-width="100px">
        <el-form-item label="作业单号">
          <el-input :model-value="fixTarget?.id" disabled />
        </el-form-item>
        <el-form-item label="当前状态">
          <el-tag :type="statusTagType(fixTarget?.status)" size="small">{{ statusLabel(fixTarget?.status) }}</el-tag>
        </el-form-item>
        <el-form-item label="目标状态" prop="status">
          <el-select v-model="fixForm.status" placeholder="选择修正后状态" clearable style="width: 220px">
            <el-option label="复位为待执行" :value="DriverJobStatus.PENDING" />
            <el-option label="作废" :value="DriverJobStatus.CANCELLED" />
          </el-select>
          <div class="page-text-sub" style="line-height: 1.5; margin-top: 4px">
            仅允许复位待执行或作废；进行中/改派/已完成由司机端流转产生，管理端不可伪造
          </div>
        </el-form-item>
        <el-form-item label="提货对接人">
          <el-input v-model="fixForm.startHandover" placeholder="留空则不修改" maxlength="30" />
        </el-form-item>
        <el-form-item label="交付对接人">
          <el-input v-model="fixForm.finishHandover" placeholder="留空则不修改" maxlength="30" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fixVisible = false">取消</el-button>
        <el-button type="primary" :loading="fixSubmitting" @click="handleFix">确认修正</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { FormInstance, ElMessage } from "element-plus";
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

/** 作业单干预（状态白名单修正 + 对接人纠错） */
interface DriverJobFixForm {
  status?: number;
  startHandover?: string;
  finishHandover?: string;
}

const fixVisible = ref(false);
const fixSubmitting = ref(false);
const fixFormRef = ref<FormInstance>();
const fixTarget = ref<DriverJobRecord>();
const fixForm = ref<DriverJobFixForm>({});

const fixRules = {
  status: [{ required: true, message: "请选择目标状态", trigger: "change" }],
};

function openFix(row: DriverJobRecord) {
  fixTarget.value = row;
  fixForm.value = {};
  fixVisible.value = true;
}

async function handleFix() {
  const valid = await fixFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  // 目标状态与当前一致时不发请求
  if (fixForm.value.status === fixTarget.value?.status) {
    ElMessage.info("目标状态与当前状态一致，无需修正");
    return;
  }
  fixSubmitting.value = true;
  try {
    await DriverJobAPI.update(fixTarget.value!.id, {
      status: fixForm.value.status,
      startHandover: fixForm.value.startHandover || undefined,
      finishHandover: fixForm.value.finishHandover || undefined,
    });
    ElMessage.success("修正成功");
    fixVisible.value = false;
    loadList();
  } finally {
    fixSubmitting.value = false;
  }
}

onMounted(loadList);
</script>

<style lang="scss" scoped>
/* 司机作业单详情抽屉：各描述区块之间留白（首个"基本信息"无此类，后续区块均带此类） */
.driver-job-detail__block {
  margin-top: 16px;
}
</style>
