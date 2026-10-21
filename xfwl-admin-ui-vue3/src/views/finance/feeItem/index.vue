<template>
  <div class="page-container">
    <!-- 顶部统计卡：全量口径前端汇总（dev 量级内准确；量大后应下沉后端聚合接口），启用/停用可点击下钻 -->
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
            <el-form-item label="费用项" prop="feeCode">
              <el-select v-model="query.feeCode" placeholder="全部" clearable style="width: 160px">
                <el-option v-for="(label, value) in FEE_CODE_LABELS" :key="value" :value="value" :label="label" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
                <el-option :value="FeeItemStatus.ENABLED" label="启用" />
                <el-option :value="FeeItemStatus.DISABLED" label="停用" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="page-toolbar__right">
          <el-button v-hasPerm="'finance:fee-item:save'" type="primary" :icon="Plus" @click="openForm()">
            新增费用项
          </el-button>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <template #empty>
            <el-empty v-if="loadError" description="加载失败">
              <el-button type="primary" @click="loadList">重试</el-button>
            </el-empty>
            <el-empty v-else description="暂无费用项配置" />
          </template>
          <!-- 展开行：备注 / 创建时间（主表瘦身） -->
          <el-table-column type="expand">
            <template #default="scope">
              <div class="expand-row">
                <span class="expand-row__item">备注 <b>{{ (scope.row as FeeItemRecord).remark || "-" }}</b></span>
                <span class="expand-row__item">创建时间 <b>{{ (scope.row as FeeItemRecord).createTime ?? "-" }}</b></span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="feeCode" label="编码" width="140" />
          <el-table-column label="名称" min-width="120" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.feeName }}</template>
          </el-table-column>
          <el-table-column label="计费方式" width="120" align="center">
            <template #default="scope">
              {{ CALC_TYPE_LABELS[scope.row.calcType] ?? scope.row.calcType }}
            </template>
          </el-table-column>
          <el-table-column label="费率/金额" min-width="150" align="right">
            <template #default="scope">
              <span class="amount-num">{{ calcValue(scope.row as FeeItemRecord) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === FeeItemStatus.ENABLED ? 'success' : 'info'" effect="plain">
                {{ scope.row.status === FeeItemStatus.ENABLED ? "启用" : "停用" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right" align="center">
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
                v-if="scope.row.status === FeeItemStatus.ENABLED"
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

    <!-- 新增/编辑弹窗：分区引导（编码与名称 → 计费规则 → 附加费预览） -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑费用项' : '新增费用项'" width="520px" append-to-body>
      <div class="gen-section-title">① 编码与名称</div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="编码" prop="feeCode">
          <el-select v-model="form.feeCode" placeholder="选择费用项编码" :disabled="!!form.id" class="w-full">
            <el-option v-for="(label, value) in FEE_CODE_LABELS" :key="value" :value="value" :label="`${value}（${label}）`" />
          </el-select>
          <div class="form-tip">编码与成本报表（RPT-01）口径对齐，创建后不可改</div>
        </el-form-item>
        <el-form-item label="名称" prop="feeName">
          <el-input v-model="form.feeName" maxlength="64" />
        </el-form-item>
      </el-form>

      <div class="gen-section-title">② 计费规则</div>
      <el-form :model="form" label-width="90px" @submit.prevent>
        <el-form-item prop="calcType">
          <el-radio-group v-model="form.calcType">
            <el-radio :value="FeeCalcType.FIXED">固定金额</el-radio>
            <el-radio :value="FeeCalcType.RATIO">按运费比例</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.calcType === FeeCalcType.FIXED" label="金额(元)" prop="fixedAmount">
          <el-input-number v-model="form.fixedAmount" :min="0" :precision="2" :step="10" class="w-full" />
        </el-form-item>
        <el-form-item v-if="form.calcType === FeeCalcType.RATIO" label="比例" prop="ratio">
          <el-input-number v-model="form.ratio" :min="0.0001" :max="1" :precision="4" :step="0.01" class="w-full" />
          <div class="form-tip">0.0500=运费的 5%</div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <!-- 附加费实时预览 -->
      <div class="gen-section-title">③ 附加费预览</div>
      <div class="gen-preview">
        <div class="gen-preview__line">
          <span v-if="form.calcType === FeeCalcType.FIXED">生成订单时按固定金额计提</span>
          <span v-else>生成订单时按运费金额比例计提</span>
        </div>
        <div class="gen-preview__total">
          附加费 =
          <b v-if="form.calcType === FeeCalcType.FIXED">¥{{ money(form.fixedAmount) }}</b>
          <template v-else>
            运费 × <b>{{ ratioPct }}</b>
          </template>
          <span class="gen-preview__unit">元</span>
          <em>停用后不再自动生成</em>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { Plus } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import FeeItemAPI, {
  CALC_TYPE_LABELS,
  FEE_CODE_LABELS,
  FeeCalcType,
  FeeItemStatus,
  type FeeItemRecord,
} from "@/api/fee-item";

defineOptions({ name: "FeeItem" });

/** 统计快照分页上限：dev 量级内汇总准确；单量增长后应下沉后端聚合接口 */
const STATS_PAGE_SIZE = 500;

/** 查询与列表状态 */
const query = ref<{ page: number; pageSize: number; feeCode?: string; status?: number }>({
  page: 1,
  pageSize: 10,
});

const loading = ref(false);
const loadError = ref(false);
const list = ref<FeeItemRecord[]>([]);
const total = ref(0);

async function loadList() {
  loading.value = true;
  loadError.value = false;
  try {
    const res = await FeeItemAPI.page({
      page: query.value.page,
      pageSize: query.value.pageSize,
      feeCode: query.value.feeCode || undefined,
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

function calcValue(row: FeeItemRecord): string {
  if (row.calcType === FeeCalcType.FIXED) return `${row.fixedAmount ?? 0} 元`;
  if (row.calcType === FeeCalcType.RATIO) {
    const pct = row.ratio != null ? Number(row.ratio) * 100 : 0;
    return `${pct}% / 运费`;
  }
  return "-";
}

/** ---------- 顶部统计卡 ---------- */
const stats = reactive({
  total: 0,
  enabled: 0,
  disabled: 0,
  fixed: 0,
  ratio: 0,
});

async function loadStats() {
  try {
    const res = await FeeItemAPI.page({ page: 1, pageSize: STATS_PAGE_SIZE });
    const rows = res?.records ?? [];
    Object.assign(stats, {
      total: rows.length,
      enabled: rows.filter((r) => r.status === FeeItemStatus.ENABLED).length,
      disabled: rows.filter((r) => r.status === FeeItemStatus.DISABLED).length,
      fixed: rows.filter((r) => r.calcType === FeeCalcType.FIXED).length,
      ratio: rows.filter((r) => r.calcType === FeeCalcType.RATIO).length,
    });
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
    key: "enabled",
    label: "启用费用项",
    value: String(stats.enabled),
    hint: `共 ${stats.total} 项 · 自动计入附加费`,
    tone: "success",
    filterable: true,
    status: FeeItemStatus.ENABLED,
  },
  {
    key: "fixed",
    label: "固定金额计费",
    value: String(stats.fixed),
    hint: "生成订单时按固定金额计提",
    tone: "primary",
  },
  {
    key: "ratio",
    label: "比例计费",
    value: String(stats.ratio),
    hint: "按运费金额比例计提",
    tone: "warning",
  },
  {
    key: "disabled",
    label: "停用费用项",
    value: String(stats.disabled),
    hint: "停用后不再自动生成",
    tone: "info",
    filterable: true,
    status: FeeItemStatus.DISABLED,
  },
]);

/** 统计卡点击下钻：再次点击同卡取消筛选 */
function onStatClick(card: StatCard) {
  if (!card.filterable || card.status == null) return;
  query.value.status = query.value.status === card.status ? undefined : card.status;
  query.value.page = 1;
  loadList();
}

/** ---------- 新增/编辑弹窗 ---------- */
const formVisible = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<Partial<FeeItemRecord>>({});

/** 比例展示（预览用） */
const ratioPct = computed(() => (form.ratio != null ? `${Number(form.ratio) * 100}%` : "-"));

const rules: FormRules = {
  feeCode: [{ required: true, message: "请选择费用项编码", trigger: "change" }],
  feeName: [{ required: true, message: "名称不能为空", trigger: "blur" }],
  calcType: [{ required: true, message: "请选择计费方式", trigger: "change" }],
  fixedAmount: [{ required: true, message: "金额不能为空", trigger: "blur" }],
  ratio: [{ required: true, message: "比例不能为空", trigger: "blur" }],
};

function openForm(row?: FeeItemRecord) {
  // 先清空再赋值，避免上次编辑残留字段带入新增
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k]);
  Object.assign(form, row ?? { calcType: FeeCalcType.FIXED });
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
    loadStats();
  } finally {
    saving.value = false;
  }
}

async function handleDisable(row: FeeItemRecord) {
  await ElMessageBox.confirm(`确认停用「${row.feeName}」？停用后自动生成与下拉选择不再包含该项。`, "停用确认", {
    type: "warning",
    confirmButtonText: "确认停用",
    cancelButtonText: "取消",
  });
  await FeeItemAPI.disable(row.id);
  ElMessage.success("已停用");
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

.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}

.amount-num {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--el-text-color-primary);
}
</style>
