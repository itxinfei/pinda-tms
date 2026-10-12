<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 + 新增 -->
      <div class="page-toolbar">
        <div class="page-toolbar__left">
          <el-form :model="query" inline>
            <el-form-item label="线路名称" prop="name">
              <el-input v-model="query.name" placeholder="线路名称" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item label="线路编号" prop="lineNumber">
              <el-input v-model="query.lineNumber" placeholder="线路编号" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item label="线路类型" prop="transportLineTypeId">
              <el-select v-model="query.transportLineTypeId" placeholder="全部类型" clearable filterable style="width: 160px">
                <el-option
                  v-for="item in lineTypeOptions"
                  :key="item.id"
                  :value="item.id"
                  :label="item.name"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="page-toolbar__right">
          <el-button type="primary" :icon="Plus" @click="openCreate">新增线路</el-button>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="name" label="线路名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="lineNumber" label="线路编号" min-width="120" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.lineNumber || "-" }}</template>
          </el-table-column>
          <el-table-column label="起始 → 目的机构" min-width="240" show-overflow-tooltip>
            <template #default="scope">{{ routeText(scope.row as TransportLineRecord) }}</template>
          </el-table-column>
          <el-table-column label="线路类型" min-width="120">
            <template #default="scope">{{ scope.row.transportLineType?.name || "-" }}</template>
          </el-table-column>
          <el-table-column prop="distance" label="距离(km)" min-width="100" align="right">
            <template #default="scope">{{ scope.row.distance ?? "-" }}</template>
          </el-table-column>
          <el-table-column prop="cost" label="成本(元)" min-width="100" align="right">
            <template #default="scope">{{ scope.row.cost ?? "-" }}</template>
          </el-table-column>
          <el-table-column prop="estimatedTime" label="预计时间(分钟)" min-width="120" align="right">
            <template #default="scope">{{ scope.row.estimatedTime ?? "-" }}</template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="150" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openDetail(scope.row as TransportLineRecord)">详情</el-button>
              <el-button type="primary" link size="small" @click="openEdit(scope.row as TransportLineRecord)">编辑</el-button>
              <el-button type="danger" link size="small" @click="handleDelete(scope.row as TransportLineRecord)">删除</el-button>
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
    <el-dialog v-model="formVisible" :title="formTitle" width="600px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="线路名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入线路名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="线路编号" prop="lineNumber">
          <el-input v-model="form.lineNumber" placeholder="选填，请输入线路编号" maxlength="32" />
        </el-form-item>
        <el-form-item label="所属机构" prop="agencyId">
          <el-tree-select
            v-model="form.agencyId"
            :data="orgTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            filterable
            :render-after-expand="false"
            placeholder="请选择所属机构"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="起始机构" prop="startOrgId">
          <el-tree-select
            v-model="form.startOrgId"
            :data="orgTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            filterable
            clearable
            :render-after-expand="false"
            placeholder="选填，请选择起始机构"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="目的机构" prop="endOrgId">
          <el-tree-select
            v-model="form.endOrgId"
            :data="orgTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            filterable
            clearable
            :render-after-expand="false"
            placeholder="选填，请选择目的机构"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="线路类型" prop="lineTypeId">
          <el-select v-model="form.lineTypeId" placeholder="选填，请选择线路类型" clearable filterable style="width: 100%">
            <el-option
              v-for="item in lineTypeOptions"
              :key="item.id"
              :value="item.id"
              :label="item.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="距离(km)" prop="distance">
          <el-input-number
            v-model="form.distance"
            :precision="2"
            :min="0"
            :step="1"
            controls-position="right"
            placeholder="选填"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="成本(元)" prop="cost">
          <el-input-number
            v-model="form.cost"
            :precision="2"
            :min="0"
            :step="100"
            controls-position="right"
            placeholder="选填"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="预计时间(分钟)" prop="estimatedTime">
          <el-input-number
            v-model="form.estimatedTime"
            :precision="0"
            :min="0"
            :step="30"
            controls-position="right"
            placeholder="选填，整数分钟"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 线路详情 -->
    <el-drawer v-model="detailVisible" title="线路详情" size="640px">
      <div v-loading="detailLoading" class="transport-line-detail">
        <template v-if="detail">
          <el-descriptions title="基本信息" :column="2" border>
            <el-descriptions-item label="线路名称">{{ detail.name || "-" }}</el-descriptions-item>
            <el-descriptions-item label="线路编号">{{ detail.lineNumber || "-" }}</el-descriptions-item>
            <el-descriptions-item label="线路类型">{{ detail.transportLineType?.name || "-" }}</el-descriptions-item>
            <el-descriptions-item label="类型编码">{{ detail.transportLineType?.typeNumber || "-" }}</el-descriptions-item>
            <el-descriptions-item label="距离(km)">{{ detail.distance ?? "-" }}</el-descriptions-item>
            <el-descriptions-item label="成本(元)">{{ detail.cost ?? "-" }}</el-descriptions-item>
            <el-descriptions-item label="预计时间(分钟)">{{ detail.estimatedTime ?? "-" }}</el-descriptions-item>
            <el-descriptions-item label="所属机构">{{ detail.agency?.name || "-" }}</el-descriptions-item>
          </el-descriptions>

          <el-descriptions title="起止机构" :column="2" border class="transport-line-detail__block">
            <el-descriptions-item label="起始地机构">{{ detail.startAgency?.name || "-" }}</el-descriptions-item>
            <el-descriptions-item label="目的地机构">{{ detail.endAgency?.name || "-" }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue";
import { Plus } from "@element-plus/icons-vue";
import type { FormInstance, FormRules } from "element-plus";
import TransportLineAPI from "@/api/transport-line";
import type {
  TransportLineRecord,
  TransportLinePageQuery,
  TransportLineSavePayload,
} from "@/api/transport-line";
import CommonSimpleAPI from "@/api/common-simple";
import type { TransportLineTypeSimple } from "@/api/common-simple";
import OrgAPI from "@/api/org";
import type { OrgNode } from "@/api/org";

/** 线路类型下拉 */
const lineTypeOptions = ref<TransportLineTypeSimple[]>([]);

/** 机构树（所属/起始/目的三个选择器共用；节点 id 为 number，提交时转 string） */
const orgTree = ref<OrgNode[]>([]);

/** 起始→目的机构 */
const routeText = (row: TransportLineRecord) => {
  const start = row.startAgency?.name;
  const end = row.endAgency?.name;
  if (!start && !end) return "-";
  return `${start ?? ""} → ${end ?? ""}`;
};

/** 查询与列表状态 */
const loading = ref(false);
const list = ref<TransportLineRecord[]>([]);
const total = ref(0);
const query = ref<TransportLinePageQuery>({ page: 1, pageSize: 10 });

/** 组装查询参数：剔除空字符串/空值，仅提交有效过滤条件 */
function buildParams(): TransportLinePageQuery {
  const { page, pageSize, ...filters } = query.value;
  const params: TransportLinePageQuery = { page, pageSize };
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
    const res = await TransportLineAPI.page(buildParams());
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
interface LineForm {
  name: string;
  lineNumber: string;
  agencyId?: number;
  startOrgId?: number;
  endOrgId?: number;
  lineTypeId?: string;
  distance?: number;
  cost?: number;
  /** 预计时间，整数分钟 */
  estimatedTime?: number;
}

const formVisible = ref(false);
const saving = ref(false);
const editingId = ref<string>();
const formRef = ref<FormInstance>();

const createDefaultForm = (): LineForm => ({
  name: "",
  lineNumber: "",
  agencyId: undefined,
  startOrgId: undefined,
  endOrgId: undefined,
  lineTypeId: undefined,
  distance: undefined,
  cost: undefined,
  estimatedTime: undefined,
});
const form = reactive<LineForm>(createDefaultForm());

const rules: FormRules<LineForm> = {
  name: [{ required: true, message: "请输入线路名称", trigger: "blur" }],
  agencyId: [{ required: true, message: "请选择所属机构", trigger: "change" }],
};

const formTitle = computed(() => (editingId.value ? "编辑线路" : "新增线路"));

/** 详情机构 id 为 string，树 node-key 为 number，统一转换后回填 */
const toOrgNodeId = (id?: string) => (id ? Number(id) : undefined);

function openCreate() {
  editingId.value = undefined;
  Object.assign(form, createDefaultForm());
  formVisible.value = true;
}

async function openEdit(row: TransportLineRecord) {
  // 编辑前拉详情：列表项只回填了机构名，外键 id 需从详情取
  const detail = await TransportLineAPI.getDetail(row.id);
  editingId.value = row.id;
  Object.assign(form, createDefaultForm(), {
    name: detail.name ?? "",
    lineNumber: detail.lineNumber ?? "",
    agencyId: toOrgNodeId(detail.agency?.id),
    startOrgId: toOrgNodeId(detail.startAgency?.id),
    endOrgId: toOrgNodeId(detail.endAgency?.id),
    lineTypeId: detail.transportLineType?.id,
    distance: detail.distance ?? undefined,
    cost: detail.cost ?? undefined,
    estimatedTime: detail.estimatedTime ?? undefined,
  });
  formVisible.value = true;
}

/** 数值选填字段：undefined/null 提交 null（实体 FieldStrategy.ALWAYS 清空），否则原值 */
const numberOrNull = (value?: number) => (value === undefined || value === null ? null : value);

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  saving.value = true;
  try {
    // 所属机构必填且编辑必须原样回传；起始/目的机构与类型选填，清空显式传 null
    const orgRef = (id?: number) => (id === undefined || id === null ? null : { id: String(id) });
    const payload: TransportLineSavePayload = {
      name: form.name.trim(),
      lineNumber: form.lineNumber.trim() || null,
      agency: orgRef(form.agencyId),
      startAgency: orgRef(form.startOrgId),
      endAgency: orgRef(form.endOrgId),
      transportLineType: form.lineTypeId ? { id: form.lineTypeId } : null,
      distance: numberOrNull(form.distance),
      cost: numberOrNull(form.cost),
      estimatedTime: numberOrNull(form.estimatedTime),
    };
    if (editingId.value) {
      await TransportLineAPI.update(editingId.value, payload);
      ElMessage.success("更新成功");
    } else {
      await TransportLineAPI.save(payload);
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

async function handleDelete(row: TransportLineRecord) {
  // BFF DELETE 实为软停用 status=0：线路分页只查启用项，停用后列表不可见且无启用入口
  try {
    await ElMessageBox.confirm(
      `确认删除线路「${row.name}」？删除后该线路将停用并从档案列表消失，当前无恢复入口；线路下存在关联车次时无法删除。`,
      "删除确认",
      { type: "warning", confirmButtonText: "删除", cancelButtonText: "取消" },
    );
  } catch {
    return;
  }
  await TransportLineAPI.remove(row.id);
  ElMessage.success("删除成功");
  // 末页仅剩一条时回退一页，避免停在空页
  if (list.value.length === 1 && query.value.page > 1) {
    query.value.page -= 1;
  }
  loadList();
}

/** 详情 */
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<TransportLineRecord>();

async function openDetail(row: TransportLineRecord) {
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = undefined;
  try {
    detail.value = await TransportLineAPI.getDetail(row.id);
  } finally {
    detailLoading.value = false;
  }
}

onMounted(() => {
  loadList();
  CommonSimpleAPI.transportLineTypeOptions().then((res) => {
    lineTypeOptions.value = res ?? [];
  });
  OrgAPI.getTree().then((res) => {
    orgTree.value = res ?? [];
  });
});
</script>

<style lang="scss" scoped>
/* 线路详情抽屉：容器作为 loading 遮罩定位上下文；区块留白 */
.transport-line-detail {
  position: relative;
  min-height: 100%;
}

.transport-line-detail__block {
  margin-top: 16px;
}
</style>
