<template>
  <div class="page-container">
    <!-- 顶部统计卡：全量口径前端汇总（dev 量级内准确；量大后应下沉后端聚合接口），待审/待发放/已发放可点击下钻 -->
    <el-row :gutter="12" class="stat-row">
      <el-col v-for="card in statCards" :key="card.key" :xs="12" :sm="6">
        <div
          class="stat-card"
          :class="{ 'is-clickable': card.filterable, 'is-active': card.filterable && query.status === card.status }"
          :role="card.filterable ? 'button' : undefined"
          :tabindex="card.filterable ? 0 : undefined"
          :aria-label="`${card.label} ${card.value}，${card.hint}`"
          @click="onStatClick(card)"
          @keydown.enter.prevent="onStatClick(card)"
          @keydown.space.prevent="onStatClick(card)"
        >
          <div class="stat-card__label">{{ card.label }}</div>
          <div class="stat-card__value" :class="`stat-card__value--${card.tone}`">{{ card.value }}</div>
          <div class="stat-card__hint">{{ card.hint }}</div>
        </div>
      </el-col>
    </el-row>

    <el-card class="page-content" shadow="never">
      <!-- 筛选 + 主操作（左筛选右主操作，member 基准范式） -->
      <div class="page-toolbar">
        <div class="page-toolbar__left">
          <el-form :model="query" inline @submit.prevent="handleSearch">
            <el-form-item label="结算单号" prop="settleNo">
              <el-input
                v-model="query.settleNo"
                placeholder="DJS 结算单号"
                clearable
                style="width: 170px"
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px">
                <el-option v-for="(label, value) in SETTLEMENT_STATUS_LABELS" :key="value" :value="Number(value)" :label="label" />
              </el-select>
            </el-form-item>
            <el-form-item label="账期" prop="filterPeriod">
              <el-date-picker
                v-model="filterPeriod"
                type="daterange"
                range-separator="至"
                start-placeholder="开始"
                end-placeholder="结束"
                value-format="YYYY-MM-DD"
                style="width: 240px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="page-toolbar__right">
          <el-button v-hasPerm="'finance:driver-settlement:generate'" type="primary" :icon="Plus" @click="openGenerate">
            生成结算单
          </el-button>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <template #empty>
            <el-empty v-if="loadError" description="加载失败">
              <el-button type="primary" @click="loadList">重试</el-button>
            </el-empty>
            <el-empty v-else description="暂无结算单" />
          </template>
          <!-- 展开行：金额构成（主表瘦身，分项金额收进展开） -->
          <el-table-column type="expand">
            <template #default="scope">
              <div class="expand-row">
                <span class="expand-row__item">底薪 <b>{{ money(scope.row.baseWage) }}</b></span>
                <span class="expand-row__item">趟次费 <b>{{ money(scope.row.tripFee) }}</b></span>
                <span class="expand-row__item">补贴 <b>{{ money(scope.row.allowance) }}</b></span>
                <span class="expand-row__item">扣款 <b>{{ money(scope.row.deduction) }}</b></span>
                <span class="expand-row__formula">应发 = 底薪 + 趟次费 + 补贴 − 扣款</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="settleNo" label="结算单号" min-width="160" show-overflow-tooltip />
          <el-table-column prop="driverName" label="司机" min-width="110" show-overflow-tooltip />
          <el-table-column label="账期" min-width="180" align="center">
            <template #default="scope">{{ (scope.row as DriverSettlementRecord).periodStart }} ~ {{ (scope.row as DriverSettlementRecord).periodEnd }}</template>
          </el-table-column>
          <el-table-column prop="tripCount" label="趟次" width="70" align="center" />
          <el-table-column label="应发(元)" width="130" align="right">
            <template #default="scope">
              <span class="amount-strong">{{ money(scope.row.totalAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="96" align="center">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.status)" size="small" effect="light">
                {{ SETTLEMENT_STATUS_LABELS[scope.row.status] ?? scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="生成时间" width="170" align="center">
            <template #default="scope">{{ (scope.row as DriverSettlementRecord).createTime ?? "-" }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right" align="center">
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
                type="danger"
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

    <!-- 生成结算单弹窗：分区引导（账期与司机 → 金额调整 → 应发预览） -->
    <el-dialog v-model="generateVisible" title="生成司机结算单" width="640px" append-to-body @closed="resetGenerate">
      <div class="gen-section-title">① 账期与司机</div>
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
            :disabled="!generateForm.period"
            :loading="candidatesLoading"
            :placeholder="generateForm.period ? '选择可结算司机' : '请先选择账期'"
            style="width: 100%"
          >
            <el-option
              v-for="d in candidates"
              :key="d.driverId"
              :value="d.driverId"
              :label="`${d.driverName ?? d.driverId}（可结算 ${d.tripCount} 趟）`"
            />
          </el-select>
          <el-alert
            v-if="generateForm.period && !candidatesLoading && !candidates.length"
            type="info"
            :closable="false"
            show-icon
            class="gen-empty-alert"
            title="该账期内暂无可结算趟次"
            description="只有账期内「已完成」且未被其他结算单引用的运输趟次才可结算；请调整账期范围，或先在作业模块完成趟次登记。"
          />
        </el-form-item>
      </el-form>

      <div class="gen-section-title">② 金额调整（元，选填调差项）</div>
      <el-form :model="generateForm" label-width="70px" @submit.prevent>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="底薪">
              <el-input-number v-model="generateForm.baseWage" :min="0" :precision="2" :step="100" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="补贴">
              <el-input-number v-model="generateForm.allowance" :min="0" :precision="2" :step="50" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="扣款">
              <el-input-number v-model="generateForm.deduction" :min="0" :precision="2" :step="50" class="w-full" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="generateForm.remark" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="选填，如油补/过路说明、扣款原因" />
        </el-form-item>
      </el-form>

      <!-- 应发实时预览：趟次费由后端按系统费率归集，前端只预览手工项 -->
      <div class="gen-section-title">③ 应发预览</div>
      <div class="gen-preview" :class="{ 'is-negative': manualSubtotal < 0 }">
        <div class="gen-preview__line">
          <span>底薪 +{{ money(generateForm.baseWage) }}</span>
          <span>补贴 +{{ money(generateForm.allowance) }}</span>
          <span>扣款 −{{ money(generateForm.deduction) }}</span>
          <span v-if="selectedCandidate" class="gen-preview__trip">趟次费：{{ selectedCandidate.tripCount }} 趟 × 系统费率</span>
          <span v-else class="gen-preview__trip">趟次费：选择司机后按系统费率归集</span>
        </div>
        <div class="gen-preview__total">
          预计应发
          <b>{{ money(manualSubtotal) }}</b>
          <span class="gen-preview__unit">元</span>
          <em>（不含趟次费，生成时自动计入）</em>
        </div>
        <div v-if="manualSubtotal < 0" class="gen-preview__warn">扣款已超过底薪 + 补贴，请核对金额</div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="generateVisible = false">取消</el-button>
          <el-button type="primary" :loading="generating" @click="handleGenerate">生成</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情抽屉（读操作居右，L1 = min(640px, 92%)） -->
    <el-drawer v-model="detailVisible" size="min(640px, 92%)" append-to-body>
      <template #header>
        <div class="detail-header">
          <span>结算单详情{{ detail ? ` · ${detail.settlement.settleNo}` : "" }}</span>
          <el-tag v-if="detail" :type="statusTagType(detail.settlement.status)" size="small" effect="light">
            {{ SETTLEMENT_STATUS_LABELS[detail.settlement.status] ?? detail.settlement.status }}
          </el-tag>
        </div>
      </template>
      <div v-if="detail" v-loading="detailLoading">
        <div class="detail-amount-card">
          <span class="detail-amount-card__label">应发金额</span>
          <span class="detail-amount-card__value">¥{{ money(detail.settlement.totalAmount) }}</span>
        </div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="司机">{{ detail.settlement.driverName ?? detail.settlement.driverId }}</el-descriptions-item>
          <el-descriptions-item label="完成趟次">{{ detail.settlement.tripCount }}</el-descriptions-item>
          <el-descriptions-item label="账期" :span="2">{{ detail.settlement.periodStart }} ~ {{ detail.settlement.periodEnd }}</el-descriptions-item>
          <el-descriptions-item label="底薪(元)">{{ money(detail.settlement.baseWage) }}</el-descriptions-item>
          <el-descriptions-item label="趟次费(元)">{{ money(detail.settlement.tripFee) }}</el-descriptions-item>
          <el-descriptions-item label="补贴(元)">{{ money(detail.settlement.allowance) }}</el-descriptions-item>
          <el-descriptions-item label="扣款(元)">{{ money(detail.settlement.deduction) }}</el-descriptions-item>
          <el-descriptions-item label="生成时间" :span="2">{{ detail.settlement.createTime ?? "-" }}</el-descriptions-item>
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
            <template #default="scope">{{ money(scope.row.amount) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { Plus } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import DriverSettlementAPI, {
  SETTLEMENT_STATUS_LABELS,
  SettlementStatus,
  type CandidateDriver,
  type DriverSettlementRecord,
  type SettlementDetailRecord,
} from "@/api/driver-settlement";

defineOptions({ name: "DriverSettlement" });

/** 统计快照分页上限：dev 量级内汇总准确；单量增长后应下沉后端聚合接口 */
const STATS_PAGE_SIZE = 500;

/** 查询与列表状态 */
const query = ref<{ page: number; pageSize: number; settleNo?: string; status?: number }>({
  page: 1,
  pageSize: 10,
});
const filterPeriod = ref<[string, string] | undefined>();

const loading = ref(false);
const loadError = ref(false);
const list = ref<DriverSettlementRecord[]>([]);
const total = ref(0);

async function loadList() {
  loading.value = true;
  loadError.value = false;
  try {
    const res = await DriverSettlementAPI.page({
      page: query.value.page,
      pageSize: query.value.pageSize,
      settleNo: query.value.settleNo?.trim() || undefined,
      status: query.value.status,
      periodStart: filterPeriod.value?.[0],
      periodEnd: filterPeriod.value?.[1],
    });
    list.value = res?.records ?? [];
    total.value = res?.total ?? 0;
  } catch {
    loadError.value = true;
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
  filterPeriod.value = undefined;
  loadList();
}

function money(value: number | null | undefined): string {
  if (value == null) return "-";
  return Number(value).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function statusTagType(status: number): "warning" | "success" | "info" | "primary" {
  if (status === SettlementStatus.PENDING) return "warning";
  if (status === SettlementStatus.AUDITED) return "primary";
  if (status === SettlementStatus.PAID) return "success";
  return "info";
}

/** ---------- 顶部统计卡 ---------- */
const stats = reactive({
  total: 0,
  totalAmount: 0,
  pendingCount: 0,
  pendingAmount: 0,
  auditedCount: 0,
  auditedAmount: 0,
  paidCount: 0,
  paidAmount: 0,
});

async function loadStats() {
  try {
    const res = await DriverSettlementAPI.page({ page: 1, pageSize: STATS_PAGE_SIZE });
    const rows = res?.records ?? [];
    const acc = { ...stats, total: rows.length, totalAmount: 0 };
    for (const r of rows) {
      const amount = Number(r.totalAmount ?? 0);
      acc.totalAmount += amount;
      if (r.status === SettlementStatus.PENDING) {
        acc.pendingCount += 1;
        acc.pendingAmount += amount;
      } else if (r.status === SettlementStatus.AUDITED) {
        acc.auditedCount += 1;
        acc.auditedAmount += amount;
      } else if (r.status === SettlementStatus.PAID) {
        acc.paidCount += 1;
        acc.paidAmount += amount;
      }
    }
    Object.assign(stats, acc);
  } catch {
    /* 统计失败不打断列表主流程，保留上次快照 */
  }
}

interface StatCard {
  key: string;
  label: string;
  value: string;
  hint: string;
  tone: "primary" | "warning" | "success" | "info";
  filterable?: boolean;
  status?: SettlementStatus;
}

const statCards = computed<StatCard[]>(() => [
  {
    key: "pending",
    label: "待审结算单",
    value: String(stats.pendingCount),
    hint: `待审金额 ¥${money(stats.pendingAmount)}`,
    tone: "warning",
    filterable: true,
    status: SettlementStatus.PENDING,
  },
  {
    key: "audited",
    label: "已审待发放",
    value: String(stats.auditedCount),
    hint: `待发放金额 ¥${money(stats.auditedAmount)}`,
    tone: "primary",
    filterable: true,
    status: SettlementStatus.AUDITED,
  },
  {
    key: "paid",
    label: "已发放",
    value: String(stats.paidCount),
    hint: `累计发放 ¥${money(stats.paidAmount)}`,
    tone: "success",
    filterable: true,
    status: SettlementStatus.PAID,
  },
  {
    key: "totalAmount",
    label: "应发总额",
    value: `¥${money(stats.totalAmount)}`,
    hint: `共 ${stats.total} 张结算单`,
    tone: "info",
  },
]);

/** 统计卡点击下钻：再次点击同卡取消筛选 */
function onStatClick(card: StatCard) {
  if (!card.filterable || card.status == null) return;
  query.value.status = query.value.status === card.status ? undefined : card.status;
  query.value.page = 1;
  loadList();
}

/** ---------- 生成结算单弹窗 ---------- */
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

/** 手工调差小计（底薪+补贴−扣款；趟次费生成时后端按费率计入，前端不预估费率） */
const manualSubtotal = computed(() => generateForm.baseWage + generateForm.allowance - generateForm.deduction);

/** 候选司机（账期内有可归集趟次；同司机账期重叠重复生成由后端幂等拒绝） */
const candidates = ref<CandidateDriver[]>([]);
const candidatesLoading = ref(false);

const selectedCandidate = computed(
  () => candidates.value.find((d) => d.driverId === generateForm.driverId) ?? null,
);

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
  resetGenerate();
  generateVisible.value = true;
}

function resetGenerate() {
  generateForm.period = undefined;
  generateForm.driverId = undefined;
  generateForm.baseWage = 0;
  generateForm.allowance = 0;
  generateForm.deduction = 0;
  generateForm.remark = "";
  candidates.value = [];
}

async function handleGenerate() {
  const valid = await generateFormRef.value?.validate().catch(() => false);
  if (!valid || !generateForm.period) return;
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
    loadStats();
  } finally {
    generating.value = false;
  }
}

/** ---------- 详情抽屉（读操作居右） ---------- */
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<{ settlement: DriverSettlementRecord; details: SettlementDetailRecord[] } | null>(null);

async function openDetail(row: DriverSettlementRecord) {
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = null;
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
    confirmButtonText: "通过",
    cancelButtonText: "取消",
  }).catch(() => Promise.reject(new Error("cancel")));
  await DriverSettlementAPI.audit(row.id, value?.trim() || undefined);
  ElMessage.success("已审核通过");
  loadList();
  loadStats();
}

/** 发放登记（1→2，线下打款后操作，不可回退） */
async function handlePay(row: DriverSettlementRecord) {
  await ElMessageBox.confirm(
    `确认已线下打款并登记发放？应发 ¥${money(row.totalAmount)}，登记后不可回退。`,
    `发放结算单 ${row.settleNo}`,
    { type: "warning", confirmButtonText: "确认发放", cancelButtonText: "取消" },
  );
  await DriverSettlementAPI.pay(row.id);
  ElMessage.success("已登记发放");
  loadList();
  loadStats();
}

onMounted(() => {
  loadList();
  loadStats();
});
</script>

<style scoped lang="scss">
.w-full {
  width: 100%;
}

/* ---------- 统计卡（参照 transport-board stage-card 范式） ---------- */
.stat-row {
  flex-shrink: 0;
}

.stat-card {
  padding: 12px 16px;
  background: var(--content-bg);
  border: 1px solid var(--card-border);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  transition:
    transform 0.15s ease,
    border-color 0.15s ease;

  &.is-clickable {
    cursor: pointer;

    &:hover {
      transform: translateY(-2px);
    }
  }

  &.is-active {
    border-color: var(--el-color-primary);

    .stat-card__label {
      color: var(--el-color-primary);
    }
  }

  &__label {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  &__value {
    margin-top: 6px;
    font-size: 24px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
    line-height: 1.2;
    color: var(--el-text-color-primary);

    &--warning {
      color: var(--el-color-warning);
    }

    &--primary {
      color: var(--el-color-primary);
    }

    &--success {
      color: var(--el-color-success);
    }

    &--info {
      color: var(--el-text-color-primary);
    }
  }

  &__hint {
    margin-top: 4px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

/* ---------- 展开行金额构成 ---------- */
.expand-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
  align-items: center;
  padding: 4px 16px;
  font-size: 13px;
  color: var(--el-text-color-regular);

  &__item b {
    margin-left: 4px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    color: var(--el-text-color-primary);
  }

  &__formula {
    margin-left: auto;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

/* ---------- 生成弹窗分区 ---------- */
.gen-section-title {
  margin: 16px 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);

  &:first-child {
    margin-top: 0;
  }
}

.gen-empty-alert {
  margin-top: 4px;
}

.gen-preview {
  padding: 12px 16px;
  background: var(--el-fill-color-light);
  border-radius: var(--card-radius);

  &__line {
    display: flex;
    flex-wrap: wrap;
    gap: 4px 16px;
    font-size: 13px;
    color: var(--el-text-color-regular);
  }

  &__trip {
    color: var(--el-text-color-secondary);
  }

  &__total {
    margin-top: 10px;
    font-size: 13px;
    color: var(--el-text-color-regular);

    b {
      margin-left: 4px;
      font-size: 22px;
      font-weight: 700;
      font-variant-numeric: tabular-nums;
      color: var(--el-color-primary);
    }

    .gen-preview__unit {
      margin-left: 2px;
    }

    em {
      margin-left: 8px;
      font-size: 12px;
      font-style: normal;
      color: var(--el-text-color-secondary);
    }
  }

  &.is-negative .gen-preview__total b {
    color: var(--el-color-danger);
  }

  &__warn {
    margin-top: 6px;
    font-size: 12px;
    color: var(--el-color-danger);
  }
}

/* ---------- 详情抽屉 ---------- */
.detail-header {
  display: flex;
  gap: 8px;
  align-items: center;
}

.detail-amount-card {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 12px;
  background: var(--el-fill-color-light);
  border-radius: var(--card-radius);

  &__label {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  &__value {
    font-size: 24px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
    color: var(--el-color-danger);
  }
}

.detail-section-title {
  margin: 16px 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.amount-strong {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--el-color-danger);
}
</style>
