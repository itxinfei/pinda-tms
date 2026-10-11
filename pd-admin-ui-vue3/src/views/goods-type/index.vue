<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 + 新增 -->
      <div class="page-toolbar">
        <div class="page-toolbar__left">
          <el-form :model="query" inline>
            <el-form-item label="名称" prop="name">
              <el-input v-model="query.name" placeholder="货物类型名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px">
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="page-toolbar__right">
          <el-button type="primary" :icon="Plus" @click="openCreate">新增货物类型</el-button>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="defaultWeight" label="默认重量(kg)" min-width="130" align="right" />
          <el-table-column prop="defaultVolume" label="默认体积(方)" min-width="130" align="right" />
          <el-table-column prop="remark" label="说明" min-width="220" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.remark || "-" }}</template>
          </el-table-column>
          <el-table-column label="状态" min-width="90" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
                {{ scope.row.status === 1 ? "启用" : "停用" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="120" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openEdit(scope.row as GoodsTypeRecord)">编辑</el-button>
              <el-button
                v-if="scope.row.status === 1"
                type="danger"
                link
                size="small"
                @click="handleDisable(scope.row as GoodsTypeRecord)"
                >停用</el-button
              >
              <el-button v-else type="success" link size="small" @click="handleEnable(scope.row as GoodsTypeRecord)">启用</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="formTitle" width="520px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入货物类型名称" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="默认重量(kg)" prop="defaultWeight">
          <el-input-number
            v-model="form.defaultWeight"
            :min="0"
            :precision="2"
            :value-on-clear="null"
            controls-position="right"
            placeholder="选填"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="默认体积(方)" prop="defaultVolume">
          <el-input-number
            v-model="form.defaultVolume"
            :min="0"
            :precision="2"
            :value-on-clear="null"
            controls-position="right"
            placeholder="选填"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="说明" prop="remark">
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
import { Plus } from "@element-plus/icons-vue";
import type { FormInstance, FormRules } from "element-plus";
import GoodsTypeAPI from "@/api/goods-type";
import type { GoodsTypeRecord, GoodsTypePageQuery } from "@/api/goods-type";

/** 查询与列表状态 */
const loading = ref(false);
const list = ref<GoodsTypeRecord[]>([]);
const total = ref(0);
const query = ref<GoodsTypePageQuery>({ page: 1, pageSize: 10 });

/** 组装查询参数：剔除空字符串/空值，仅提交有效过滤条件 */
function buildParams(): GoodsTypePageQuery {
  const { page, pageSize, ...filters } = query.value;
  const params: GoodsTypePageQuery = { page, pageSize };
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
    const res = await GoodsTypeAPI.page(buildParams());
    list.value = res?.items ?? [];
    total.value = res?.counts ?? 0;
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

/** 新增/编辑弹窗 */
const formVisible = ref(false);
const saving = ref(false);
const editingId = ref<string>();
const formRef = ref<FormInstance>();

const createDefaultForm = (): GoodsTypeRecord => ({
  name: "",
  defaultWeight: undefined,
  defaultVolume: undefined,
  remark: "",
});
const form = reactive<GoodsTypeRecord>(createDefaultForm());

const rules: FormRules<GoodsTypeRecord> = {
  name: [{ required: true, message: "请输入货物类型名称", trigger: "blur" }],
};

const formTitle = computed(() => (editingId.value ? "编辑货物类型" : "新增货物类型"));

function openCreate() {
  editingId.value = undefined;
  Object.assign(form, createDefaultForm());
  formVisible.value = true;
}

async function openEdit(row: GoodsTypeRecord) {
  const detail = await GoodsTypeAPI.getDetail(row.id!);
  editingId.value = row.id;
  Object.assign(form, createDefaultForm(), detail);
  formVisible.value = true;
}

/** 提交体只取表单字段，避免 id/status 等回显字段透传到后端 */
function buildPayload(): GoodsTypeRecord {
  return {
    name: form.name.trim(),
    defaultWeight: form.defaultWeight ?? undefined,
    defaultVolume: form.defaultVolume ?? undefined,
    remark: form.remark?.trim() || undefined,
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
      await GoodsTypeAPI.update(editingId.value, buildPayload());
      ElMessage.success("更新成功");
    } else {
      await GoodsTypeAPI.save(buildPayload());
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

async function handleDisable(row: GoodsTypeRecord) {
  try {
    await ElMessageBox.confirm(
      `确认停用「${row.name}」？停用后该货物类型将不可再被选择，不影响已有业务数据，可随时重新启用。若仍被车辆类型关联，后端会拦截并提示。`,
      "停用确认",
      { type: "warning", confirmButtonText: "停用", cancelButtonText: "取消" },
    );
  } catch {
    return;
  }
  try {
    await GoodsTypeAPI.disable(row.id!);
    ElMessage.success("已停用");
    // 筛选"启用"状态时，停用后记录会从结果消失；末页仅剩一条则回退一页，避免停在空页
    if (query.value.status === 1 && list.value.length === 1 && query.value.page > 1) {
      query.value.page -= 1;
    }
    loadList();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "停用失败");
  }
}

async function handleEnable(row: GoodsTypeRecord) {
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
    await GoodsTypeAPI.enable(row.id!);
    ElMessage.success("已启用");
    loadList();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "启用失败");
  }
}

onMounted(() => {
  loadList();
});
</script>
