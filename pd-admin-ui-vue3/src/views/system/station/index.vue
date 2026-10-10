<template>
  <div class="page-container">
    <el-card class="page-search" shadow="never">
      <el-form :model="queryParams" :inline="true">
        <el-form-item label="岗位名称" prop="name">
          <el-input
            v-model="queryParams.name"
            placeholder="岗位名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="所属组织" prop="orgId">
          <el-tree-select
            v-model="queryParams.orgId"
            :data="orgTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            :render-after-expand="false"
            placeholder="全部组织"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">搜索</el-button>
          <el-button @click="handleResetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="page-content" shadow="never">
      <div class="page-toolbar">
        <div class="page-toolbar__left">
          <el-button type="primary" @click="openDialog()">新增岗位</el-button>
        </div>
        <div class="page-toolbar__right">
          <el-tooltip content="刷新" placement="top">
            <el-button class="page-icon-btn" @click="loadPage">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="name" label="岗位名称" min-width="180" />
          <el-table-column prop="orgId" label="所属组织" min-width="200">
            <template #default="scope">{{ orgNameMap.get(scope.row.orgId) ?? scope.row.orgId }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag v-if="scope.row.status" type="success">正常</el-tag>
              <el-tag v-else type="info">停用</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="describe" label="简介" min-width="200" show-overflow-tooltip />
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" fixed="right" align="left" width="150">
            <template #default="scope">
              <el-button type="primary" link size="small" @click.stop="openDialog(scope.row as Station)">
                编辑
              </el-button>
              <el-button type="danger" link size="small" @click.stop="handleDelete(scope.row as Station)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="page-pagination">
        <el-pagination
          v-model:current-page="queryParams.pageNo"
          v-model:page-size="queryParams.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadPage"
          @current-change="loadPage"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialog.visible"
      :title="dialog.title"
      width="520px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="岗位名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入岗位名称" />
        </el-form-item>
        <el-form-item label="所属组织" prop="orgId">
          <el-tree-select
            v-model="form.orgId"
            :data="orgTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            :render-after-expand="false"
            placeholder="请选择所属组织"
            style="width: 100%"
          />
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
import { ref, reactive, computed, onMounted } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import { Refresh } from "@element-plus/icons-vue";
import StationAPI from "@/api/station";
import type { Station, StationForm } from "@/api/station";
import OrgAPI from "@/api/org";
import type { OrgNode } from "@/api/org";

const loading = ref(false);
const list = ref<Station[]>([]);
const total = ref(0);
const orgTree = ref<OrgNode[]>([]);

/** orgId -> 组织全名映射，用于表格列展示 */
const orgNameMap = computed(() => {
  const map = new Map<number, string>();
  const walk = (nodes: OrgNode[], parents: string[]) => {
    nodes.forEach((node) => {
      const chain = [...parents, node.name];
      map.set(node.id, chain.join(" / "));
      if (node.children?.length) walk(node.children, chain);
    });
  };
  walk(orgTree.value, []);
  return map;
});

const queryParams = reactive({ pageNo: 1, size: 10, name: "", orgId: undefined as number | undefined });

async function loadOrgTree() {
  orgTree.value = (await OrgAPI.getTree()) ?? [];
}

async function loadPage() {
  loading.value = true;
  try {
    const result = await StationAPI.page({
      pageNo: queryParams.pageNo,
      size: queryParams.size,
      name: queryParams.name || undefined,
      orgId: queryParams.orgId,
    });
    list.value = result.records ?? [];
    total.value = result.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  queryParams.pageNo = 1;
  loadPage();
}

function handleResetQuery() {
  queryParams.name = "";
  queryParams.orgId = undefined;
  queryParams.pageNo = 1;
  loadPage();
}

const dialog = ref({ visible: false, title: "" });
const submitting = ref(false);
const formRef = ref<FormInstance>();

const emptyForm = (): StationForm => ({ name: "", orgId: undefined as unknown as number, status: true, describe: "" });
const form = ref<StationForm>(emptyForm());

const rules: FormRules = {
  name: [{ required: true, message: "请输入岗位名称", trigger: "blur" }],
  orgId: [{ required: true, message: "请选择所属组织", trigger: "change" }],
};

function openDialog(row?: Station) {
  if (row) {
    dialog.value.title = `编辑 - ${row.name}`;
    form.value = { ...row };
  } else {
    dialog.value.title = "新增岗位";
    form.value = emptyForm();
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
    if (form.value.id) {
      await StationAPI.update(form.value);
      ElMessage.success("修改成功");
    } else {
      await StationAPI.create(form.value);
      ElMessage.success("新增成功");
    }
    dialog.value.visible = false;
    await loadPage();
  } finally {
    submitting.value = false;
  }
}

async function handleDelete(row: Station) {
  await ElMessageBox.confirm(`确定删除岗位「${row.name}」吗？`, "删除确认", {
    type: "warning",
    confirmButtonText: "确定删除",
    cancelButtonText: "取消",
  });
  await StationAPI.remove(row.id);
  ElMessage.success("删除成功");
  await loadPage();
}

onMounted(async () => {
  await loadOrgTree();
  await loadPage();
});
</script>
