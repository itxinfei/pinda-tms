<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <el-form class="page-search" :model="query" inline>
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

      <div class="page-toolbar">
        <el-button v-hasPerm="'finance:invoice:apply'" type="primary" @click="openApply">申请开票</el-button>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column label="发票号" min-width="170" show-overflow-tooltip>
            <template #default="scope">{{ (scope.row as InvoiceRecord).invoiceNo || "-" }}</template>
          </el-table-column>
          <el-table-column prop="customerId" label="客户ID" min-width="140" show-overflow-tooltip />
          <el-table-column label="开票金额(元)" width="120" align="right">
            <template #default="scope">{{ formatMoney(scope.row.amount) }}</template>
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
          <el-table-column prop="taxNo" label="客户税号" min-width="150" show-overflow-tooltip>
            <template #default="scope">{{ (scope.row as InvoiceRecord).taxNo || "-" }}</template>
          </el-table-column>
          <el-table-column prop="mailAddress" label="邮寄地址" min-width="180" show-overflow-tooltip>
            <template #default="scope">{{ (scope.row as InvoiceRecord).mailAddress || "-" }}</template>
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

    <!-- 申请开票弹窗（写操作居中弹窗 + append-to-body） -->
    <el-dialog v-model="applyVisible" title="申请开票" width="520px" append-to-body>
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="90px">
        <el-form-item label="关联账单" prop="billId">
          <el-select v-model="applyForm.billId" filterable placeholder="选择已确认账单" style="width: 100%">
            <el-option
              v-for="b in confirmedBills"
              :key="b.id"
              :value="b.id"
              :label="`${b.billNo}（${b.customerId}，¥${formatMoney(b.totalAmount)}）`"
            />
          </el-select>
        </el-form-item>
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
      <div class="form-tip">
        开票金额与客户取自所选账单（不可修改）；同账单已有未作废发票时将拒绝重复申请。测试环境开具为自编发票号（TEST- 前缀），不接真实税控。
      </div>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="handleApply">申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
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

/** 查询与列表状态 */
const query = ref<{ page: number; pageSize: number; customerId?: string; status?: number }>({
  page: 1,
  pageSize: 10,
});

const loading = ref(false);
const list = ref<InvoiceRecord[]>([]);
const total = ref(0);

async function loadList() {
  loading.value = true;
  try {
    const res = await InvoiceAPI.page({
      page: query.value.page,
      pageSize: query.value.pageSize,
      customerId: query.value.customerId?.trim() || undefined,
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

function statusTagType(status: number): "warning" | "success" | "danger" | "info" | "primary" {
  if (status === InvoiceStatus.APPLIED) return "warning";
  if (status === InvoiceStatus.ISSUED || status === InvoiceStatus.MAILED) return "success";
  if (status === InvoiceStatus.VOIDED) return "danger";
  return "info";
}

/** 申请开票弹窗 */
const applyVisible = ref(false);
const applying = ref(false);
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

async function openApply() {
  applyForm.billId = undefined;
  applyForm.type = InvoiceType.GENERAL;
  applyForm.taxNo = "";
  applyForm.mailAddress = "";
  applyVisible.value = true;
  try {
    const res = await FreightBillAPI.page({ page: 1, pageSize: 50, status: FreightBillStatus.CONFIRMED });
    confirmedBills.value = res?.records ?? [];
  } catch {
    confirmedBills.value = [];
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
  } finally {
    applying.value = false;
  }
}

/** 开具（待申请/已申请 → 已开具） */
async function handleIssue(row: InvoiceRecord) {
  await ElMessageBox.confirm("确认开具该发票？测试环境将生成自编发票号（TEST- 前缀）。", "开具发票", { type: "warning" });
  await InvoiceAPI.issue(row.id);
  ElMessage.success("发票已开具");
  loadList();
}

/** 邮寄（已开具 → 已邮寄） */
async function handleMail(row: InvoiceRecord) {
  await ElMessageBox.confirm("确认标记该发票为已邮寄？", "邮寄发票", { type: "info" });
  await InvoiceAPI.mail(row.id);
  ElMessage.success("发票已标记邮寄");
  loadList();
}

/** 作废（→5，原因必填） */
async function handleVoid(row: InvoiceRecord) {
  const { value } = await ElMessageBox.prompt("作废原因（必填）", `作废发票 ${row.invoiceNo || row.id}`, {
    type: "warning",
    inputValidator: (v: string) => (v && v.trim() ? true : "作废原因不能为空"),
  });
  await InvoiceAPI.void(row.id, value.trim());
  ElMessage.success("发票已作废");
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
</style>
