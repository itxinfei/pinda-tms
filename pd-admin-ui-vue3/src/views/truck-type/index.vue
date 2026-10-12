<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 + 新增 -->
      <div class="page-toolbar">
        <div class="page-toolbar__left">
          <el-form :model="query" inline>
            <el-form-item label="名称" prop="name">
              <el-input v-model="query.name" placeholder="车辆类型名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px">
                <el-option label="启用" :value="CommonStatus.ENABLED" />
                <el-option label="停用" :value="CommonStatus.DISABLED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="page-toolbar__right">
          <el-button type="primary" :icon="Plus" @click="openCreate">新增车辆类型</el-button>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
          <el-table-column label="准载重(t)" min-width="100" align="right">
            <template #default="scope">{{ formatNumber(scope.row.allowableLoad, 2) }}</template>
          </el-table-column>
          <el-table-column label="准体积(m³)" min-width="110" align="right">
            <template #default="scope">{{ formatNumber(scope.row.allowableVolume, 2) }}</template>
          </el-table-column>
          <el-table-column label="尺寸 长×宽×高(m)" min-width="180" align="center">
            <template #default="scope">{{ formatDimension(scope.row as TruckTypeRecord) }}</template>
          </el-table-column>
          <el-table-column label="可运货物类型" min-width="220">
            <template #default="scope">
              <template v-if="(scope.row.goodsTypes ?? []).length">
                <el-tag
                  v-for="goods in scope.row.goodsTypes.slice(0, GOODS_TAG_COLLAPSE_COUNT)"
                  :key="goods.id"
                  size="small"
                  type="info"
                  class="truck-type__goods-tag"
                >
                  {{ goods.name }}
                </el-tag>
                <!-- 关联类型过多时折叠为 +N，hover 查看全部，避免撑高行高 -->
                <el-tooltip
                  v-if="scope.row.goodsTypes.length > GOODS_TAG_COLLAPSE_COUNT"
                  :content="(scope.row as TruckTypeRecord).goodsTypes!.slice(GOODS_TAG_COLLAPSE_COUNT).map((g) => g.name).join('、')"
                  placement="top"
                >
                  <el-tag size="small" type="info" class="truck-type__goods-tag">
                    +{{ scope.row.goodsTypes.length - GOODS_TAG_COLLAPSE_COUNT }}
                  </el-tag>
                </el-tooltip>
              </template>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" min-width="90" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === CommonStatus.ENABLED ? 'success' : 'info'" size="small">
                {{ scope.row.status === CommonStatus.ENABLED ? "启用" : "停用" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="120" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openEdit(scope.row as TruckTypeRecord)">编辑</el-button>
              <el-button
                v-if="scope.row.status === CommonStatus.ENABLED"
                type="danger"
                link
                size="small"
                @click="handleDisable(scope.row as TruckTypeRecord)"
                >停用</el-button
              >
              <el-button v-else type="success" link size="small" @click="handleEnable(scope.row as TruckTypeRecord)">启用</el-button>
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
          @size-change="handleSizeChange"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="formTitle" width="520px" @closed="resetForm">
      <el-form ref="formRef" v-loading="detailLoading" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入车辆类型名称" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="准载重量(t)" prop="allowableLoad">
          <el-input-number
            v-model="form.allowableLoad"
            :min="0"
            :max="9999"
            :precision="2"
            :value-on-clear="null"
            controls-position="right"
            placeholder="选填"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="准载体积(m³)" prop="allowableVolume">
          <el-input-number
            v-model="form.allowableVolume"
            :min="0"
            :max="9999"
            :precision="2"
            :value-on-clear="null"
            controls-position="right"
            placeholder="选填"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="车厢尺寸(m)">
          <div class="truck-type__dimension">
            <el-input-number
              v-model="form.measureLong"
              :min="0"
              :max="99"
              :precision="2"
              :value-on-clear="null"
              controls-position="right"
              placeholder="长"
              class="truck-type__dimension-item"
            />
            <el-input-number
              v-model="form.measureWidth"
              :min="0"
              :max="99"
              :precision="2"
              :value-on-clear="null"
              controls-position="right"
              placeholder="宽"
              class="truck-type__dimension-item"
            />
            <el-input-number
              v-model="form.measureHigh"
              :min="0"
              :max="99"
              :precision="2"
              :value-on-clear="null"
              controls-position="right"
              placeholder="高"
              class="truck-type__dimension-item"
            />
          </div>
        </el-form-item>
        <el-form-item label="可运货物类型" prop="goodsTypeIds">
          <el-select
            v-model="form.goodsTypeIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            clearable
            filterable
            placeholder="选择该车型允许运输的货物类型（可多选）"
            style="width: 100%"
          >
            <el-option v-for="item in goodsTypeOptions" :key="item.id" :value="item.id" :label="item.name" />
          </el-select>
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
import { Plus } from "@element-plus/icons-vue";
import type { FormInstance, FormRules } from "element-plus";
import TruckTypeAPI from "@/api/truck-type";
import type { TruckTypeRecord, TruckTypePageQuery } from "@/api/truck-type";
import CommonSimpleAPI from "@/api/common-simple";
import type { GoodsTypeSimple } from "@/api/common-simple";
import { CommonStatus } from "@/enums";
import { formatNumber } from "@/utils/format";

/** 表格中货物类型标签的直接展示上限，超出折叠为 +N */
const GOODS_TAG_COLLAPSE_COUNT = 2;

/** 货物类型下拉（仅启用项） */
const goodsTypeOptions = ref<GoodsTypeSimple[]>([]);

/** 查询与列表状态 */
const loading = ref(false);
const list = ref<TruckTypeRecord[]>([]);
const total = ref(0);
const query = ref<TruckTypePageQuery>({ page: 1, pageSize: 10 });

/** 组装查询参数：剔除空字符串/空值，仅提交有效过滤条件 */
function buildParams(): TruckTypePageQuery {
  const { page, pageSize, ...filters } = query.value;
  const params: TruckTypePageQuery = { page, pageSize };
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== "" && value !== null && value !== undefined) {
      (params as unknown as Record<string, unknown>)[key] = value;
    }
  });
  return params;
}

async function loadList() {
  loading.value = true;
  try {
    const res = await TruckTypeAPI.page(buildParams());
    // 统一剔除 BFF 富化缺失产生的 null，保证列表标签/折叠 +N/tooltip 渲染安全
    list.value = (res?.items ?? []).map((item) => ({ ...item, goodsTypes: validGoodsTypes(item.goodsTypes) }));
    total.value = res?.counts ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.value.page = 1;
  loadList();
}

/** 切换每页条数后必须回第 1 页，否则高页码会请求空页 */
function handleSizeChange() {
  query.value.page = 1;
  loadList();
}

function handleReset() {
  query.value = { page: 1, pageSize: 10 };
  loadList();
}

/** 尺寸列格式化：任一维度有值即展示，全部为空显示 - */
function formatDimension(row: TruckTypeRecord): string {
  const dims = [row.measureLong, row.measureWidth, row.measureHigh];
  if (dims.every((v) => v === null || v === undefined)) {
    return "-";
  }
  return dims.map((v) => (v === null || v === undefined ? "-" : v)).join(" × ");
}

/**
 * 货型关联可能被后端回填为 null（BFF 富化缺失时按 id 取不到对象）。
 * 列表渲染与编辑回填前统一过滤掉 null，避免 v-for 对 null 取 id/name 导致整页崩溃。
 */
function validGoodsTypes(list?: GoodsTypeSimple[] | null): GoodsTypeSimple[] {
  return (list ?? []).filter((item): item is GoodsTypeSimple => !!item);
}

/** 新增/编辑弹窗 */
interface TruckTypeForm {
  name: string;
  allowableLoad?: number | null;
  allowableVolume?: number | null;
  measureLong?: number | null;
  measureWidth?: number | null;
  measureHigh?: number | null;
  goodsTypeIds: string[];
}

const formVisible = ref(false);
const saving = ref(false);
/** 编辑态详情加载：弹窗先打开、列表行先回填，详情接口慢时给 loading 反馈而不是无响应 */
const detailLoading = ref(false);
const editingId = ref<string>();
const formRef = ref<FormInstance>();

const createDefaultForm = (): TruckTypeForm => ({
  name: "",
  allowableLoad: null,
  allowableVolume: null,
  measureLong: null,
  measureWidth: null,
  measureHigh: null,
  goodsTypeIds: [],
});
const form = reactive<TruckTypeForm>(createDefaultForm());

const rules: FormRules<TruckTypeForm> = {
  // whitespace: true 拦截纯空格名称（required 只挡空串）
  name: [{ required: true, whitespace: true, message: "请输入车辆类型名称", trigger: "blur" }],
};

const formTitle = computed(() => (editingId.value ? "编辑车辆类型" : "新增车辆类型"));

/** 把记录（列表行或详情）映射到表单结构 */
function applyRecordToForm(record: Partial<TruckTypeRecord>) {
  Object.assign(form, createDefaultForm(), {
    name: record.name ?? "",
    allowableLoad: record.allowableLoad ?? null,
    allowableVolume: record.allowableVolume ?? null,
    measureLong: record.measureLong ?? null,
    measureWidth: record.measureWidth ?? null,
    measureHigh: record.measureHigh ?? null,
    goodsTypeIds: validGoodsTypes(record.goodsTypes).map((item) => item.id),
  });
}

function openCreate() {
  editingId.value = undefined;
  detailLoading.value = false;
  Object.assign(form, createDefaultForm());
  formVisible.value = true;
}

async function openEdit(row: TruckTypeRecord) {
  editingId.value = row.id;
  applyRecordToForm(row);
  formVisible.value = true;
  detailLoading.value = true;
  try {
    const detail = await TruckTypeAPI.getDetail(row.id!);
    applyRecordToForm(detail);
  } catch {
    // 错误提示由请求拦截器统一弹出；详情拿不到则关闭弹窗，避免编辑到残缺数据
    formVisible.value = false;
  } finally {
    detailLoading.value = false;
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  saving.value = true;
  try {
    // 后端按 goodsTypes[].id 维护关联表
    const payload = {
      name: form.name.trim(),
      allowableLoad: form.allowableLoad ?? undefined,
      allowableVolume: form.allowableVolume ?? undefined,
      measureLong: form.measureLong ?? undefined,
      measureWidth: form.measureWidth ?? undefined,
      measureHigh: form.measureHigh ?? undefined,
      goodsTypes: form.goodsTypeIds.map((id) => ({ id })),
    };
    if (editingId.value) {
      await TruckTypeAPI.update(editingId.value, payload);
      ElMessage.success("更新成功");
    } else {
      await TruckTypeAPI.save(payload);
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
  detailLoading.value = false;
}

/**
 * 启停后记录可能因状态筛选离开当前页：末页仅剩这一条时回退一页，避免停在空页。
 * @param filteredOut 当前查询是否正按"会把该记录过滤掉"的状态筛选
 */
function fallbackPageIfNeeded(filteredOut: boolean) {
  if (filteredOut && list.value.length === 1 && query.value.page > 1) {
    query.value.page -= 1;
  }
}

async function handleDisable(row: TruckTypeRecord) {
  try {
    await ElMessageBox.confirm(
      `确认停用「${row.name}」？停用后该车型将不可再被选择，不影响已有车辆与业务数据，可随时重新启用。`,
      "停用确认",
      { type: "warning", confirmButtonText: "停用", cancelButtonText: "取消" },
    );
  } catch {
    return;
  }
  try {
    await TruckTypeAPI.disable(row.id!);
    ElMessage.success("已停用");
    fallbackPageIfNeeded(query.value.status === CommonStatus.ENABLED);
    loadList();
  } catch {
    // 错误提示由请求拦截器统一弹出，这里只收尾，避免双重 toast
  }
}

async function handleEnable(row: TruckTypeRecord) {
  try {
    await ElMessageBox.confirm(`确认重新启用「${row.name}」？启用后将恢复在各下拉选项中的选择。`, "启用确认", {
      type: "info",
      confirmButtonText: "启用",
      cancelButtonText: "取消",
    });
  } catch {
    return;
  }
  try {
    await TruckTypeAPI.enable(row.id!);
    ElMessage.success("已启用");
    fallbackPageIfNeeded(query.value.status === CommonStatus.DISABLED);
    loadList();
  } catch {
    // 错误提示由请求拦截器统一弹出，这里只收尾，避免双重 toast
  }
}

onMounted(() => {
  loadList();
  CommonSimpleAPI.goodsTypeOptions()
    .then((res) => {
      // 后端 findAll 未过滤停用项，下拉只保留启用状态
      goodsTypeOptions.value = (res ?? []).filter((item) => item.status === CommonStatus.ENABLED);
    })
    .catch(() => {
      // 错误提示由请求拦截器统一弹出；下拉保持空，新增/编辑时该字段不可选但不阻塞页面
    });
});
</script>

<style scoped lang="scss">
.truck-type__goods-tag {
  margin: 2px 4px 2px 0;
}

.truck-type__dimension {
  display: flex;
  gap: 8px;
  width: 100%;
}

.truck-type__dimension-item {
  flex: 1;
}
</style>
