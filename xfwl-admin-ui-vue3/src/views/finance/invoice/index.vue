<template>
  <div class="page-container">
    <!-- 顶部统计卡：全量口径前端汇总（dev 量级内准确；量大后应下沉后端聚合接口，不含作废），三状态可点击下钻 -->
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
              <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
                <el-option v-for="(label, value) in INVOICE_STATUS_LABELS" :key="value" :value="Number(value)" :label="label" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="page-toolbar__right">
          <el-button v-hasPerm="'finance:invoice:apply'" type="primary" :icon="Plus" @click="openApply">
            申请开票
          </el-button>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <template #empty>
            <el-empty v-if="loadError" description="加载失败">
              <el-button type="primary" @click="loadList">重试</el-button>
            </el-empty>
            <el-empty v-else description="暂无发票记录" />
          </template>
          <!-- 展开行：税号 / 邮寄地址 / 作废原因 / 备注（主表瘦身） -->
          <el-table-column type="expand">
            <template #default="scope">
              <div class="expand-row">
                <span class="expand-row__item">税号 <b>{{ (scope.row as InvoiceRecord).taxNo || "-" }}</b></span>
                <span class="expand-row__item">邮寄地址 <b>{{ (scope.row as InvoiceRecord).mailAddress || "-" }}</b></span>
                <span v-if="scope.row.status === InvoiceStatus.VOIDED" class="expand-row__item">
                  作废原因 <b>{{ (scope.row as InvoiceRecord).voidReason || "-" }}</b>
                </span>
                <span class="expand-row__item">备注 <b>{{ (scope.row as InvoiceRecord).remark || "-" }}</b></span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="发票号" min-width="170" show-overflow-tooltip>
            <template #default="scope">{{ (scope.row as InvoiceRecord).invoiceNo || "-" }}</template>
          </el-table-column>
          <el-table-column prop="customerId" label="客户ID" min-width="140" show-overflow-tooltip />
          <el-table-column label="开票金额(元)" width="130" align="right">
            <template #default="scope">
              <span class="amount-strong">{{ money(scope.row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="票种" width="110" align="center">
            <template #default="scope">{{ INVOICE_TYPE_LABELS[scope.row.type] ?? scope.row.type }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.status)" effect="plain">
                {{ INVOICE_STATUS_LABELS[scope.row.status] ?? scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="invoiceTime" label="开具时间" width="170" align="center">
            <template #default="scope">{{ (scope.row as InvoiceRecord).invoiceTime ?? "-" }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right" align="center">
            <template #default="scope">
              <el-button
                v-if="scope.row.status === InvoiceStatus.APPLIED || scope.row.status === InvoiceStatus.REQUESTED"
                v-hasPerm="'finance:invoice:issue'"
                link
                type="success"
                @click="handleIssue(scope.row as InvoiceRecord)"
              >
                开具
              </el-button>
              <el-button
                v-if="scope.row.status === InvoiceStatus.ISSUED"
                v-hasPerm="'finance:invoice:mail'"
                link
                type="primary"
                @click="handleMail(scope.row as InvoiceRecord)"
              >
                邮寄
              </el-button>
              <el-button
                v-if="scope.row.status !== InvoiceStatus.VOIDED"
                v-hasPerm="'finance:invoice:void'"
                link
                type="danger"
                @click="handleVoid(scope.row as InvoiceRecord)"
              >
                作废
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

    <!-- 申请开票弹窗：分区引导（关联账单 → 发票信息 → 开票预览） -->
    <el-dialog v-model="applyVisible" title="申请开票" width="560px" append-to-body @closed="resetApply">
      <div class="gen-section-title">① 关联账单</div>
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="90px">
        <el-form-item label="账单" prop="billId">
          <el-select
            v-model="applyForm.billId"
            filterable
            :loading="billsLoading"
            placeholder="选择已确认账单"
            class="w-full"
          >
            <el-option
              v-for="b in confirmedBills"
              :key="b.id"
              :value="b.id"
              :label="`${b.billNo}（${b.customerId}，¥${money(b.totalAmount)}）`"
            />
          </el-select>
          <el-alert
            v-if="!billsLoading && !confirmedBills.length"
            type="info"
            :closable="false"
            show-icon
            class="gen-empty-alert"
            title="暂无可开票的已确认账单"
            description="请先在「运费账单」完成对账并确认，确认后的账单才能申请开票。"
          />
        </el-form-item>
      </el-form>

      <div class="gen-section-title">② 发票信息</div>
      <el-form :model="applyForm" label-width="90px" @submit.prevent>
        <el-form-item label="票种" prop="type">
          <el-radio-group v-model="applyForm.type">
            <el-radio v-for="(label, value) in INVOICE_TYPE_LABELS" :key="value" :value="Number(value)">{{ label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="客户税号" prop="taxNo">
          <el-input v-model="applyForm.taxNo" placeholder="选填，专票建议填写" />
        </el-form-item>
        <el-form-item label="邮寄地址" prop="mailAddress">
          <el-input v-model="applyForm.mailAddress" placeholder="选填" />
        </el-form-item>
      </el-form>

      <!-- 开票实时预览：金额与客户取自所选账单，不可修改 -->
      <div class="gen-section-title">③ 开票预览</div>
      <div class="gen-preview">
        <div class="gen-preview__line">
          <span v-if="selectedBill">客户 {{ selectedBill.customerId }} · 账单 {{ selectedBill.billNo }}</span>
          <span v-else>选择账单后显示开票信息</span>
        </div>
        <div class="gen-preview__total">
          开票金额
          <b>{{ selectedBill ? `¥${money(selectedBill.totalAmount)}` : "-" }}</b>
          <span class="gen-preview__unit">元</span>
          <em>取自账单，不可修改</em>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="applyVisible = false">取消</el-button>
          <el-button type="primary" :loading="applying" @click="handleApply">申请</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { Plus } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import InvoiceAPI, {
  INVOICE_STATUS_LABELS,
  INVOICE_TYPE_LABELS,
  InvoiceStatus,
  InvoiceType,
  type InvoiceRecord,
} from "@/api/invoice";
import FreightBillAPI, { FreightBillStatus, type FreightBillRecord } from "@/api/freight-bill";

defineOptions({ name: "Invoice" });

/** 统计快照分页上限：dev 量级内汇总准确；单量增长后应下沉后端聚合接口 */
const STATS_PAGE_SIZE = 500;

/** 查询与列表状态 */
const query = ref<{ page: number; pageSize: number; customerId?: string; status?: number }>({
  page: 1,
  pageSize: 10,
});

const loading = ref(false);
const loadError = ref(false);
const list = ref<InvoiceRecord[]>([]);
const total = ref(0);

async function loadList() {
  loading.value = true;
  loadError.value = false;
  try {
    const res = await InvoiceAPI.page({
      page: query.value.page,
      pageSize: query.value.pageSize,
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

function statusTagType(status: number): "warning" | "success" | "danger" | "info" | "primary" {
  if (status === InvoiceStatus.APPLIED || status === InvoiceStatus.REQUESTED) return "warning";
  if (status === InvoiceStatus.ISSUED || status === InvoiceStatus.MAILED) return "success";
  if (status === InvoiceStatus.VOIDED) return "danger";
  return "info";
}

/** ---------- 顶部统计卡（不含作废） ---------- */
const stats = reactive({
  total: 0,
  totalAmount: 0,
  appliedCount: 0,
  appliedAmount: 0,
  requestedCount: 0,
  requestedAmount: 0,
  issuedCount: 0,
  issuedAmount: 0,
});

async function loadStats() {
  try {
    const res = await InvoiceAPI.page({ page: 1, pageSize: STATS_PAGE_SIZE });
    const rows = res?.records ?? [];
    const valid = rows.filter((r) => r.status !== InvoiceStatus.VOIDED);
    const acc = { ...stats, total: valid.length, totalAmount: 0 };
    for (const r of valid) {
      const amount = Number(r.amount ?? 0);
      acc.totalAmount += amount;
      if (r.status === InvoiceStatus.APPLIED) {
        acc.appliedCount += 1;
        acc.appliedAmount += amount;
      } else if (r.status === InvoiceStatus.REQUESTED) {
        acc.requestedCount += 1;
        acc.requestedAmount += amount;
      } else if (r.status === InvoiceStatus.ISSUED) {
        acc.issuedCount += 1;
        acc.issuedAmount += amount;
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
    key: "applied",
    label: "待申请",
    value: String(stats.appliedCount),
    hint: `待申请金额 ¥${money(stats.appliedAmount)}`,
    tone: "warning",
    filterable: true,
    status: InvoiceStatus.APPLIED,
  },
  {
    key: "requested",
    label: "已申请待开具",
    value: String(stats.requestedCount),
    hint: `待开具金额 ¥${money(stats.requestedAmount)}`,
    tone: "primary",
    filterable: true,
    status: InvoiceStatus.REQUESTED,
  },
  {
    key: "issued",
    label: "已开具待邮寄",
    value: String(stats.issuedCount),
    hint: `开票金额 ¥${money(stats.issuedAmount)}`,
    tone: "success",
    filterable: true,
    status: InvoiceStatus.ISSUED,
  },
  {
    key: "totalAmount",
    label: "开票总额",
    value: `¥${money(stats.totalAmount)}`,
    hint: `共 ${stats.total} 张（不含作废）`,
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

/** ---------- 申请开票弹窗 ---------- */
const applyVisible = ref(false);
const applying = ref(false);
const billsLoading = ref(false);
const applyFormRef = ref<FormInstance>();
const applyForm = reactive<{ billId: string | undefined; type: InvoiceType; taxNo: string; mailAddress: string }>({
  billId: undefined,
  type: InvoiceType.GENERAL,
  taxNo: "",
  mailAddress: "",
});

const applyRules: FormRules = {
  billId: [{ required: true, message: "请选择关联账单", trigger: "change" }],
};

/** 已确认账单（status=1，供申请开票选择；同账单重复申请由后端幂等拒绝） */
const confirmedBills = ref<FreightBillRecord[]>([]);

const selectedBill = computed(
  () => confirmedBills.value.find((b) => String(b.id) === String(applyForm.billId)) ?? null,
);

function resetApply() {
  applyForm.billId = undefined;
  applyForm.type = InvoiceType.GENERAL;
  applyForm.taxNo = "";
  applyForm.mailAddress = "";
  confirmedBills.value = [];
}

async function openApply() {
  resetApply();
  applyVisible.value = true;
  billsLoading.value = true;
  try {
    const res = await FreightBillAPI.page({ page: 1, pageSize: 50, status: FreightBillStatus.CONFIRMED });
    confirmedBills.value = res?.records ?? [];
  } catch {
    confirmedBills.value = [];
  } finally {
    billsLoading.value = false;
  }
}

async function handleApply() {
  await applyFormRef.value?.validate().catch(() => Promise.reject(new Error("validate")));
  applying.value = true;
  try {
    await InvoiceAPI.apply({
      billId: applyForm.billId!,
      type: applyForm.type,
      taxNo: applyForm.taxNo.trim() || undefined,
      mailAddress: applyForm.mailAddress.trim() || undefined,
    });
    ElMessage.success("开票申请已提交");
    applyVisible.value = false;
    loadList();
    loadStats();
  } finally {
    applying.value = false;
  }
}

/** 开具（待申请/已申请 → 已开具） */
async function handleIssue(row: InvoiceRecord) {
  await ElMessageBox.confirm(
    `确认开具该发票（¥${money(row.amount)}）？测试环境将生成自编发票号（TEST- 前缀）。`,
    "开具发票",
    { type: "warning", confirmButtonText: "确认开具", cancelButtonText: "取消" },
  );
  await InvoiceAPI.issue(row.id);
  ElMessage.success("发票已开具");
  loadList();
  loadStats();
}

/** 邮寄（已开具 → 已邮寄） */
async function handleMail(row: InvoiceRecord) {
  await ElMessageBox.confirm("确认标记该发票为已邮寄？", "邮寄发票", {
    type: "info",
    confirmButtonText: "确认邮寄",
    cancelButtonText: "取消",
  });
  await InvoiceAPI.mail(row.id);
  ElMessage.success("发票已标记邮寄");
  loadList();
  loadStats();
}

/** 作废（→5，原因必填，不可逆） */
async function handleVoid(row: InvoiceRecord) {
  const { value } = await ElMessageBox.prompt("作废原因（必填）", `作废发票 ${row.invoiceNo || row.id}`, {
    type: "warning",
    inputValidator: (v: string) => (v && v.trim() ? true : "作废原因不能为空"),
    confirmButtonText: "确认作废",
    cancelButtonText: "取消",
  });
  await InvoiceAPI.void(row.id, value.trim());
  ElMessage.success("发票已作废");
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

/* ---------- 弹窗分区与预览 ---------- */
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

  &__total {
    margin-top: 10px;
    font-size: 13px;
    color: var(--el-text-color-regular);

    b {
      margin-left: 4px;
      font-size: 18px;
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
}

.amount-strong {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--el-color-danger);
}
</style>
