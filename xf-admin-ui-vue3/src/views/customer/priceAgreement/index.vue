<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <el-form class="page-search" :model="query" inline>
        <el-form-item label="客户ID" prop="memberId">
          <el-input
            v-model="query.memberId"
            placeholder="客户 id（月结/企业客户）"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option :value="1" label="生效" />
            <el-option :value="0" label="停用" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="page-toolbar">
        <el-button v-hasPerm="'finance:price-agreement:save'" type="primary" @click="openForm()">
          新增协议
        </el-button>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="memberId" label="客户ID" min-width="140" show-overflow-tooltip />
          <el-table-column label="计价方式" width="90" align="center">
            <template #default="scope">
              {{ PRICE_TYPE_LABELS[scope.row.priceType] ?? scope.row.priceType }}
            </template>
          </el-table-column>
          <el-table-column prop="price" label="单价(元)" width="100" align="right" />
          <el-table-column label="折扣" width="80" align="right">
            <template #default="scope">{{ formatDiscount(scope.row.discount) }}</template>
          </el-table-column>
          <el-table-column label="重量段(kg)" min-width="130" align="center">
            <template #default="scope">{{ weightRange(scope.row as PriceAgreementRecord) }}</template>
          </el-table-column>
          <el-table-column prop="effectiveFrom" label="生效日期" width="110" align="center" />
          <el-table-column label="失效日期" width="110" align="center">
            <template #default="scope">{{ scope.row.effectiveTo ?? "长期" }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="plain">
                {{ scope.row.status === 1 ? "生效" : "停用" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.remark || "-" }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right" align="center">
            <template #default="scope">
              <el-button
                v-hasPerm="'finance:price-agreement:save'"
                link
                type="primary"
                @click="openForm(scope.row as PriceAgreementRecord)"
              >
                编辑
              </el-button>
              <el-button
                v-if="scope.row.status === 1"
                v-hasPerm="'finance:price-agreement:disable'"
                link
                type="danger"
                @click="handleDisable(scope.row as PriceAgreementRecord)"
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
      :title="form.id ? '编辑价格协议' : '新增价格协议'"
      width="520px"
      append-to-body
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="客户ID" prop="memberId">
          <el-input v-model="form.memberId" placeholder="月结/企业客户 id（散客不适用协议价）" />
        </el-form-item>
        <el-form-item label="计价方式" prop="priceType">
          <el-select v-model="form.priceType" style="width: 100%">
            <el-option v-for="(label, value) in PRICE_TYPE_LABELS" :key="value" :value="value" :label="label" />
          </el-select>
        </el-form-item>
        <el-form-item label="单价(元)" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="折扣" prop="discount">
          <el-input-number v-model="form.discount" :min="0.01" :max="1" :precision="2" :step="0.05" style="width: 100%" />
          <div class="form-tip">0.90=9折，1.00=无折扣</div>
        </el-form-item>
        <el-form-item label="重量段(kg)">
          <el-input-number v-model="form.weightMin" :min="0" :precision="2" placeholder="下限" controls-position="right" style="width: 140px" />
          <span class="range-separator">~</span>
          <el-input-number v-model="form.weightMax" :min="0" :precision="2" placeholder="上限(空=不封顶)" controls-position="right" style="width: 170px" />
        </el-form-item>
        <el-form-item label="生效日期" prop="effectiveFrom">
          <el-date-picker v-model="form.effectiveFrom" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="失效日期" prop="effectiveTo">
          <el-date-picker v-model="form.effectiveTo" type="date" value-format="YYYY-MM-DD" placeholder="空=长期有效" style="width: 100%" />
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
import PriceAgreementAPI, { PRICE_TYPE_LABELS, type PriceAgreementRecord } from "@/api/price-agreement";

defineOptions({ name: "PriceAgreement" });

/** 查询与列表状态 */
const query = ref<{ page: number; pageSize: number; memberId?: string; status?: number }>({
  page: 1,
  pageSize: 10,
});

const loading = ref(false);
const list = ref<PriceAgreementRecord[]>([]);
const total = ref(0);

async function loadList() {
  loading.value = true;
  try {
    const res = await PriceAgreementAPI.page({
      page: query.value.page,
      pageSize: query.value.pageSize,
      memberId: query.value.memberId?.trim() || undefined,
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

function formatDiscount(discount: number): string {
  if (discount == null) return "-";
  return Number(discount) === 1 ? "无" : `${Number(discount) * 10}折`;
}

function weightRange(row: PriceAgreementRecord): string {
  if (row.weightMin == null && row.weightMax == null) return "不限";
  const min = row.weightMin ?? 0;
  const max = row.weightMax != null ? `≤${row.weightMax}` : "以上";
  return `${min}~${max}`;
}

/** 新增/编辑弹窗 */
const formVisible = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<Partial<PriceAgreementRecord>>({});

const rules: FormRules = {
  memberId: [{ required: true, message: "客户ID不能为空", trigger: "blur" }],
  priceType: [{ required: true, message: "请选择计价方式", trigger: "change" }],
  price: [{ required: true, message: "单价不能为空", trigger: "blur" }],
  discount: [{ required: true, message: "折扣不能为空", trigger: "blur" }],
  effectiveFrom: [{ required: true, message: "生效日期不能为空", trigger: "change" }],
};

function openForm(row?: PriceAgreementRecord) {
  Object.assign(form, row ?? { priceType: 1, discount: 1 });
  formVisible.value = true;
}

async function handleSave() {
  await formRef.value?.validate().catch(() => Promise.reject(new Error("validate")));
  saving.value = true;
  try {
    await PriceAgreementAPI.save({ ...form });
    ElMessage.success("保存成功");
    formVisible.value = false;
    loadList();
  } finally {
    saving.value = false;
  }
}

async function handleDisable(row: PriceAgreementRecord) {
  await ElMessageBox.confirm(`确认停用该价格协议？停用后计费回落默认规则。`, "停用确认", {
    type: "warning",
  });
  await PriceAgreementAPI.disable(row.id);
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

.range-separator {
  margin: 0 8px;
  color: var(--el-text-color-secondary);
}
</style>
