<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <el-form class="page-search" :model="query" inline>
        <el-form-item label="结算单号" prop="settleNo">
          <el-input
            v-model="query.settleNo"
            placeholder="DJS 结算单号"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="(label, value) in SETTLEMENT_STATUS_LABELS" :key="value" :value="Number(value)" :label="label" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="page-toolbar">
        <el-button v-hasPerm="'finance:driver-settlement:generate'" type="primary" @click="openGenerate">
          生成结算单
        </el-button>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="settleNo" label="结算单号" min-width="160" show-overflow-tooltip />
          <el-table-column prop="driverName" label="司机" min-width="110" show-overflow-tooltip />
          <el-table-column label="账期" min-width="180" align="center">
            <template #default="scope">{{ (scope.row as DriverSettlementRecord).periodStart }} ~ {{ (scope.row as DriverSettlementRecord).periodEnd }}</template>
          </el-table-column>
          <el-table-column prop="tripCount" label="趟次" width="70" align="center" />
          <el-table-column label="底薪(元)" width="100" align="right">
            <template #default="scope">{{ formatMoney(scope.row.baseWage) }}</template>
          </el-table-column>
          <el-table-column label="趟次费(元)" width="110" align="right">
            <template #default="scope">{{ formatMoney(scope.row.tripFee) }}</template>
          </el-table-column>
          <el-table-column label="补贴(元)" width="100" align="right">
            <template #default="scope">{{ formatMoney(scope.row.allowance) }}</template>
          </el-table-column>
          <el-table-column label="扣款(元)" width="100" align="right">
            <template #default="scope">{{ formatMoney(scope.row.deduction) }}</template>
          </el-table-column>
          <el-table-column label="应发(元)" width="110" align="right">
            <template #default="scope">
              <span class="amount-strong">{{ formatMoney(scope.row.totalAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.status)" effect="plain">
                {{ SETTLEMENT_STATUS_LABELS[scope.row.status] ?? scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="生成时间" width="170" align="center">
            <template #default="scope">{{ (scope.row as DriverSettlementRecord).createTime ?? "-" }}</template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right" align="center">
            <template #default="scope">
              <el-button link type="primary" @click="openDetail(scope.row as DriverSettlementRecord)">详情</el-button>
              <el-button
                v-if="scope.row.status === SettlementStatus.PENDING"
                v-hasPerm="'finance:driver-settlement:audit'"
                link
                type="success"
                @click="handleAudit(scope.row as DriverSettlementRecord)"
              >
                审核
              </el-button>
              <el-button
                v-if="scope.row.status === SettlementStatus.AUDITED"
                v-hasPerm="'finance:driver-settlement:pay'"
                link
                type="warning"
                @click="handlePay(scope.row as DriverSettlementRecord)"
              >
                发放
              </el-button>
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

    <!-- 生成结算单弹窗（写操作居中弹窗 + append-to-body） -->
    <el-dialog v-model="generateVisible" title="生成司机结算单" width="560px" append-to-body>
      <el-form ref="generateFormRef" :model="generateForm" :rules="generateRules" label-width="90px">
        <el-form-item label="账期" prop="period">
          <el-date-picker
            v-model="generateForm.period"
            type="daterange"
            range-separator="至"
            start-placeholder="账期起"
            end-placeholder="账期止"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            @change="loadCandidates"
          />
        </el-form-item>
        <el-form-item label="司机" prop="driverId">
          <el-select
            v-model="generateForm.driverId"
            filterable
            :loading="candidatesLoading"
            placeholder="先选账期，再选可结算司机"
            style="width: 100%"
          >
            <el-option
              v-for="d in candidates"
              :key="d.driverId"
              :value="d.driverId"
              :label="`${d.driverName ?? d.driverId}（可结算 ${d.tripCount} 趟）`"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="底薪(元)" prop="baseWage">
          <el-input-number v-model="generateForm.baseWage" :min="0" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="补贴(元)" prop="allowance">
          <el-input-number v-model="generateForm.allowance" :min="0" :precision="2" :step="50" style="width: 100%" />
        </el-form-item>
        <el-form-item label="扣款(元)" prop="deduction">
          <el-input-number v-model="generateForm.deduction" :min="0" :precision="2" :step="50" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="generateForm.remark" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="选填，如油补/过路说明、扣款原因" />
        </el-form-item>
      </el-form>
      <div class="form-tip">
        系统按每趟固定费率归集账期内已完成趟次（明细可在详情查看）；底薪/补贴/扣款为手工调差项，应发 = 底薪 + 趟次费 + 补贴 − 扣款。
        同司机账期重叠已有结算单时将拒绝重复生成。
      </div>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="handleGenerate">生成</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉（读操作居右抽屉） -->
    <el-drawer v-model="detailVisible" title="结算单详情" size="620px" append-to-body>
      <template #header>
        <div class="detail-header">
          <span>结算单详情</span>
          <el-tag v-if="detail" :type="statusTagType(detail.settlement.status)" effect="plain" style="margin-left: 8px">
            {{ SETTLEMENT_STATUS_LABELS[detail.settlement.status] ?? detail.settlement.status }}
          </el-tag>
        </div>
      </template>
      <div v-if="detail" v-loading="detailLoading">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="结算单号" :span="2">{{ detail.settlement.settleNo }}</el-descriptions-item>
          <el-descriptions-item label="司机">{{ detail.settlement.driverName ?? detail.settlement.driverId }}</el-descriptions-item>
          <el-descriptions-item label="完成趟次">{{ detail.settlement.tripCount }}</el-descriptions-item>
          <el-descriptions-item label="账期" :span="2">{{ detail.settlement.periodStart }} ~ {{ detail.settlement.periodEnd }}</el-descriptions-item>
          <el-descriptions-item label="底薪(元)">{{ formatMoney(detail.settlement.baseWage) }}</el-descriptions-item>
          <el-descriptions-item label="趟次费(元)">{{ formatMoney(detail.settlement.tripFee) }}</el-descriptions-item>
          <el-descriptions-item label="补贴(元)">{{ formatMoney(detail.settlement.allowance) }}</el-descriptions-item>
          <el-descriptions-item label="扣款(元)">{{ formatMoney(detail.settlement.deduction) }}</el-descriptions-item>
          <el-descriptions-item label="应发(元)" :span="2">
            <span class="amount-strong">{{ formatMoney(detail.settlement.totalAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.settlement.auditRemark" label="审核备注" :span="2">
            {{ detail.settlement.auditRemark }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.settlement.paidTime" label="发放时间" :span="2">
            {{ detail.settlement.paidTime }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.settlement.remark" label="备注" :span="2">
            {{ detail.settlement.remark }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-section-title">逐趟明细（{{ detail.details?.length ?? 0 }} 趟）</div>
        <el-table :data="detail.details ?? []" border size="small" max-height="360">
          <el-table-column prop="taskTransportId" label="运输任务" min-width="150" show-overflow-tooltip />
          <el-table-column prop="driverJobId" label="司机作业单" min-width="150" show-overflow-tooltip />
          <el-table-column label="金额(元)" width="100" align="right">
            <template #default="scope">{{ formatMoney(scope.row.amount) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import DriverSettlementAPI, {
  SETTLEMENT_STATUS_LABELS,
  SettlementStatus,
  type CandidateDriver,
  type DriverSettlementRecord,
  type SettlementDetailRecord,
} from "@/api/driver-settlement";

defineOptions({ name: "DriverSettlement" });

/** 查询与列表状态 */
const query = ref<{ page: number; pageSize: number; settleNo?: string; status?: number }>({
  page: 1,
  pageSize: 10,
});

const loading = ref(false);
const list = ref<DriverSettlementRecord[]>([]);
const total = ref(0);

async function loadList() {
  loading.value = true;
  try {
    const res = await DriverSettlementAPI.page({
      page: query.value.page,
      pageSize: query.value.pageSize,
      settleNo: query.value.settleNo?.trim() || undefined,
      status: query.value.status,
    });
    list.value = res?.records ?? [];
    total.value = res?.total ?? 0;
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

function formatMoney(value: number | null | undefined): string {
  if (value == null) return "-";
  return Number(value).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function statusTagType(status: number): "warning" | "success" | "info" | "primary" {
  if (status === SettlementStatus.PENDING) return "warning";
  if (status === SettlementStatus.AUDITED) return "primary";
  if (status === SettlementStatus.PAID) return "success";
  return "info";
}

/** 生成结算单弹窗 */
const generateVisible = ref(false);
const generating = ref(false);
const generateFormRef = ref<FormInstance>();
const generateForm = reactive<{
  period: [string, string] | undefined;
  driverId: string | undefined;
  baseWage: number;
  allowance: number;
  deduction: number;
  remark: string;
}>({
  period: undefined,
  driverId: undefined,
  baseWage: 0,
  allowance: 0,
  deduction: 0,
  remark: "",
});

const generateRules: FormRules = {
  period: [{ required: true, message: "请选择账期", trigger: "change" }],
  driverId: [{ required: true, message: "请选择司机", trigger: "change" }],
};

/** 候选司机（账期内有可归集趟次；同司机账期重叠重复生成由后端幂等拒绝） */
const candidates = ref<CandidateDriver[]>([]);
const candidatesLoading = ref(false);

async function loadCandidates() {
  candidates.value = [];
  generateForm.driverId = undefined;
  if (!generateForm.period) return;
  const [periodStart, periodEnd] = generateForm.period;
  candidatesLoading.value = true;
  try {
    candidates.value = (await DriverSettlementAPI.drivers({ periodStart, periodEnd })) ?? [];
  } catch {
    candidates.value = [];
  } finally {
    candidatesLoading.value = false;
  }
}

function openGenerate() {
  generateForm.period = undefined;
  generateForm.driverId = undefined;
  generateForm.baseWage = 0;
  generateForm.allowance = 0;
  generateForm.deduction = 0;
  generateForm.remark = "";
  candidates.value = [];
  generateVisible.value = true;
}

async function handleGenerate() {
  await generateFormRef.value?.validate().catch(() => Promise.reject(new Error("validate")));
  if (!generateForm.period) return;
  generating.value = true;
  try {
    await DriverSettlementAPI.generate({
      driverId: generateForm.driverId!,
      periodStart: generateForm.period[0],
      periodEnd: generateForm.period[1],
      baseWage: generateForm.baseWage || undefined,
      allowance: generateForm.allowance || undefined,
      deduction: generateForm.deduction || undefined,
      remark: generateForm.remark.trim() || undefined,
    });
    ElMessage.success("结算单已生成");
    generateVisible.value = false;
    loadList();
  } finally {
    generating.value = false;
  }
}

/** 详情抽屉（读操作居右） */
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<{ settlement: DriverSettlementRecord; details: SettlementDetailRecord[] } | null>(null);

async function openDetail(row: DriverSettlementRecord) {
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    detail.value = await DriverSettlementAPI.detail(row.id);
  } finally {
    detailLoading.value = false;
  }
}

/** 审核（0→1，备注选填） */
async function handleAudit(row: DriverSettlementRecord) {
  const { value } = await ElMessageBox.prompt("审核备注（选填）", `审核结算单 ${row.settleNo}`, {
    type: "warning",
    inputPlaceholder: "选填",
    inputValue: "",
  }).catch(() => Promise.reject(new Error("cancel")));
  await DriverSettlementAPI.audit(row.id, value?.trim() || undefined);
  ElMessage.success("已审核通过");
  loadList();
}

/** 发放登记（1→2，线下打款后操作） */
async function handlePay(row: DriverSettlementRecord) {
  await ElMessageBox.confirm(
    `确认已线下打款并登记发放？应发 ¥${formatMoney(row.totalAmount)}，登记后不可回退。`,
    `发放结算单 ${row.settleNo}`,
    { type: "warning" },
  );
  await DriverSettlementAPI.pay(row.id);
  ElMessage.success("已登记发放");
  loadList();
}

onMounted(loadList);
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
  margin: 0 0 12px 24px;
}

.detail-header {
  display: flex;
  align-items: center;
}

.detail-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 16px 0 8px;
}

.amount-strong {
  font-weight: 600;
  color: var(--el-color-danger);
}
</style>
