<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 + 新增 -->
      <div class="page-toolbar">
        <div class="page-toolbar__left">
          <el-form :model="query" inline>
            <el-form-item label="名称" prop="name">
              <el-input v-model="query.name" placeholder="车队名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item label="编号" prop="fleetNumber">
              <el-input v-model="query.fleetNumber" placeholder="车队编号" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item label="负责人" prop="manager">
              <el-select
                v-model="query.manager"
                filterable
                remote
                clearable
                :remote-method="searchUsers"
                :loading="userLoading"
                placeholder="全部负责人"
                style="width: 180px"
              >
                <el-option
                  v-for="user in userOptions"
                  :key="user.userId"
                  :value="user.userId"
                  :label="managerLabel(user)"
                />
              </el-select>
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
          <el-button type="primary" :icon="Plus" @click="openCreate">新增车队</el-button>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="name" label="车队名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="fleetNumber" label="车队编号" min-width="130" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.fleetNumber || "-" }}</template>
          </el-table-column>
          <el-table-column label="所属机构" min-width="150" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.agency?.name || "-" }}</template>
          </el-table-column>
          <el-table-column label="负责人" min-width="110">
            <template #default="scope">{{ scope.row.manager?.name || "-" }}</template>
          </el-table-column>
          <el-table-column prop="truckCount" label="车辆数" min-width="90" align="right" />
          <el-table-column prop="driverCount" label="司机数" min-width="90" align="right" />
          <el-table-column label="状态" min-width="90" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
                {{ scope.row.status === 1 ? "启用" : "停用" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="120" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openEdit(scope.row as FleetRecord)">编辑</el-button>
              <el-button
                v-if="scope.row.status === 1"
                type="danger"
                link
                size="small"
                @click="handleDisable(scope.row as FleetRecord)"
                >停用</el-button
              >
              <el-button v-else type="success" link size="small" @click="handleEnable(scope.row as FleetRecord)">启用</el-button>
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
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="车队名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入车队名称" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="车队编号" prop="fleetNumber">
          <el-input v-model="form.fleetNumber" placeholder="选填，请输入车队编号" maxlength="32" />
        </el-form-item>
        <el-form-item label="负责人" prop="managerId">
          <el-select
            v-model="form.managerId"
            filterable
            remote
            clearable
            :remote-method="searchUsers"
            :loading="userLoading"
            placeholder="输入姓名搜索负责人"
            style="width: 100%"
          >
            <el-option
              v-for="user in userOptions"
              :key="user.userId"
              :value="user.userId"
              :label="managerLabel(user)"
            />
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
import FleetAPI from "@/api/fleet";
import type { FleetRecord, FleetPageQuery } from "@/api/fleet";
import CommonSimpleAPI from "@/api/common-simple";
import type { SysUserSimple } from "@/api/common-simple";

/** 负责人下拉选项（查询栏与弹窗共用） */
const userOptions = ref<SysUserSimple[]>([]);
const userLoading = ref(false);

function managerLabel(user: SysUserSimple): string {
  const displayName = user.name || user.username || user.mobile || user.userId;
  return user.workNumber ? `${displayName}（${user.workNumber}）` : displayName;
}

async function searchUsers(name?: string) {
  userLoading.value = true;
  try {
    userOptions.value = (await CommonSimpleAPI.userOptions(name)) ?? [];
  } finally {
    userLoading.value = false;
  }
}

/** 编辑回填时把详情中的负责人合并进下拉选项，避免只显示 userId */
function mergeUserOption(user?: SysUserSimple) {
  if (user && !userOptions.value.some((item) => item.userId === user.userId)) {
    userOptions.value.unshift(user);
  }
}

/** 查询与列表状态 */
const loading = ref(false);
const list = ref<FleetRecord[]>([]);
const total = ref(0);
const query = ref<FleetPageQuery>({ page: 1, pageSize: 10 });

/** 组装查询参数：剔除空字符串/空值，仅提交有效过滤条件 */
function buildParams(): FleetPageQuery {
  const { page, pageSize, ...filters } = query.value;
  const params: FleetPageQuery = { page, pageSize };
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
    const res = await FleetAPI.page(buildParams());
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
interface FleetForm {
  name: string;
  fleetNumber: string;
  managerId?: string;
}

const formVisible = ref(false);
const saving = ref(false);
const editingId = ref<string>();
const formRef = ref<FormInstance>();

const createDefaultForm = (): FleetForm => ({
  name: "",
  fleetNumber: "",
  managerId: undefined,
});
const form = reactive<FleetForm>(createDefaultForm());

const rules: FormRules<FleetForm> = {
  name: [{ required: true, message: "请输入车队名称", trigger: "blur" }],
};

const formTitle = computed(() => (editingId.value ? "编辑车队" : "新增车队"));

function openCreate() {
  editingId.value = undefined;
  Object.assign(form, createDefaultForm());
  formVisible.value = true;
}

async function openEdit(row: FleetRecord) {
  const detail = await FleetAPI.getDetail(row.id!);
  editingId.value = row.id;
  mergeUserOption(detail.manager);
  Object.assign(form, createDefaultForm(), {
    name: detail.name ?? "",
    fleetNumber: detail.fleetNumber ?? "",
    managerId: detail.manager?.userId,
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
      fleetNumber: form.fleetNumber.trim() || undefined,
      manager: form.managerId ? { userId: form.managerId } : null,
    };
    if (editingId.value) {
      await FleetAPI.update(editingId.value, payload);
      ElMessage.success("更新成功");
    } else {
      await FleetAPI.save(payload);
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

async function handleDisable(row: FleetRecord) {
  try {
    await ElMessageBox.confirm(
      `确认停用「${row.name}」？停用后该车队将不可再被选择（新增车辆/司机归属等），不影响已有车辆与业务数据，可随时重新启用。`,
      "停用确认",
      { type: "warning", confirmButtonText: "停用", cancelButtonText: "取消" },
    );
  } catch {
    return;
  }
  try {
    await FleetAPI.disable(row.id!);
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

async function handleEnable(row: FleetRecord) {
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
    await FleetAPI.enable(row.id!);
    ElMessage.success("已启用");
    loadList();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "启用失败");
  }
}

onMounted(() => {
  loadList();
  // 预载全部用户，下拉直接可选；输入关键字时再走 remote 模糊查询
  searchUsers();
});
</script>
