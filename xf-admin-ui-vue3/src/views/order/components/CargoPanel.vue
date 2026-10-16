<template>
  <div class="cargo-panel">
    <!-- 汇总条 -->
    <div class="cargo-panel__summary">
      <span class="cargo-panel__summary-item">
        共 <b>{{ list.length }}</b> 项货物
      </span>
      <el-divider direction="vertical" />
      <span class="cargo-panel__summary-item">总重量：<b>{{ formatNumber(totalWeight) }}</b> kg</span>
      <el-divider direction="vertical" />
      <span class="cargo-panel__summary-item">总体积：<b>{{ formatNumber(totalVolume) }}</b> 方</span>
      <el-divider direction="vertical" />
      <span class="cargo-panel__summary-item">总货值：<b>{{ formatNumber(totalCargoValue) }}</b> 元</span>
      <div class="cargo-panel__summary-right">
        <slot name="actions">
          <el-button
            v-if="editable"
            v-hasPerm="['order:update']"
            type="primary"
            size="small"
            :icon="Plus"
            @click="openCreate"
          >
            新增货物
          </el-button>
        </slot>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" border size="small" empty-text="该订单暂无货物明细">
      <el-table-column type="index" label="#" width="45" align="center" />
      <el-table-column prop="name" label="货物名称" min-width="140" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.name || "-" }}</template>
      </el-table-column>
      <el-table-column label="货物类型" min-width="110" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.goodsType?.name || "-" }}</template>
      </el-table-column>
      <el-table-column label="数量" width="90" align="right">
        <template #default="scope">
          {{ scope.row.quantity ?? "-" }}
          <span v-if="scope.row.unit" class="cargo-panel__unit">{{ scope.row.unit }}</span>
        </template>
      </el-table-column>
      <el-table-column label="重量(kg)" width="100" align="right">
        <template #default="scope">{{ formatNumber(scope.row.weight) }}</template>
      </el-table-column>
      <el-table-column label="体积(方)" width="100" align="right">
        <template #default="scope">{{ formatNumber(scope.row.volume) }}</template>
      </el-table-column>
      <el-table-column label="货值(元)" width="110" align="right">
        <template #default="scope">{{ formatNumber(scope.row.cargoValue) }}</template>
      </el-table-column>
      <el-table-column prop="cargoBarcode" label="条码" min-width="120" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.cargoBarcode || "-" }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.remark || "-" }}</template>
      </el-table-column>
      <el-table-column v-if="editable" label="操作" fixed="right" width="110" align="center">
        <template #default="scope">
          <el-button
            v-hasPerm="['order:update']"
            type="primary"
            link
            size="small"
            @click="openEdit(scope.row as OrderCargoRecord)"
          >
            编辑
          </el-button>
          <el-button
            v-hasPerm="['order:update']"
            type="danger"
            link
            size="small"
            @click="handleRemove(scope.row as OrderCargoRecord)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="formTitle" width="560px" append-to-body @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="货物名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入货物名称" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="货物类型" prop="goodsTypeId">
          <el-select v-model="form.goodsTypeId" placeholder="请选择货物类型" filterable style="width: 100%">
            <el-option v-for="gt in goodsTypeOptions" :key="gt.id" :label="gt.name" :value="gt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number
            v-model="form.quantity"
            :min="0"
            :precision="0"
            :value-on-clear="null"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="如：件/箱/吨" maxlength="16" style="width: 100%" />
        </el-form-item>
        <el-form-item label="重量(kg)" prop="weight">
          <el-input-number
            v-model="form.weight"
            :min="0"
            :precision="2"
            :value-on-clear="null"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="体积(方)" prop="volume">
          <el-input-number
            v-model="form.volume"
            :min="0"
            :precision="2"
            :value-on-clear="null"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="货值(元)" prop="cargoValue">
          <el-input-number
            v-model="form.cargoValue"
            :min="0"
            :precision="2"
            :value-on-clear="null"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="条码" prop="cargoBarcode">
          <el-input v-model="form.cargoBarcode" placeholder="选填" maxlength="64" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { Plus } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import CargoAPI, { type OrderCargoRecord } from "@/api/cargo";
import CommonSimpleAPI, { type GoodsTypeSimple } from "@/api/common-simple";
import { formatNumber } from "@/utils/format";

/**
 * 订单货物明细面板（订单详情抽屉内联 + 货物维护页共用）
 *
 * <p>只读模式（editable=false）下隐藏增删改与操作列，用于已终态订单只读查看。</p>
 */
const props = withDefaults(
  defineProps<{
    /** 订单 id（雪花 Long 字符串形态） */
    orderId: string;
    /** 是否允许增删改 */
    editable?: boolean;
  }>(),
  { editable: true }
);

/** 货物变更后通知父组件（订单金额/距离随货物重量变化，需刷新详情） */
const emit = defineEmits<{ (e: "change"): void }>();

const loading = ref(false);
const list = ref<OrderCargoRecord[]>([]);

const totalWeight = computed(() =>
  list.value.reduce((sum, item) => sum + Number(item.weight ?? 0), 0)
);
const totalVolume = computed(() => list.value.reduce((sum, item) => sum + Number(item.volume ?? 0), 0));
const totalCargoValue = computed(() =>
  list.value.reduce((sum, item) => sum + Number(item.cargoValue ?? 0), 0)
);

async function loadList() {
  if (!props.orderId) {
    list.value = [];
    return;
  }
  loading.value = true;
  try {
    list.value = await CargoAPI.list(props.orderId);
  } finally {
    loading.value = false;
  }
}

/** 货物类型下拉 */
const goodsTypeOptions = ref<GoodsTypeSimple[]>([]);
async function loadGoodsTypes() {
  try {
    goodsTypeOptions.value = await CommonSimpleAPI.goodsTypeOptions();
  } catch {
    goodsTypeOptions.value = [];
  }
}

// 新增/编辑弹窗
const formVisible = ref(false);
const saving = ref(false);
const editingId = ref<string>();
const formRef = ref<FormInstance>();

interface CargoForm {
  name: string;
  goodsTypeId?: string;
  quantity: number | null;
  unit: string;
  weight: number | null;
  volume: number | null;
  cargoValue: number | null;
  cargoBarcode: string;
  remark: string;
}

const createDefaultForm = (): CargoForm => ({
  name: "",
  goodsTypeId: undefined,
  quantity: null,
  unit: "",
  weight: null,
  volume: null,
  cargoValue: null,
  cargoBarcode: "",
  remark: "",
});

const form = reactive<CargoForm>(createDefaultForm());

const rules: FormRules<CargoForm> = {
  name: [{ required: true, message: "请输入货物名称", trigger: "blur" }],
};

const formTitle = computed(() => (editingId.value ? "编辑货物" : "新增货物"));

function openCreate() {
  editingId.value = undefined;
  Object.assign(form, createDefaultForm());
  formVisible.value = true;
}

function openEdit(row: OrderCargoRecord) {
  editingId.value = row.id;
  Object.assign(form, createDefaultForm(), {
    name: row.name ?? "",
    goodsTypeId: row.goodsType?.id,
    quantity: row.quantity ?? null,
    unit: row.unit ?? "",
    weight: row.weight ?? null,
    volume: row.volume ?? null,
    cargoValue: row.cargoValue ?? null,
    cargoBarcode: row.cargoBarcode ?? "",
    remark: row.remark ?? "",
  });
  formVisible.value = true;
}

/** 提交体只取表单字段，货物类型转 {id} 关联 */
function buildPayload(): Partial<OrderCargoRecord> {
  return {
    name: form.name.trim(),
    goodsType: form.goodsTypeId ? { id: form.goodsTypeId } : null,
    quantity: form.quantity ?? null,
    unit: form.unit.trim() || null,
    weight: form.weight ?? null,
    volume: form.volume ?? null,
    cargoValue: form.cargoValue ?? null,
    cargoBarcode: form.cargoBarcode.trim() || null,
    remark: form.remark.trim() || null,
  };
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  saving.value = true;
  try {
    if (editingId.value) {
      await CargoAPI.update(editingId.value, buildPayload());
      ElMessage.success("更新成功");
    } else {
      await CargoAPI.save(props.orderId, buildPayload());
      ElMessage.success("新增成功");
    }
    formVisible.value = false;
    await loadList();
    emit("change");
  } finally {
    saving.value = false;
  }
}

function resetForm() {
  formRef.value?.resetFields();
  Object.assign(form, createDefaultForm());
  editingId.value = undefined;
}

async function handleRemove(row: OrderCargoRecord) {
  try {
    await ElMessageBox.confirm(
      `确认删除货物「${row.name || "未命名"}」？删除后不可恢复。`,
      "删除确认",
      { type: "warning", confirmButtonText: "确认删除", cancelButtonText: "取消" }
    );
  } catch {
    return;
  }
  await CargoAPI.remove(String(row.id));
  ElMessage.success("删除成功");
  await loadList();
  emit("change");
}

watch(() => props.orderId, loadList);
onMounted(() => {
  loadList();
  loadGoodsTypes();
});

defineExpose({ reload: loadList });
</script>

<style scoped lang="scss">
.cargo-panel__summary {
  display: flex;
  align-items: center;
  gap: 4px;
  padding-bottom: 10px;
  font-size: 13px;
  color: var(--el-text-color-regular);

  &-right {
    margin-left: auto;
  }

  &-item b {
    color: var(--el-text-color-primary);
  }
}

.cargo-panel__unit {
  margin-left: 2px;
  color: var(--el-text-color-secondary);
}
</style>