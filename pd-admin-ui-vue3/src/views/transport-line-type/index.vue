<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 + 新增 -->
      <div class="page-toolbar">
        <div class="page-toolbar__left">
          <el-form :model="query" inline>
            <el-form-item label="名称" prop="name">
              <el-input v-model="query.name" placeholder="线路类型名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item label="编码" prop="typeNumber">
              <el-input v-model="query.typeNumber" placeholder="线路类型编码" clearable @keyup.enter="handleSearch" />
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
          <el-button type="primary" :icon="Plus" @click="openCreate">新增线路类型</el-button>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="typeNumber" label="编码" min-width="140" show-overflow-tooltip />
          <el-table-column label="起始机构类型" min-width="140">
            <template #default="scope">{{ scope.row.startAgencyTypeName || agencyTypeText(scope.row.startAgencyType) }}</template>
          </el-table-column>
          <el-table-column label="目的机构类型" min-width="140">
            <template #default="scope">{{ scope.row.endAgencyTypeName || agencyTypeText(scope.row.endAgencyType) }}</template>
          </el-table-column>
          <el-table-column label="更新人" min-width="110">
            <template #default="scope">{{ scope.row.updater?.name || "-" }}</template>
          </el-table-column>
          <el-table-column prop="lastUpdateTime" label="最后更新时间" min-width="170" />
          <el-table-column label="状态" min-width="90" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
                {{ scope.row.status === 1 ? "启用" : "停用" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="120" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openEdit(scope.row as TransportLineTypeRecord)">编辑</el-button>
              <el-button
                v-if="scope.row.status === 1"
                type="danger"
                link
                size="small"
                @click="handleDisable(scope.row as TransportLineTypeRecord)"
                >停用</el-button
              >
              <el-button v-else type="success" link size="small" @click="handleEnable(scope.row as TransportLineTypeRecord)">启用</el-button>
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
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入线路类型名称" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="编码" prop="typeNumber">
          <el-input v-model="form.typeNumber" placeholder="请输入线路类型编码，如 CITY_TRUNK" maxlength="32" />
        </el-form-item>
        <el-form-item label="起始机构类型" prop="startAgencyType">
          <el-select v-model="form.startAgencyType" placeholder="请选择" clearable style="width: 100%">
            <el-option v-for="item in AGENCY_TYPE_OPTIONS" :key="item.value" :value="item.value" :label="item.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="目的机构类型" prop="endAgencyType">
          <el-select v-model="form.endAgencyType" placeholder="请选择" clearable style="width: 100%">
            <el-option v-for="item in AGENCY_TYPE_OPTIONS" :key="item.value" :value="item.value" :label="item.label" />
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
import TransportLineTypeAPI from "@/api/transport-line-type";
import type { TransportLineTypeRecord, TransportLineTypePageQuery } from "@/api/transport-line-type";

/**
 * 机构类型枚举，与后端 com.itheima.pinda.enums.org.OrgType 保持一致
 * 1 分公司 / 2 一级转运中心 / 3 二级转运中心 / 4 网点
 */
const AGENCY_TYPE_OPTIONS = [
  { value: 1, label: "分公司" },
  { value: 2, label: "一级转运中心" },
  { value: 3, label: "二级转运中心" },
  { value: 4, label: "网点" },
] as const;

function agencyTypeText(value?: number): string {
  if (value === null || value === undefined) {
    return "-";
  }
  return AGENCY_TYPE_OPTIONS.find((item) => item.value === value)?.label ?? "-";
}

/** 查询与列表状态 */
const loading = ref(false);
const list = ref<TransportLineTypeRecord[]>([]);
const total = ref(0);
const query = ref<TransportLineTypePageQuery>({ page: 1, pageSize: 10 });

/** 组装查询参数：剔除空字符串/空值，仅提交有效过滤条件 */
function buildParams(): TransportLineTypePageQuery {
  const { page, pageSize, ...filters } = query.value;
  const params: TransportLineTypePageQuery = { page, pageSize };
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
    const res = await TransportLineTypeAPI.page(buildParams());
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
interface TransportLineTypeForm {
  name: string;
  typeNumber: string;
  startAgencyType?: number;
  endAgencyType?: number;
}

const formVisible = ref(false);
const saving = ref(false);
const editingId = ref<string>();
const formRef = ref<FormInstance>();

const createDefaultForm = (): TransportLineTypeForm => ({
  name: "",
  typeNumber: "",
  startAgencyType: undefined,
  endAgencyType: undefined,
});
const form = reactive<TransportLineTypeForm>(createDefaultForm());

const rules: FormRules<TransportLineTypeForm> = {
  name: [{ required: true, message: "请输入线路类型名称", trigger: "blur" }],
  typeNumber: [{ required: true, message: "请输入线路类型编码", trigger: "blur" }],
};

const formTitle = computed(() => (editingId.value ? "编辑线路类型" : "新增线路类型"));

function openCreate() {
  editingId.value = undefined;
  Object.assign(form, createDefaultForm());
  formVisible.value = true;
}

async function openEdit(row: TransportLineTypeRecord) {
  const detail = await TransportLineTypeAPI.getDetail(row.id!);
  editingId.value = row.id;
  Object.assign(form, createDefaultForm(), {
    name: detail.name ?? "",
    typeNumber: detail.typeNumber ?? "",
    startAgencyType: detail.startAgencyType,
    endAgencyType: detail.endAgencyType,
  });
  formVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  saving.value = true;
  try {
    const payload = {
      name: form.name.trim(),
      typeNumber: form.typeNumber.trim(),
      startAgencyType: form.startAgencyType,
      endAgencyType: form.endAgencyType,
    };
    if (editingId.value) {
      await TransportLineTypeAPI.update(editingId.value, payload);
      ElMessage.success("更新成功");
    } else {
      await TransportLineTypeAPI.save(payload);
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

async function handleDisable(row: TransportLineTypeRecord) {
  try {
    await ElMessageBox.confirm(
      `确认停用「${row.name}」？停用后该线路类型将不可再被选择，不影响已有线路与业务数据，可随时重新启用。`,
      "停用确认",
      { type: "warning", confirmButtonText: "停用", cancelButtonText: "取消" },
    );
  } catch {
    return;
  }
  try {
    await TransportLineTypeAPI.disable(row.id!);
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

async function handleEnable(row: TransportLineTypeRecord) {
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
    await TransportLineTypeAPI.enable(row.id!);
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
