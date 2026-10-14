<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 + 新增 -->
      <div class="page-toolbar">
        <div class="page-toolbar__left">
          <el-form :model="query" inline>
            <el-form-item label="手机号" prop="phone">
              <el-input
                v-model="query.phone"
                placeholder="按手机号模糊查询"
                clearable
                style="width: 200px"
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px">
                <el-option label="正常" :value="MemberStatus.ENABLED" />
                <el-option label="禁用" :value="MemberStatus.DISABLED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="page-toolbar__right">
          <el-button type="primary" :icon="Plus" @click="openCreate">新增会员</el-button>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="phone" label="手机号" min-width="140" show-overflow-tooltip />
          <el-table-column label="姓名" min-width="120" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.name || "-" }}</template>
          </el-table-column>
          <el-table-column label="身份证号" min-width="180" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.idCardNo || "-" }}</template>
          </el-table-column>
          <el-table-column label="实名认证" min-width="100" align="center">
            <template #default="scope">
              <el-tag :type="verifyTagType(scope.row.idCardNoVerify)" size="small">
                {{ verifyText(scope.row.idCardNoVerify) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" min-width="90" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === MemberStatus.ENABLED ? 'success' : 'danger'" size="small">
                {{ scope.row.status === MemberStatus.ENABLED ? "正常" : "禁用" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="180" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openEdit(scope.row as MemberRecord)">编辑</el-button>
              <el-button
                v-if="scope.row.status === MemberStatus.ENABLED"
                type="danger"
                link
                size="small"
                @click="handleDisable(scope.row as MemberRecord)"
                >禁用</el-button
              >
              <el-button
                v-else
                type="success"
                link
                size="small"
                @click="handleEnable(scope.row as MemberRecord)"
                >启用</el-button
              >
              <el-button type="danger" link size="small" @click="handleDelete(scope.row as MemberRecord)">删除</el-button>
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
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="请输入 11 位手机号"
            maxlength="11"
            :disabled="!!editingId"
          />
          <div v-if="editingId" class="member-form__hint">手机号为登录账号，建档后不可修改</div>
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="选填" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCardNo">
          <el-input v-model="form.idCardNo" placeholder="选填" maxlength="18" />
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
import MemberAPI, { MemberStatus, IdCardVerifyStatus, type MemberRecord, type MemberPageQuery } from "@/api/member";

/** 实名认证状态文案（后端 idCardVerify：0未验证 1通过 2未通过） */
function verifyText(status?: number): string {
  if (status === IdCardVerifyStatus.VERIFIED) {
    return "已认证";
  }
  if (status === IdCardVerifyStatus.REJECTED) {
    return "认证未通过";
  }
  return "未认证";
}

function verifyTagType(status?: number): "success" | "danger" | "info" {
  if (status === IdCardVerifyStatus.VERIFIED) {
    return "success";
  }
  if (status === IdCardVerifyStatus.REJECTED) {
    return "danger";
  }
  return "info";
}

/** 查询与列表状态 */
const loading = ref(false);
const list = ref<MemberRecord[]>([]);
const total = ref(0);
const query = ref<MemberPageQuery>({ page: 1, pageSize: 10 });

/** 组装查询参数：剔除空字符串/空值，仅提交有效过滤条件 */
function buildParams(): MemberPageQuery {
  const { page, pageSize, ...filters } = query.value;
  const params: MemberPageQuery = { page, pageSize };
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
    const res = await MemberAPI.page(buildParams());
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

const createDefaultForm = (): MemberRecord => ({
  phone: "",
  name: "",
  idCardNo: "",
});
const form = reactive<MemberRecord>(createDefaultForm());

const rules: FormRules<MemberRecord> = {
  phone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    { pattern: /^1\d{10}$/, message: "请输入正确的 11 位手机号", trigger: "blur" },
  ],
  idCardNo: [{ pattern: /(^$)|(^\d{17}[\dXx]$)/, message: "身份证号格式不正确", trigger: "blur" }],
};

const formTitle = computed(() => (editingId.value ? "编辑会员" : "新增会员"));

function openCreate() {
  editingId.value = undefined;
  Object.assign(form, createDefaultForm());
  formVisible.value = true;
}

async function openEdit(row: MemberRecord) {
  const detail = await MemberAPI.getDetail(row.id!);
  editingId.value = row.id;
  Object.assign(form, createDefaultForm(), detail);
  formVisible.value = true;
}

/** 提交体只取表单字段，避免 id/status 等回显字段透传到后端 */
function buildPayload(): MemberRecord {
  return {
    phone: form.phone.trim(),
    name: form.name?.trim() || undefined,
    idCardNo: form.idCardNo?.trim() || undefined,
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
      await MemberAPI.update(editingId.value, buildPayload());
      ElMessage.success("更新成功");
    } else {
      await MemberAPI.save(buildPayload());
      ElMessage.success("新增成功");
    }
    formVisible.value = false;
    loadList();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "保存失败");
  } finally {
    saving.value = false;
  }
}

function resetForm() {
  formRef.value?.resetFields();
  Object.assign(form, createDefaultForm());
  editingId.value = undefined;
}

async function handleDisable(row: MemberRecord) {
  try {
    await ElMessageBox.confirm(
      `确认禁用会员「${row.name || row.phone}」？禁用后该账号将无法在客户端下单及预估价，历史订单不受影响，可随时重新启用。`,
      "禁用确认",
      { type: "warning", confirmButtonText: "禁用", cancelButtonText: "取消" },
    );
  } catch {
    return;
  }
  try {
    await MemberAPI.disable(row.id!);
    ElMessage.success("已禁用");
    // 筛选"正常"状态时，禁用后记录会从结果消失；末页仅剩一条则回退一页，避免停在空页
    if (query.value.status === MemberStatus.ENABLED && list.value.length === 1 && query.value.page > 1) {
      query.value.page -= 1;
    }
    loadList();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "禁用失败");
  }
}

async function handleEnable(row: MemberRecord) {
  try {
    await ElMessageBox.confirm(`确认重新启用会员「${row.name || row.phone}」？启用后将恢复下单资格。`, "启用确认", {
      type: "info",
      confirmButtonText: "启用",
      cancelButtonText: "取消",
    });
  } catch {
    return;
  }
  try {
    await MemberAPI.enable(row.id!);
    ElMessage.success("已启用");
    loadList();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "启用失败");
  }
}

async function handleDelete(row: MemberRecord) {
  try {
    await ElMessageBox.confirm(
      `确认删除会员「${row.name || row.phone}」？删除为逻辑删除，列表不再展示，操作不可恢复。`,
      "删除确认",
      { type: "warning", confirmButtonText: "删除", cancelButtonText: "取消" },
    );
  } catch {
    return;
  }
  try {
    await MemberAPI.remove(row.id!);
    ElMessage.success("已删除");
    // 末页仅剩一条时回退一页，避免停在空页
    if (list.value.length === 1 && query.value.page > 1) {
      query.value.page -= 1;
    }
    loadList();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "删除失败");
  }
}

onMounted(() => {
  loadList();
});
</script>

<style scoped lang="scss">
.member-form__hint {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--el-text-color-secondary);
}
</style>
