<template>
  <div class="page-container">
    <!-- 顶部统计卡：全量口径前端汇总（dev 量级内准确；量大后应下沉后端聚合接口），生效/停用可点击下钻 -->
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
                <el-option :value="AgreementStatus.ACTIVE" label="生效" />
                <el-option :value="AgreementStatus.DISABLED" label="停用" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="page-toolbar__right">
          <el-button v-hasPerm="'finance:price-agreement:save'" type="primary" :icon="Plus" @click="openForm()">
            新增协议
          </el-button>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <template #empty>
            <el-empty v-if="loadError" description="加载失败">
              <el-button type="primary" @click="loadList">重试</el-button>
            </el-empty>
            <el-empty v-else description="暂无价格协议" />
          </template>
          <!-- 展开行：折扣 / 重量段 / 备注（主表瘦身） -->
          <el-table-column type="expand">
            <template #default="scope">
              <div class="expand-row">
                <span class="expand-row__item">折扣 <b>{{ formatDiscount(scope.row.discount) }}</b></span>
                <span class="expand-row__item">重量段(kg) <b>{{ weightRange(scope.row as PriceAgreementRecord) }}</b></span>
                <span class="expand-row__item">备注 <b>{{ (scope.row as PriceAgreementRecord).remark || "-" }}</b></span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="memberId" label="客户ID" min-width="140" show-overflow-tooltip />
          <el-table-column label="计价方式" width="100" align="center">
            <template #default="scope">{{ PRICE_TYPE_LABELS[scope.row.priceType] ?? scope.row.priceType }}</template>
          </el-table-column>
          <el-table-column label="单价(元)" width="110" align="right">
            <template #default="scope">
              <span class="amount-num">{{ money(scope.row.price) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="有效期" min-width="200" align="center">
            <template #default="scope">
              {{ (scope.row as PriceAgreementRecord).effectiveFrom }} ~
              {{ (scope.row as PriceAgreementRecord).effectiveTo ?? "长期" }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === AgreementStatus.ACTIVE ? 'success' : 'info'" effect="plain">
                {{ scope.row.status === AgreementStatus.ACTIVE ? "生效" : "停用" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right" align="center">
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
                v-if="scope.row.status === AgreementStatus.ACTIVE"
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

    <!-- 新增/编辑弹窗：分区引导（客户与计价 → 适用范围 → 计费口径预览） -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑价格协议' : '新增价格协议'" width="560px" append-to-body>
      <div class="gen-section-title">① 客户与计价</div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="客户ID" prop="memberId">
          <el-input v-model="form.memberId" placeholder="月结/企业客户 id（散客不适用协议价）" />
        </el-form-item>
        <el-form-item label="计价方式" prop="priceType">
          <el-select v-model="form.priceType" style="width: 100%">
            <el-option v-for="(label, value) in PRICE_TYPE_LABELS" :key="value" :value="Number(value)" :label="label" />
          </el-select>
        </el-form-item>
        <el-form-item label="单价(元)" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" class="w-full" />
        </el-form-item>
        <el-form-item label="折扣" prop="discount">
          <el-input-number v-model="form.discount" :min="0.01" :max="1" :precision="2" :step="0.05" class="w-full" />
          <div class="form-tip">0.90=9折，1.00=无折扣</div>
        </el-form-item>
      </el-form>

      <div class="gen-section-title">② 适用范围</div>
      <el-form :model="form" label-width="90px" @submit.prevent>
        <el-form-item label="重量段(kg)">
          <el-input-number v-model="form.weightMin" :min="0" :precision="2" placeholder="下限" controls-position="right" style="width: 150px" />
          <span class="range-separator">~</span>
          <el-input-number v-model="form.weightMax" :min="0" :precision="2" placeholder="上限(空=不封顶)" controls-position="right" style="width: 180px" />
        </el-form-item>
        <el-form-item label="生效日期" prop="effectiveFrom">
          <el-date-picker v-model="form.effectiveFrom" type="date" value-format="YYYY-MM-DD" class="w-full" />
        </el-form-item>
        <el-form-item label="失效日期" prop="effectiveTo">
          <el-date-picker v-model="form.effectiveTo" type="date" value-format="YYYY-MM-DD" placeholder="空=长期有效" class="w-full" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <!-- 计费口径实时预览 -->
      <div class="gen-section-title">③ 计费口径预览</div>
      <div class="gen-preview">
        <div class="gen-preview__line">
          <span>单价 ¥{{ money(form.price) }}/{{ PRICE_TYPE_LABELS[Number(form.priceType)] ?? "-" }}</span>
          <span>折扣 {{ formatDiscount(Number(form.discount ?? 1)) }}</span>
        </div>
        <div class="gen-preview__total">
          计费 = 数量 × 单价
          <b v-if="Number(form.discount ?? 1) < 1">× {{ form.discount }}</b>
          <span class="gen-preview__unit">元</span>
          <em>Drools 计费前优先命中协议价</em>
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
import PriceAgreementAPI, {
  AgreementStatus,
  PRICE_TYPE_LABELS,
  PriceType,
  type PriceAgreementRecord,
} from "@/api/price-agreement";

defineOptions({ name: "PriceAgreement" });

/** 统计快照分页上限：dev 量级内汇总准确；单量增长后应下沉后端聚合接口 */
const STATS_PAGE_SIZE = 500;

/** 查询与列表状态 */
const query = ref<{ page: number; pageSize: number; memberId?: string; status?: number }>({
  page: 1,
  pageSize: 10,
});

const loading = ref(false);
const loadError = ref(false);
const list = ref<PriceAgreementRecord[]>([]);
const total = ref(0);

async function loadList() {
  loading.value = true;
  loadError.value = false;
  try {
    const res = await PriceAgreementAPI.page({
      page: query.value.page,
      pageSize: query.value.pageSize,
      memberId: query.value.memberId?.trim() || undefined,
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

/** ---------- 顶部统计卡 ---------- */
const stats = reactive({
  total: 0,
  activeCount: 0,
  activeMembers: 0,
  disabledCount: 0,
  expiringCount: 0,
});

async function loadStats() {
  try {
    const res = await PriceAgreementAPI.page({ page: 1, pageSize: STATS_PAGE_SIZE });
    const rows = res?.records ?? [];
    const members = new Set<string>();
    let expiring = 0;
    const horizon = new Date();
    horizon.setDate(horizon.getDate() + 30);
    for (const r of rows) {
      if (r.status === AgreementStatus.ACTIVE) {
        members.add(r.memberId);
        if (r.effectiveTo && new Date(`${r.effectiveTo}T23:59:59`) <= horizon) expiring += 1;
      }
    }
    Object.assign(stats, {
      total: rows.length,
      activeCount: rows.filter((r) => r.status === AgreementStatus.ACTIVE).length,
      activeMembers: members.size,
      disabledCount: rows.filter((r) => r.status === AgreementStatus.DISABLED).length,
      expiringCount: expiring,
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
    key: "active",
    label: "生效协议",
    value: String(stats.activeCount),
    hint: `覆盖 ${stats.activeMembers} 位客户`,
    tone: "success",
    filterable: true,
    status: AgreementStatus.ACTIVE,
  },
  {
    key: "expiring",
    label: "30 天内到期",
    value: String(stats.expiringCount),
    hint: "到期后计费回落默认规则",
    tone: "warning",
  },
  {
    key: "disabled",
    label: "停用协议",
    value: String(stats.disabledCount),
    hint: "停用后不再命中协议价",
    tone: "info",
    filterable: true,
    status: AgreementStatus.DISABLED,
  },
  {
    key: "total",
    label: "协议总数",
    value: String(stats.total),
    hint: "含生效与停用",
    tone: "primary",
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
const form = reactive<Partial<PriceAgreementRecord>>({});

const rules: FormRules = {
  memberId: [{ required: true, message: "客户ID不能为空", trigger: "blur" }],
  priceType: [{ required: true, message: "请选择计价方式", trigger: "change" }],
  price: [{ required: true, message: "单价不能为空", trigger: "blur" }],
  discount: [{ required: true, message: "折扣不能为空", trigger: "blur" }],
  effectiveFrom: [{ required: true, message: "生效日期不能为空", trigger: "change" }],
};

function openForm(row?: PriceAgreementRecord) {
  // 先清空再赋值，避免上次编辑残留字段带入新增
  Object.keys(form).forEach((k) => delete (form as Record<string, unknown>)[k]);
  Object.assign(form, row ?? { priceType: PriceType.PER_WAYBILL, discount: 1 });
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
    loadStats();
  } finally {
    saving.value = false;
  }
}

async function handleDisable(row: PriceAgreementRecord) {
  await ElMessageBox.confirm(
    `确认停用客户 ${row.memberId} 的价格协议？停用后计费回落默认规则。`,
    "停用确认",
    { type: "warning", confirmButtonText: "确认停用", cancelButtonText: "取消" },
  );
  await PriceAgreementAPI.disable(row.id);
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

.range-separator {
  margin: 0 8px;
  color: var(--el-text-color-secondary);
}

.amount-num {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--el-text-color-primary);
}
</style>
