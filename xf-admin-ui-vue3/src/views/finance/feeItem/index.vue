<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <el-form class="page-search" :model="query" inline>
        <el-form-item label="费用项" prop="feeCode">
          <el-select v-model="query.feeCode" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="(label, value) in FEE_CODE_LABELS" :key="value" :value="value" :label="label" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="停用" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="page-toolbar">
        <el-button v-hasPerm="'finance:fee-item:save'" type="primary" @click="openForm()">
          新增费用项
        </el-button>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="feeCode" label="编码" width="140">
            <template #default="scope">{{ scope.row.feeCode }}</template>
          </el-table-column>
          <el-table-column label="名称" min-width="120">
            <template #default="scope">{{ scope.row.feeName }}</template>
          </el-table-column>
          <el-table-column label="计费方式" width="110" align="center">
            <template #default="scope">
              {{ CALC_TYPE_LABELS[scope.row.calcType] ?? scope.row.calcType }}
            </template>
          </el-table-column>
          <el-table-column label="费率/金额" min-width="150" align="right">
            <template #default="scope">{{ calcValue(scope.row as FeeItemRecord) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="plain">
                {{ scope.row.status === 1 ? "启用" : "停用" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.remark || "-" }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right" align="center">
            <template #default="scope">
              <el-button
                v-hasPerm="'finance:fee-item:save'"
                link
                type="primary"
                @click="openForm(scope.row as FeeItemRecord)"
              >
                编辑
              </el-button>
              <el-button
                v-if="scope.row.status === 1"
                v-hasPerm="'finance:fee-item:disable'"
                link
                type="danger"
                @click="handleDisable(scope.row as FeeItemRecord)"
              >
                停用
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

    <!-- 新增/编辑弹窗（写操作居中弹窗 + append-to-body） -->
    <el-dialog
      v-model="formVisible"
      :title="form.id ? '编辑费用项' : '新增费用项'"
      width="480px"
      append-to-body
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="编码" prop="feeCode">
          <el-select v-model="form.feeCode" placeholder="选择费用项编码" :disabled="!!form.id" style="width: 100%">
            <el-option v-for="(label, value) in FEE_CODE_LABELS" :key="value" :value="value" :label="`${value}（${label}）`" />
          </el-select>
          <div class="form-tip">编码与成本报表（RPT-01）口径对齐，创建后不可改</div>
        </el-form-item>
        <el-form-item label="名称" prop="feeName">
          <el-input v-model="form.feeName" maxlength="64" />
        </el-form-item>
        <el-form-item label="计费方式" prop="calcType">
          <el-radio-group v-model="form.calcType">
            <el-radio :value="1">固定金额</el-radio>
            <el-radio :value="2">按运费比例</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.calcType === 1" label="金额(元)" prop="fixedAmount">
          <el-input-number v-model="form.fixedAmount" :min="0" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="form.calcType === 2" label="比例" prop="ratio">
          <el-input-number v-model="form.ratio" :min="0.0001" :max="1" :precision="4" :step="0.01" style="width: 100%" />
          <div class="form-tip">0.0500=运费的 5%</div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import FeeItemAPI, {
  CALC_TYPE_LABELS,
  FEE_CODE_LABELS,
  type FeeItemRecord,
} from "@/api/fee-item";

defineOptions({ name: "FeeItem" });

/** 查询与列表状态 */
const query = ref<{ page: number; pageSize: number; feeCode?: string; status?: number }>({
  page: 1,
  pageSize: 10,
});

const loading = ref(false);
const list = ref<FeeItemRecord[]>([]);
const total = ref(0);

async function loadList() {
  loading.value = true;
  try {
    const res = await FeeItemAPI.page({
      page: query.value.page,
      pageSize: query.value.pageSize,
      feeCode: query.value.feeCode || undefined,
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

function calcValue(row: FeeItemRecord): string {
  if (row.calcType === 1) return `${row.fixedAmount ?? 0} 元`;
  if (row.calcType === 2) {
    const pct = row.ratio != null ? Number(row.ratio) * 100 : 0;
    return `${pct}% / 运费`;
  }
  return "-";
}

/** 新增/编辑弹窗 */
const formVisible = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<Partial<FeeItemRecord>>({});

const rules: FormRules = {
  feeCode: [{ required: true, message: "请选择费用项编码", trigger: "change" }],
  feeName: [{ required: true, message: "名称不能为空", trigger: "blur" }],
  calcType: [{ required: true, message: "请选择计费方式", trigger: "change" }],
  fixedAmount: [{ required: true, message: "金额不能为空", trigger: "blur" }],
  ratio: [{ required: true, message: "比例不能为空", trigger: "blur" }],
};

function openForm(row?: FeeItemRecord) {
  Object.assign(form, row ?? { calcType: 1 });
  formVisible.value = true;
}

async function handleSave() {
  await formRef.value?.validate().catch(() => Promise.reject(new Error("validate")));
  saving.value = true;
  try {
    await FeeItemAPI.save({ ...form });
    ElMessage.success("保存成功");
    formVisible.value = false;
    loadList();
  } finally {
    saving.value = false;
  }
}

async function handleDisable(row: FeeItemRecord) {
  await ElMessageBox.confirm(`确认停用「${row.feeName}」？停用后自动生成与下拉选择不再包含该项。`, "停用确认", {
    type: "warning",
  });
  await FeeItemAPI.disable(row.id);
  ElMessage.success("已停用");
  loadList();
}

onMounted(loadList);
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}
</style>
