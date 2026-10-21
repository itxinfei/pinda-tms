<template>
  <div class="page-container">
    <!-- 顶部统计卡：全量口径前端汇总（dev 量级内准确；量大后应下沉后端聚合接口），三状态可点击下钻 -->
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
            <el-form-item label="账单号" prop="billNo">
              <el-input
                v-model="query.billNo"
                placeholder="账单号（FB 开头）"
                clearable
                style="width: 180px"
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item label="客户ID" prop="customerId">
              <el-input
                v-model="query.customerId"
                placeholder="客户 id（散客=GUEST）"
                clearable
                style="width: 180px"
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
                <el-option v-for="(label, value) in FREIGHT_BILL_STATUS_LABELS" :key="value" :value="Number(value)" :label="label" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="page-toolbar__right">
          <el-button v-hasPerm="'finance:freight-bill:generate'" type="primary" :icon="Plus" @click="openGenerate">
            生成账单
          </el-button>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <template #empty>
            <el-empty v-if="loadError" description="加载失败">
              <el-button type="primary" @click="loadList">重试</el-button>
            </el-empty>
            <el-empty v-else description="暂无运费账单" />
          </template>
          <!-- 展开行：备注 / 创建时间（主表瘦身） -->
          <el-table-column type="expand">
            <template #default="scope">
              <div class="expand-row">
                <span class="expand-row__item">备注 <b>{{ (scope.row as FreightBillRecord).remark || "-" }}</b></span>
                <span class="expand-row__item">创建时间 <b>{{ (scope.row as FreightBillRecord).createTime ?? "-" }}</b></span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="billNo" label="账单号" min-width="170" show-overflow-tooltip />
          <el-table-column prop="customerId" label="客户ID" min-width="140" show-overflow-tooltip />
          <el-table-column label="账期" min-width="190" align="center">
            <template #default="scope">{{ periodRange(scope.row as FreightBillRecord) }}</template>
          </el-table-column>
          <el-table-column prop="orderCount" label="结算单数" width="90" align="right" />
          <el-table-column label="应收总额(元)" width="130" align="right">
            <template #default="scope">
              <span class="amount-strong">{{ money(scope.row.totalAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.status)" effect="plain">
                {{ FREIGHT_BILL_STATUS_LABELS[scope.row.status] ?? scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right" align="center">
            <template #default="scope">
              <el-button
                v-hasPerm="'finance:freight-bill:detail'"
                link
                type="primary"
                @click="openReconcile(scope.row as FreightBillRecord)"
              >
                对账
              </el-button>
              <el-button
                v-if="scope.row.status === FreightBillStatus.PENDING"
                v-hasPerm="'finance:freight-bill:confirm'"
                link
                type="success"
                @click="handleConfirm(scope.row as FreightBillRecord)"
              >
                确认
              </el-button>
              <el-button
                v-if="scope.row.status === FreightBillStatus.CONFIRMED"
                v-hasPerm="'finance:freight-bill:write-off'"
                link
                type="danger"
                @click="handleWriteOff(scope.row as FreightBillRecord)"
              >
                核销
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

    <!-- 生成账单弹窗：分区引导（客户与账期 → 归集规则说明） -->
    <el-dialog v-model="generateVisible" title="生成账单" width="520px" append-to-body @closed="resetGenerate">
      <div class="gen-section-title">① 客户与账期</div>
      <el-form ref="generateFormRef" :model="generateForm" :rules="generateRules" label-width="90px">
        <el-form-item label="客户ID" prop="customerId">
          <el-input v-model="generateForm.customerId" placeholder="客户 id（对齐结算单 settle_object_id）" />
        </el-form-item>
        <el-form-item label="账期" prop="period">
          <el-date-picker
            v-model="generateForm.period"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="账期开始"
            end-placeholder="账期结束"
            class="w-full"
          />
        </el-form-item>
      </el-form>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="gen-empty-alert"
        title="归集规则"
        description="归集该客户账期内「应收且未归账」的结算单生成账单；同客户同账期已有未核销账单时将拒绝重复生成。"
      />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="generateVisible = false">取消</el-button>
          <el-button type="primary" :loading="generating" @click="handleGenerate">生成</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 对账详情抽屉（读操作居右，标题带单号 + 顶部金额高亮卡） -->
    <el-drawer v-model="reconcileVisible" size="min(720px, 92%)" append-to-body>
      <template #header>
        <div class="detail-header">
          <span>对账详情{{ view ? ` · ${view.bill.billNo}` : "" }}</span>
          <el-tag v-if="view" :type="statusTagType(view.bill.status)" size="small" effect="light">
            {{ FREIGHT_BILL_STATUS_LABELS[view.bill.status] ?? view.bill.status }}
          </el-tag>
        </div>
      </template>
      <div v-loading="reconcileLoading">
        <template v-if="view">
          <div class="detail-amount-card">
            <span class="detail-amount-card__label">应收总额</span>
            <span class="detail-amount-card__value">¥{{ money(view.bill.totalAmount) }}</span>
            <span class="detail-amount-card__sub">
              结算单 {{ view.bill.orderCount }} 笔 · 应收合计 ¥{{ money(view.sumReceivable) }}
            </span>
          </div>
          <el-alert
            v-if="view.diff === 0"
            type="success"
            :closable="false"
            show-icon
            title="账实相符：账单总额与结算单应收合计一致"
            style="margin-bottom: 12px"
          />
          <el-alert
            v-else
            type="warning"
            :closable="false"
            show-icon
            :title="`存在差异：账单总额比结算单应收合计${view.diff > 0 ? '多' : '少'} ${money(Math.abs(view.diff))} 元，请核实`"
            style="margin-bottom: 12px"
          />

          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="账单号">{{ view.bill.billNo }}</el-descriptions-item>
            <el-descriptions-item label="客户ID">{{ view.bill.customerId }}</el-descriptions-item>
            <el-descriptions-item label="账期">{{ periodRange(view.bill) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTagType(view.bill.status)" effect="plain">
                {{ FREIGHT_BILL_STATUS_LABELS[view.bill.status] ?? view.bill.status }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="账单总额(元)">{{ money(view.bill.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="应收合计(元)">{{ money(view.sumReceivable) }}</el-descriptions-item>
            <el-descriptions-item label="差异(元)">
              <span :style="{ color: view.diff === 0 ? 'var(--el-color-success)' : 'var(--el-color-danger)' }">
                {{ money(view.diff) }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ view.bill.createTime ?? "-" }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ view.bill.remark || "-" }}</el-descriptions-item>
          </el-descriptions>

          <div class="detail-section-title">归集的应收结算单（{{ view.settlements.length }} 笔）</div>
          <el-table :data="view.settlements" border size="small" max-height="420">
            <el-table-column prop="settlementNo" label="结算单号" min-width="170" show-overflow-tooltip />
            <el-table-column prop="orderId" label="订单ID" min-width="120" show-overflow-tooltip />
            <el-table-column label="应收金额(元)" width="120" align="right">
              <template #default="scope">{{ money(scope.row.receivableAmount) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="scope">
                {{ SETTLEMENT_STATUS_LABELS[scope.row.status] ?? scope.row.status }}
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170" align="center">
              <template #default="scope">{{ scope.row.createTime ?? "-" }}</template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { Plus } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import FreightBillAPI, {
  FREIGHT_BILL_STATUS_LABELS,
  SETTLEMENT_STATUS_LABELS,
  FreightBillStatus,
  type FreightBillRecord,
  type ReconcileView,
} from "@/api/freight-bill";

defineOptions({ name: "FreightBill" });

/** 统计快照分页上限：dev 量级内汇总准确；单量增长后应下沉后端聚合接口 */
const STATS_PAGE_SIZE = 500;

/** 查询与列表状态 */
const query = ref<{ page: number; pageSize: number; billNo?: string; customerId?: string; status?: number }>({
  page: 1,
  pageSize: 10,
});

const loading = ref(false);
const loadError = ref(false);
const list = ref<FreightBillRecord[]>([]);
const total = ref(0);

async function loadList() {
  loading.value = true;
  loadError.value = false;
  try {
    const res = await FreightBillAPI.page({
      page: query.value.page,
      pageSize: query.value.pageSize,
      billNo: query.value.billNo?.trim() || undefined,
      customerId: query.value.customerId?.trim() || undefined,
      status: query.value.status,
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
  loadList();
}

function money(value: number | null | undefined): string {
  if (value == null) return "-";
  return Number(value).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function periodRange(row: FreightBillRecord): string {
  if (!row.periodStart && !row.periodEnd) return "-";
  return `${row.periodStart ?? "?"} ~ ${row.periodEnd ?? "?"}`;
}

function statusTagType(status: number): "warning" | "success" | "info" {
  if (status === FreightBillStatus.PENDING) return "warning";
  if (status === FreightBillStatus.CONFIRMED) return "success";
  return "info";
}

/** ---------- 顶部统计卡 ---------- */
const stats = reactive({
  total: 0,
  totalAmount: 0,
  pendingCount: 0,
  pendingAmount: 0,
  confirmedCount: 0,
  confirmedAmount: 0,
  writtenOffCount: 0,
  writtenOffAmount: 0,
});

async function loadStats() {
  try {
    const res = await FreightBillAPI.page({ page: 1, pageSize: STATS_PAGE_SIZE });
    const rows = res?.records ?? [];
    const acc = { ...stats, total: rows.length, totalAmount: 0 };
    for (const r of rows) {
      const amount = Number(r.totalAmount ?? 0);
      acc.totalAmount += amount;
      if (r.status === FreightBillStatus.PENDING) {
        acc.pendingCount += 1;
        acc.pendingAmount += amount;
      } else if (r.status === FreightBillStatus.CONFIRMED) {
        acc.confirmedCount += 1;
        acc.confirmedAmount += amount;
      } else if (r.status === FreightBillStatus.WRITTEN_OFF) {
        acc.writtenOffCount += 1;
        acc.writtenOffAmount += amount;
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
  status?: number;
}

const statCards = computed<StatCard[]>(() => [
  {
    key: "pending",
    label: "待确认账单",
    value: String(stats.pendingCount),
    hint: `待确认金额 ¥${money(stats.pendingAmount)}`,
    tone: "warning",
    filterable: true,
    status: FreightBillStatus.PENDING,
  },
  {
    key: "confirmed",
    label: "已确认待核销",
    value: String(stats.confirmedCount),
    hint: `待核销金额 ¥${money(stats.confirmedAmount)}`,
    tone: "primary",
    filterable: true,
    status: FreightBillStatus.CONFIRMED,
  },
  {
    key: "writtenOff",
    label: "已核销",
    value: String(stats.writtenOffCount),
    hint: `累计核销 ¥${money(stats.writtenOffAmount)}`,
    tone: "success",
    filterable: true,
    status: FreightBillStatus.WRITTEN_OFF,
  },
  {
    key: "totalAmount",
    label: "应收总额",
    value: `¥${money(stats.totalAmount)}`,
    hint: `共 ${stats.total} 张账单`,
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

/** ---------- 生成账单弹窗 ---------- */
const generateVisible = ref(false);
const generating = ref(false);
const generateFormRef = ref<FormInstance>();
const generateForm = reactive<{ customerId: string; period: [string, string] | null }>({
  customerId: "",
  period: null,
});

const generateRules: FormRules = {
  customerId: [{ required: true, message: "客户ID不能为空", trigger: "blur" }],
  period: [{ required: true, message: "请选择账期起止", trigger: "change" }],
};

function openGenerate() {
  resetGenerate();
  generateVisible.value = true;
}

function resetGenerate() {
  generateForm.customerId = "";
  generateForm.period = null;
}

async function handleGenerate() {
  await generateFormRef.value?.validate().catch(() => Promise.reject(new Error("validate")));
  generating.value = true;
  try {
    const bill = await FreightBillAPI.generate(
      generateForm.customerId.trim(),
      generateForm.period![0],
      generateForm.period![1]
    );
    ElMessage.success(`账单已生成：${(bill as FreightBillRecord | undefined)?.billNo ?? "成功"}`);
    generateVisible.value = false;
    loadList();
    loadStats();
  } finally {
    generating.value = false;
  }
}

/** ---------- 对账详情抽屉（读操作居右） ---------- */
const reconcileVisible = ref(false);
const reconcileLoading = ref(false);
const view = ref<ReconcileView | null>(null);

async function openReconcile(row: FreightBillRecord) {
  reconcileVisible.value = true;
  reconcileLoading.value = true;
  view.value = null;
  try {
    view.value = await FreightBillAPI.reconcile(row.id);
  } finally {
    reconcileLoading.value = false;
  }
}

/** 确认（0→1） */
async function handleConfirm(row: FreightBillRecord) {
  await ElMessageBox.confirm(
    `确认账单 ${row.billNo}？确认后其下 ${row.orderCount} 笔结算单将联动置为「已对账」。`,
    "确认账单",
    { type: "warning", confirmButtonText: "确认账单", cancelButtonText: "取消" }
  );
  await FreightBillAPI.confirm(row.id);
  ElMessage.success("账单已确认");
  loadList();
  loadStats();
}

/** 核销（1→2，不可逆） */
async function handleWriteOff(row: FreightBillRecord) {
  await ElMessageBox.confirm(
    `核销账单 ${row.billNo}（应收 ¥${money(row.totalAmount)} 元）？核销后不可恢复，其下结算单将联动置为「已结算」。`,
    "核销账单",
    { type: "warning", confirmButtonText: "确认核销", cancelButtonText: "取消" }
  );
  await FreightBillAPI.writeOff(row.id);
  ElMessage.success("账单已核销");
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

/* ---------- 展开行 ---------- */
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
    color: var(--el-text-color-primary);
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

/* ---------- 对账抽屉 ---------- */
.detail-header {
  display: flex;
  gap: 8px;
  align-items: center;
}

.detail-amount-card {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px 12px;
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

  &__sub {
    font-size: 12px;
    color: var(--el-text-color-secondary);
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
