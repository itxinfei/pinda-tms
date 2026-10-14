<template>
  <div class="page-container">
    <el-card class="page-search" shadow="never">
      <el-form ref="queryFormRef" :model="params" :inline="true">
        <el-form-item label="关键字" prop="keywords">
          <el-input
            v-model="params.keywords"
            placeholder="请输入配置键 / 配置名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-select
            v-model="params.status"
            placeholder="全部"
            clearable
            style="width: 120px"
            @change="handleQuery"
          >
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleQuery">搜索</el-button>
          <el-button @click="handleResetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card ref="tableWrapperRef" class="page-content" shadow="never">
      <div class="page-toolbar">
        <div class="page-toolbar__left">
          <el-button type="primary" @click="openDialog()">新增参数</el-button>
        </div>
        <div class="page-toolbar__right">
          <el-tooltip content="刷新" placement="top">
            <el-button class="page-icon-btn" @click="handleQuery">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="全屏" placement="top">
            <el-button class="page-icon-btn" @click="toggleFullscreen">
              <el-icon><FullScreen /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table
          ref="dataTableRef"
          v-loading="loading"
          :data="list"
          class="page-table"
          height="100%"
          highlight-current-row
          border
        >
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column key="configName" label="参数名称" prop="configName" min-width="140" />
          <el-table-column key="configKey" label="参数键" prop="configKey" min-width="160" />
          <el-table-column label="参数值" min-width="180">
            <template #default="{ row }">
              <span v-if="row.isSensitive === 1" class="config-value--secret">
                {{ row.valueMasked || "（未设置）" }}
              </span>
              <span v-else>{{ row.configValue ?? "" }}</span>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="90">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.valueType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="敏感" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.isSensitive === 1 ? 'danger' : 'info'">
                {{ row.isSensitive === 1 ? "是" : "否" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 1 ? 'success' : 'warning'">
                {{ row.status === 1 ? "启用" : "停用" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column key="remark" label="备注" prop="remark" min-width="120" />
          <el-table-column fixed="right" label="操作" width="160">
            <template #default="scope">
              <el-button type="primary" size="small" link @click="openDialog(scope.row.id)">
                编辑
              </el-button>
              <el-button type="danger" size="small" link @click="handleDelete(scope.row.id)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <pagination
        v-if="total > 0"
        v-model:total="total"
        v-model:page="params.pageNum"
        v-model:limit="params.pageSize"
        class="page-pagination"
        @pagination="fetchData"
      />
    </el-card>

    <el-dialog
      v-model="dialogState.visible"
      :title="dialogState.title"
      width="560px"
      @close="closeDialog"
    >
      <el-form
        ref="dataFormRef"
        :model="formData"
        :rules="rules"
        label-suffix=":"
        label-width="100px"
      >
        <el-form-item label="参数名称" prop="configName">
          <el-input v-model="formData.configName" placeholder="请输入参数名称" :maxlength="100" />
        </el-form-item>
        <el-form-item label="参数键" prop="configKey">
          <el-input
            v-model="formData.configKey"
            placeholder="如 baidu.map.ak（字母/数字/点/下划线/中划线）"
            :maxlength="150"
          />
        </el-form-item>
        <el-form-item label="值类型" prop="valueType">
          <el-select v-model="formData.valueType" style="width: 100%">
            <el-option label="字符串 string" value="string" />
            <el-option label="数字 number" value="number" />
            <el-option label="布尔 boolean" value="boolean" />
            <el-option label="JSON" value="json" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否敏感" prop="isSensitive">
          <el-radio-group v-model="formData.isSensitive" :disabled="!!formData.id">
            <el-radio :value="0">否</el-radio>
            <el-radio :value="1">是（加密存储，列表脱敏）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="参数值" prop="configValue">
          <el-input
            v-model="formData.configValue"
            :type="formData.isSensitive === 1 ? 'password' : 'text'"
            :show-password="formData.isSensitive === 1"
            :placeholder="valuePlaceholder"
            :maxlength="500"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="formData.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            :rows="3"
            :maxlength="255"
            show-word-limit
            type="textarea"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="handleSubmit">确定</el-button>
          <el-button @click="closeDialog">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { useFullscreen } from "@vueuse/core";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import { Refresh, FullScreen } from "@element-plus/icons-vue";

import ConfigAPI from "@/api/system/config";
import type { ConfigItem, ConfigForm, ConfigQueryParams } from "@/api/system/config";
import { usePageTable } from "@/composables";

defineOptions({
  name: "Config",
  inheritAttrs: false,
});

const tableWrapperRef = ref<HTMLElement | null>(null);
const { toggle: toggleFullscreen } = useFullscreen(tableWrapperRef);

const queryFormRef = ref<FormInstance>();
const dataFormRef = ref<FormInstance>();

// 分页表格数据管理
const { loading, list, total, params, fetchData, handleQuery, handleResetQuery } = usePageTable<
  ConfigItem,
  ConfigQueryParams
>({
  initialParams: { pageNum: 1, pageSize: 10, keywords: "", status: "" },
  request: ConfigAPI.getPage,
  onBeforeReset: () => queryFormRef.value?.resetFields(),
});

const dialogState = reactive({ title: "", visible: false });

// 敏感项编辑时的当前脱敏值（仅用于占位提示，不回显明文）
const currentMasked = ref("");

function createDefaultForm(): ConfigForm {
  return {
    id: undefined,
    configName: "",
    configKey: "",
    configValue: "",
    valueType: "string",
    isSensitive: 0,
    status: 1,
    remark: "",
  };
}

const formData = reactive<ConfigForm>(createDefaultForm());

// 敏感项编辑时占位提示「当前脱敏值，留空不修改」
const valuePlaceholder = computed(() => {
  if (formData.id && formData.isSensitive === 1) {
    return currentMasked.value
      ? `当前：${currentMasked.value}，留空则不修改`
      : "当前未设置，请输入参数值";
  }
  return "请输入参数值";
});

const rules: FormRules = {
  configName: [{ required: true, message: "请输入参数名称", trigger: "blur" }],
  configKey: [
    { required: true, message: "请输入参数键", trigger: "blur" },
    {
      pattern: /^[A-Za-z0-9_.-]+$/,
      message: "只能包含字母、数字、点、下划线、中划线",
      trigger: "blur",
    },
  ],
  configValue: [
    {
      validator: (_rule, value: string, callback) => {
        // 新增必填；编辑时非敏感必填；编辑敏感项允许留空（留空=不修改原值）
        const isEditSensitive = !!formData.id && formData.isSensitive === 1;
        if (!isEditSensitive && !value) {
          callback(new Error("请输入参数值"));
          return;
        }
        callback();
      },
      trigger: "blur",
    },
  ],
};

/**
 * 打开新增/编辑参数配置弹窗
 */
async function openDialog(id?: string): Promise<void> {
  Object.assign(formData, createDefaultForm());
  currentMasked.value = "";
  if (id) {
    dialogState.title = "修改参数配置";
    const data = await ConfigAPI.detail(id);
    // valueMasked 仅用于占位提示，不进表单提交体；敏感值不回显明文，置空由用户重新输入（留空=不改）
    const { valueMasked, ...editable } = data;
    Object.assign(formData, editable, {
      configValue: data.isSensitive === 1 ? "" : (data.configValue ?? ""),
    });
    currentMasked.value = data.isSensitive === 1 ? (valueMasked ?? "") : "";
  } else {
    dialogState.title = "新增参数配置";
  }
  dialogState.visible = true;
}

/**
 * 校验并提交参数配置表单
 */
async function handleSubmit(): Promise<void> {
  const valid = await dataFormRef.value?.validate().catch(() => false);
  if (!valid) return;

  loading.value = true;
  try {
    const id = formData.id;
    if (id) {
      await ConfigAPI.update(id, formData);
      ElMessage.success("修改成功");
    } else {
      await ConfigAPI.create(formData);
      ElMessage.success("新增成功");
    }
    closeDialog();
    // 保留当前页码与搜索条件刷新
    fetchData();
  } finally {
    loading.value = false;
  }
}

/**
 * 关闭弹窗并重置表单
 */
function closeDialog(): void {
  dialogState.visible = false;
  dataFormRef.value?.resetFields();
  dataFormRef.value?.clearValidate();
  Object.assign(formData, createDefaultForm());
  currentMasked.value = "";
}

/**
 * 删除参数配置（物理删除）
 */
async function handleDelete(id: string): Promise<void> {
  await ElMessageBox.confirm("确认删除该项参数配置？删除后不可恢复", "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  });
  loading.value = true;
  try {
    await ConfigAPI.deleteById(id);
    ElMessage.success("删除成功");
    // 保留当前页码与搜索条件刷新
    fetchData();
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  handleQuery();
});
</script>

<style scoped>
.config-value--secret {
  font-family: var(--el-font-family-monospace, monospace);
  letter-spacing: 1px;
  color: var(--el-text-color-secondary);
}
</style>
