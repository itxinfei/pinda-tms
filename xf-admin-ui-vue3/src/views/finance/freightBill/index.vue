<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <el-form class="page-search" :model="query" inline>
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

      <div class="page-toolbar">
        <el-button v-hasPerm="'finance:freight-bill:generate'" type="primary" @click="openGenerate">
          生成账单
        </el-button>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="billNo" label="账单号" min-width="170" show-overflow-tooltip />
          <el-table-column prop="customerId" label="客户ID" min-width="140" show-overflow-tooltip />
          <el-table-column label="账期" min-width="190" align="center">
            <template #default="scope">{{ periodRange(scope.row as FreightBillRecord) }}</template>
          </el-table-column>
          <el-table-column prop="orderCount" label="结算单数" width="90" align="right" />
          <el-table-column label="应收总额(元)" width="120" align="right">
            <template #default="scope">{{ formatMoney(scope.row.totalAmount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.status)" effect="plain">
                {{ FREIGHT_BILL_STATUS_LABELS[scope.row.status] ?? scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" align="center">
            <template #default="scope">{{ scope.row.createTime ?? "-" }}</template>
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

    <!-- 生成账单弹窗（写操作居中弹窗 + append-to-body） -->
    <el-dialog v-model="generateVisible" title="生成账单" width="480px" append-to-body>
      <el-form ref="generateFormRef" :model="generateForm" :rules="generateRules" label-width="90px">
        <el-form-item label="客户ID" prop="customerId">
          <el-input v-model="generateForm.customerId" placeholder="客户 id（对齐结算单 settle_object_id）" />
        </el-form-item>
        <el-form-item label="账期" prop="period">
          <el-date-picker
            v-model="generateForm.period"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="~"
            start-placeholder="账期开始"
            end-placeholder="账期结束"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <div class="form-tip">
        归集该客户账期内「应收且未归账」的结算单生成账单；同客户同账期已有未核销账单时将拒绝重复生成。
      </div>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="handleGenerate">生成</el-button>
      </template>
    </el-dialog>

    <!-- 对账详情抽屉（读操作居右抽屉 + append-to-body） -->
    <el-drawer v-model="reconcileVisible" title="对账详情" size="720px" append-to-body>
      <div v-loading="reconcileLoading">
        <template v-if="view">
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
            :title="`存在差异：账单总额比结算单应收合计${view.diff > 0 ? '多' : '少'} ${formatMoney(Math.abs(view.diff))} 元，请核实`"
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
            <el-descriptions-item label="结算单数">{{ view.bill.orderCount }}</el-descriptions-item>
            <el-descriptions-item label="账单总额(元)">{{ formatMoney(view.bill.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="应收合计(元)">{{ formatMoney(view.sumReceivable) }}</el-descriptions-item>
            <el-descriptions-item label="差异(元)">
              <span :style="{ color: view.diff === 0 ? 'var(--el-color-success)' : 'var(--el-color-danger)' }">
                {{ formatMoney(view.diff) }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ view.bill.remark || "-" }}</el-descriptions-item>
          </el-descriptions>

          <div class="drawer-section-title">归集的应收结算单</div>
          <el-table :data="view.settlements" border size="small" max-height="420">
            <el-table-column prop="settlementNo" label="结算单号" min-width="170" show-overflow-tooltip />
            <el-table-column prop="orderId" label="订单ID" min-width="120" show-overflow-tooltip />
            <el-table-column label="应收金额(元)" width="120" align="right">
              <template #default="scope">{{ formatMoney(scope.row.receivableAmount) }}</template>
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
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import FreightBillAPI, {
  FREIGHT_BILL_STATUS_LABELS,
  SETTLEMENT_STATUS_LABELS,
  FreightBillStatus,
  type FreightBillRecord,
  type ReconcileView,
} from "@/api/freight-bill";

defineOptions({ name: "FreightBill" });

/** 查询与列表状态 */
const query = ref<{ page: number; pageSize: number; billNo?: string; customerId?: string; status?: number }>({
  page: 1,
  pageSize: 10,
});

const loading = ref(false);
const list = ref<FreightBillRecord[]>([]);
const total = ref(0);

async function loadList() {
  loading.value = true;
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

function periodRange(row: FreightBillRecord): string {
  if (!row.periodStart && !row.periodEnd) return "-";
  return `${row.periodStart ?? "?"} ~ ${row.periodEnd ?? "?"}`;
}

function statusTagType(status: number): "warning" | "success" | "info" {
  if (status === FreightBillStatus.PENDING) return "warning";
  if (status === FreightBillStatus.CONFIRMED) return "success";
  return "info";
}

/** 生成账单弹窗 */
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
  generateForm.customerId = "";
  generateForm.period = null;
  generateVisible.value = true;
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
  } finally {
    generating.value = false;
  }
}

/** 对账详情抽屉 */
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
    { type: "warning" }
  );
  await FreightBillAPI.confirm(row.id);
  ElMessage.success("账单已确认");
  loadList();
}

/** 核销（1→2） */
async function handleWriteOff(row: FreightBillRecord) {
  await ElMessageBox.confirm(
    `核销账单 ${row.billNo}（应收 ${formatMoney(row.totalAmount)} 元）？核销后不可恢复，其下结算单将联动置为「已结算」。`,
    "核销账单",
    { type: "warning" }
  );
  await FreightBillAPI.writeOff(row.id);
  ElMessage.success("账单已核销");
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

.drawer-section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 16px 0 8px;
}
</style>
