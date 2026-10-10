<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <div class="page-toolbar">
        <div class="page-toolbar__left">
          <el-button type="primary" @click="openDialog('root')">新增组织</el-button>
        </div>
        <div class="page-toolbar__right">
          <el-tooltip :content="expandAll ? '折叠全部' : '展开全部'" placement="top">
            <el-button class="page-icon-btn" @click="toggleExpandAll">
              <span v-if="expandAll" class="i-svg:checkbox-indeterminate" />
              <span v-else class="i-svg:add-box" />
            </el-button>
          </el-tooltip>
          <el-divider class="page-toolbar__divider" direction="vertical" />
          <el-tooltip content="刷新" placement="top">
            <el-button class="page-icon-btn" @click="loadTree">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table
          :key="tableKey"
          v-loading="loading"
          :data="tree"
          class="page-table"
          row-key="id"
          :default-expand-all="expandAll"
          border
          height="100%"
          :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        >
          <el-table-column prop="name" label="组织名称" min-width="220" />
          <el-table-column prop="orgType" label="组织类型" width="150" align="center">
            <template #default="scope">
              <el-tag :type="orgTagType(scope.row.orgType)">
                {{ orgTypeLabel(scope.row.orgType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="contractNumber" label="联系电话" width="150" />
          <el-table-column prop="sortValue" label="排序" width="90" align="center" />
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="scope">
              <el-tag v-if="scope.row.status" type="success">正常</el-tag>
              <el-tag v-else type="info">停用</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" align="left" width="210">
            <template #default="scope">
              <el-button type="primary" link size="small" @click.stop="openDialog('child', scope.row as OrgNode)">
                新增下级
              </el-button>
              <el-button type="primary" link size="small" @click.stop="openDialog('edit', scope.row as OrgNode)">
                编辑
              </el-button>
              <el-button type="danger" link size="small" @click.stop="handleDelete(scope.row as OrgNode)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-dialog
      v-model="dialog.visible"
      :title="dialog.title"
      width="600px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="上级组织" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            :render-after-expand="false"
            placeholder="请选择上级组织"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="组织名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入组织名称" />
        </el-form-item>
        <el-form-item label="组织类型" prop="orgType">
          <el-select v-model="form.orgType" placeholder="请选择组织类型" style="width: 100%">
            <el-option
              v-for="item in ORG_TYPE_OPTIONS"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="简称" prop="abbreviation">
          <el-input v-model="form.abbreviation" placeholder="请输入简称" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contractNumber">
          <el-input v-model="form.contractNumber" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="排序" prop="sortValue">
          <el-input-number v-model="form.sortValue" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" />
        </el-form-item>
        <el-form-item label="简介" prop="describe">
          <el-input v-model="form.describe" type="textarea" :rows="2" placeholder="请输入简介" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import { Refresh } from "@element-plus/icons-vue";
import OrgAPI from "@/api/org";
import type { OrgNode, OrgForm, OrgType } from "@/api/org";

/** 组织类型枚举（与后端 CoreOrg 注释口径一致） */
const ORG_TYPE_OPTIONS: { value: OrgType; label: string }[] = [
  { value: 1, label: "分公司" },
  { value: 2, label: "一级转运中心" },
  { value: 3, label: "二级转运中心" },
  { value: 4, label: "网点" },
];

const orgTypeLabel = (type: OrgType) =>
  ORG_TYPE_OPTIONS.find((item) => item.value === type)?.label ?? "未知";

const ORG_TAG_TYPE: Record<OrgType, "primary" | "success" | "warning" | "info"> = {
  1: "primary",
  2: "success",
  3: "warning",
  4: "info",
};
const orgTagType = (type: OrgType) => ORG_TAG_TYPE[type] ?? "info";

const loading = ref(false);
const tree = ref<OrgNode[]>([]);

async function loadTree() {
  loading.value = true;
  try {
    tree.value = (await OrgAPI.getTree()) ?? [];
  } finally {
    loading.value = false;
  }
}

/** 展开/折叠：default-expand-all 仅初始化生效，通过 key 重建表格 */
const expandAll = ref(true);
const tableKey = ref(0);
function toggleExpandAll() {
  expandAll.value = !expandAll.value;
  tableKey.value++;
}

/** 上级组织下拉：顶级 + 现有组织树 */
const parentOptions = computed<OrgNode[]>(() => [
  { id: 0, name: "顶级组织（根）", parentId: 0, orgType: 1, status: true },
  ...tree.value,
]);

type DialogMode = "root" | "child" | "edit";
const dialog = ref({ visible: false, title: "", mode: "root" as DialogMode });
const submitting = ref(false);
const formRef = ref<FormInstance>();

const emptyForm = (): OrgForm => ({
  parentId: 0,
  name: "",
  orgType: 1,
  abbreviation: "",
  contractNumber: "",
  address: "",
  sortValue: 0,
  status: true,
  describe: "",
});
const form = ref<OrgForm>(emptyForm());

const rules: FormRules = {
  parentId: [{ required: true, message: "请选择上级组织", trigger: "change" }],
  name: [{ required: true, message: "请输入组织名称", trigger: "blur" }],
  orgType: [{ required: true, message: "请选择组织类型", trigger: "change" }],
};

function openDialog(mode: DialogMode, row?: OrgNode) {
  dialog.value.mode = mode;
  if (mode === "root") {
    dialog.value.title = "新增组织";
    form.value = emptyForm();
  } else if (mode === "child" && row) {
    dialog.value.title = `新增下级 - ${row.name}`;
    form.value = { ...emptyForm(), parentId: row.id, orgType: row.orgType };
  } else if (mode === "edit" && row) {
    dialog.value.title = `编辑 - ${row.name}`;
    form.value = { ...emptyForm(), ...row };
  }
  dialog.value.visible = true;
}

function resetForm() {
  form.value = emptyForm();
  formRef.value?.clearValidate();
}

async function submitForm() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    if (dialog.value.mode === "edit") {
      await OrgAPI.update(form.value);
      ElMessage.success("修改成功");
    } else {
      await OrgAPI.create(form.value);
      ElMessage.success("新增成功");
    }
    dialog.value.visible = false;
    await loadTree();
  } finally {
    submitting.value = false;
  }
}

async function handleDelete(row: OrgNode) {
  await ElMessageBox.confirm(
    `确定删除组织「${row.name}」吗？存在下级组织时将无法删除。`,
    "删除确认",
    { type: "warning", confirmButtonText: "确定删除", cancelButtonText: "取消" }
  );
  await OrgAPI.remove(row.id);
  ElMessage.success("删除成功");
  await loadTree();
}

onMounted(loadTree);
</script>
