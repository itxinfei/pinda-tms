<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 订单信息 + 新增 -->
      <div class="page-toolbar">
        <div class="page-toolbar__left">
          <span class="cargo-order-no">订单号：<b>{{ orderNo || orderId || "-" }}</b></span>
        </div>
        <div class="page-toolbar__right">
          <el-button @click="goBack">返回</el-button>
          <el-button type="primary" :icon="Plus" @click="openCreate">新增货物</el-button>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="name" label="货物名称" min-width="160" show-overflow-tooltip />
          <el-table-column label="货物类型" min-width="120">
            <template #default="scope">{{ scope.row.goodsType?.name || "-" }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="90" align="right" />
          <el-table-column prop="unit" label="单位" width="90" align="center" />
          <el-table-column label="重量(kg)" min-width="110" align="right">
            <template #default="scope">{{ scope.row.weight ?? "-" }}</template>
          </el-table-column>
          <el-table-column label="体积(方)" min-width="110" align="right">
            <template #default="scope">{{ scope.row.volume ?? "-" }}</template>
          </el-table-column>
          <el-table-column label="货值(元)" min-width="110" align="right">
            <template #default="scope">{{ scope.row.cargoValue ?? "-" }}</template>
          </el-table-column>
          <el-table-column prop="cargoBarcode" label="条码" min-width="140" show-overflow-tooltip />
          <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.remark || "-" }}</template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="120" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openEdit(scope.row as OrderCargoRecord)">编辑</el-button>
              <el-button type="danger" link size="small" @click="handleRemove(scope.row as OrderCargoRecord)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="formTitle" width="560px" @closed="resetForm">
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
          <el-input-number v-model="form.quantity" :min="0" :precision="0" :value-on-clear="null" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="如：件/箱/吨" maxlength="16" style="width: 100%" />
        </el-form-item>
        <el-form-item label="重量(kg)" prop="weight">
          <el-input-number v-model="form.weight" :min="0" :precision="2" :value-on-clear="null" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="体积(方)" prop="volume">
          <el-input-number v-model="form.volume" :min="0" :precision="2" :value-on-clear="null" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="货值(元)" prop="cargoValue">
          <el-input-number v-model="form.cargoValue" :min="0" :precision="2" :value-on-clear="null" controls-position="right" style="width: 100%" />
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
import { ref, reactive, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Plus } from "@element-plus/icons-vue";
import type { FormInstance, FormRules } from "element-plus";
import CargoAPI, { type OrderCargoRecord } from "@/api/cargo";
import CommonSimpleAPI, { type GoodsTypeSimple } from "@/api/common-simple";

const route = useRoute();
const router = useRouter();

const orderId = computed(() => (route.query.orderId as string) || "");
const orderNo = computed(() => (route.query.orderNo as string) || "");

/** 列表状态 */
const loading = ref(false);
const list = ref<OrderCargoRecord[]>([]);

async function loadList() {
  if (!orderId.value) {
    ElMessage.warning("缺少订单参数，无法加载货物列表");
    return;
  }
  loading.value = true;
  try {
    list.value = await CargoAPI.list(orderId.value);
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.back();
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

/** 新增/编辑弹窗 */
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
      await CargoAPI.save(orderId.value, buildPayload());
      ElMessage.success("新增成功");
    }
    formVisible.value = false;
    loadList();
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
      { type: "warning", confirmButtonText: "删除", cancelButtonText: "取消" },
    );
  } catch {
    return;
  }
  try {
    await CargoAPI.remove(row.id!);
    ElMessage.success("已删除");
    loadList();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "删除失败");
  }
}

onMounted(() => {
  loadGoodsTypes();
  loadList();
});
</script>

<style scoped>
.cargo-order-no {
  font-size: 14px;
  color: var(--el-text-color-regular);
}
.cargo-order-no b {
  color: var(--el-text-color-primary);
  font-weight: 600;
}
</style>
