<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <el-form class="page-search" :model="query" inline>
        <el-form-item label="编码" prop="code">
          <el-input v-model="query.code" placeholder="承运商编码" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="query.name" placeholder="承运商名称（模糊）" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="(label, value) in CARRIER_STATUS_LABELS" :key="value" :value="value" :label="label" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="page-toolbar">
        <el-button v-hasPerm="'base:carrier:save'" type="primary" @click="openForm()">新增承运商</el-button>
        <el-button v-hasPerm="'base:carrier:capacity'" @click="capacityVisible = true">运力池查询</el-button>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="code" label="编码" width="120" show-overflow-tooltip />
          <el-table-column prop="name" label="承运商名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="contactName" label="联系人" width="100">
            <template #default="scope">{{ scope.row.contactName || "-" }}</template>
          </el-table-column>
          <el-table-column prop="contactPhone" label="联系电话" width="130">
            <template #default="scope">{{ scope.row.contactPhone || "-" }}</template>
          </el-table-column>
          <el-table-column prop="licenseNo" label="运输许可证号" min-width="150" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.licenseNo || "-" }}</template>
          </el-table-column>
          <el-table-column label="许可证有效期" width="120" align="center">
            <template #default="scope">
              <span :class="{ 'license-expiring': isExpiring(scope.row as CarrierRecord) }">
                {{ scope.row.licenseExpiry ?? "-" }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="结算方式" width="90" align="center">
            <template #default="scope">{{ SETTLE_TYPE_LABELS[scope.row.settleType] ?? scope.row.settleType }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.status)" effect="plain">
                {{ CARRIER_STATUS_LABELS[scope.row.status] ?? scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
          <el-table-column label="操作" width="200" fixed="right" align="center">
            <template #default="scope">
              <el-button v-hasPerm="'base:carrier:detail'" link type="primary" @click="openDetail(scope.row as CarrierRecord)">
                详情
              </el-button>
              <el-button v-hasPerm="'base:carrier:save'" link type="primary" @click="openForm(scope.row as CarrierRecord)">
                编辑
              </el-button>
              <el-dropdown
                v-if="scope.row.status === CarrierStatus.ACTIVE"
                v-hasPerm="'base:carrier:disable'"
                class="status-dropdown"
                @command="(cmd: number) => handleChangeStatus(scope.row as CarrierRecord, cmd)"
              >
                <el-button link type="warning">状态</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :command="CarrierStatus.SUSPENDED">暂停合作</el-dropdown-item>
                    <el-dropdown-item :command="CarrierStatus.BLACKLIST">拉黑</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button
                v-else
                v-hasPerm="'base:carrier:disable'"
                link
                type="success"
                @click="handleChangeStatus(scope.row as CarrierRecord, CarrierStatus.ACTIVE)"
              >
                恢复合作
              </el-button>
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

    <!-- 新增/编辑承运商弹窗（写操作居中弹窗 + append-to-body） -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑承运商' : '新增承运商'" width="560px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" placeholder="租户内唯一，如 WL-001" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="承运商名称" prop="name">
          <el-input v-model="form.name" placeholder="主体名称" />
        </el-form-item>
        <el-form-item label="运输许可证号" prop="licenseNo">
          <el-input v-model="form.licenseNo" placeholder="道路运输许可证号" />
        </el-form-item>
        <el-form-item label="许可证有效期" prop="licenseExpiry">
          <el-date-picker v-model="form.licenseExpiry" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" />
        </el-form-item>
        <el-form-item label="结算方式" prop="settleType">
          <el-select v-model="form.settleType" style="width: 100%">
            <el-option v-for="(label, value) in SETTLE_TYPE_LABELS" :key="value" :value="value" :label="label" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉（读操作居右，含挂靠车辆/司机管理） -->
    <el-drawer v-model="detailVisible" :title="`承运商详情 · ${detail?.carrier?.name ?? ''}`" size="720px" append-to-body>
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="编码">{{ detail.carrier.code }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detail.carrier.status)" effect="plain">
              {{ CARRIER_STATUS_LABELS[detail.carrier.status] ?? detail.carrier.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="联系人">{{ detail.carrier.contactName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.carrier.contactPhone || "-" }}</el-descriptions-item>
          <el-descriptions-item label="运输许可证号">{{ detail.carrier.licenseNo || "-" }}</el-descriptions-item>
          <el-descriptions-item label="许可证有效期">
            <span :class="{ 'license-expiring': isExpiring(detail.carrier) }">{{ detail.carrier.licenseExpiry ?? "-" }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="结算方式">
            {{ SETTLE_TYPE_LABELS[detail.carrier.settleType] ?? detail.carrier.settleType }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.carrier.createTime ?? "-" }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.carrier.remark || "-" }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs v-model="activeTab" class="detail-tabs">
          <el-tab-pane label="挂靠车辆" name="trucks">
            <div class="tab-toolbar">
              <el-button
                v-hasPerm="'base:carrier:save'"
                type="primary"
                size="small"
                :disabled="detail.carrier.status === CarrierStatus.BLACKLIST"
                @click="openTruckForm()"
              >
                新增挂靠车辆
              </el-button>
            </div>
            <el-table :data="detail.trucks" border size="small" max-height="380">
              <el-table-column prop="licensePlate" label="车牌号" width="110" />
              <el-table-column label="车型" min-width="100">
                <template #default="scope">{{ truckTypeName(scope.row.truckTypeId) }}</template>
              </el-table-column>
              <el-table-column prop="allowableLoad" label="准载(kg)" width="100" align="right" />
              <el-table-column prop="allowableVolume" label="准载(m³)" width="100" align="right" />
              <el-table-column label="状态" width="80" align="center">
                <template #default="scope">
                  <el-tag :type="scope.row.status === 1 ? 'success' : scope.row.status === 2 ? 'warning' : 'info'" effect="plain">
                    {{ CARRIER_MEMBER_STATUS_LABELS[scope.row.status] ?? scope.row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" fixed="right" align="center">
                <template #default="scope">
                  <el-button v-hasPerm="'base:carrier:save'" link type="primary" @click="openTruckForm(scope.row as CarrierTruckRecord)">
                    编辑
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="挂靠司机" name="drivers">
            <div class="tab-toolbar">
              <el-button
                v-hasPerm="'base:carrier:save'"
                type="primary"
                size="small"
                :disabled="detail.carrier.status === CarrierStatus.BLACKLIST"
                @click="openDriverForm()"
              >
                新增挂靠司机
              </el-button>
            </div>
            <el-table :data="detail.drivers" border size="small" max-height="380">
              <el-table-column prop="name" label="姓名" width="110" />
              <el-table-column prop="phone" label="联系电话" width="130" />
              <el-table-column prop="licenseNo" label="驾驶证号" min-width="160" show-overflow-tooltip />
              <el-table-column label="状态" width="80" align="center">
                <template #default="scope">
                  <el-tag :type="scope.row.status === 1 ? 'success' : scope.row.status === 2 ? 'warning' : 'info'" effect="plain">
                    {{ CARRIER_MEMBER_STATUS_LABELS[scope.row.status] ?? scope.row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" fixed="right" align="center">
                <template #default="scope">
                  <el-button v-hasPerm="'base:carrier:save'" link type="primary" @click="openDriverForm(scope.row as CarrierDriverRecord)">
                    编辑
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>

    <!-- 挂靠车辆弹窗 -->
    <el-dialog v-model="truckFormVisible" :title="truckForm.id ? '编辑挂靠车辆' : '新增挂靠车辆'" width="480px" append-to-body>
      <el-form ref="truckFormRef" :model="truckForm" :rules="truckRules" label-width="100px">
        <el-form-item label="车牌号" prop="licensePlate">
          <el-input v-model="truckForm.licensePlate" placeholder="如 冀A12345" />
        </el-form-item>
        <el-form-item label="车型" prop="truckTypeId">
          <el-select v-model="truckForm.truckTypeId" placeholder="选填" clearable style="width: 100%">
            <el-option v-for="t in truckTypes" :key="t.id" :value="t.id" :label="t.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="准载重量(kg)" prop="allowableLoad">
          <el-input-number v-model="truckForm.allowableLoad" :min="0" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="准载体积(m³)" prop="allowableVolume">
          <el-input-number v-model="truckForm.allowableVolume" :min="0" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="truckForm.status" style="width: 100%">
            <el-option v-for="(label, value) in CARRIER_MEMBER_STATUS_LABELS" :key="value" :value="value" :label="label" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="truckFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveTruck">保存</el-button>
      </template>
    </el-dialog>

    <!-- 挂靠司机弹窗 -->
    <el-dialog v-model="driverFormVisible" :title="driverForm.id ? '编辑挂靠司机' : '新增挂靠司机'" width="480px" append-to-body>
      <el-form ref="driverFormRef" :model="driverForm" :rules="driverRules" label-width="100px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="driverForm.name" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="driverForm.phone" />
        </el-form-item>
        <el-form-item label="驾驶证号" prop="licenseNo">
          <el-input v-model="driverForm.licenseNo" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="driverForm.status" style="width: 100%">
            <el-option v-for="(label, value) in CARRIER_MEMBER_STATUS_LABELS" :key="value" :value="value" :label="label" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="driverFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveDriver">保存</el-button>
      </template>
    </el-dialog>

    <!-- 运力池查询抽屉（读操作居右） -->
    <el-drawer v-model="capacityVisible" title="外协运力池查询" size="560px" append-to-body>
      <el-form inline @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="capacityQuery.keyword" placeholder="车牌/承运商名/编码" clearable style="width: 180px" @keyup.enter="loadCapacity" />
        </el-form-item>
        <el-form-item label="车型">
          <el-select v-model="capacityQuery.truckTypeId" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="t in truckTypes" :key="t.id" :value="t.id" :label="t.name" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadCapacity">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="capacityLoading" :data="capacityList" border size="small">
        <el-table-column prop="licensePlate" label="车牌号" width="110" />
        <el-table-column prop="carrierName" label="所属承运商" min-width="140" show-overflow-tooltip />
        <el-table-column label="车型" min-width="90">
          <template #default="scope">{{ truckTypeName(scope.row.truckTypeId) }}</template>
        </el-table-column>
        <el-table-column prop="allowableLoad" label="准载(kg)" width="95" align="right" />
        <el-table-column prop="allowableVolume" label="准载(m³)" width="95" align="right" />
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import CarrierAPI, {
  CARRIER_MEMBER_STATUS_LABELS,
  CARRIER_STATUS_LABELS,
  CarrierStatus,
  SETTLE_TYPE_LABELS,
  SettleType,
  type CarrierDriverRecord,
  type CarrierRecord,
  type CarrierTruckRecord,
} from "@/api/carrier";
import TruckTypeAPI, { type TruckTypeRecord } from "@/api/truck-type";

/** 许可证到期预警天数（默认 30 天，仅前端提示，不阻断） */
const LICENSE_EXPIRY_WARN_DAYS = 30;

const loading = ref(false);
const saving = ref(false);
const list = ref<CarrierRecord[]>([]);
const total = ref(0);
const query = reactive({ page: 1, pageSize: 10, code: "", name: "", status: undefined as number | undefined });

const formVisible = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<Partial<CarrierRecord>>({});

const detailVisible = ref(false);
const detail = ref<{ carrier: CarrierRecord; trucks: CarrierTruckRecord[]; drivers: CarrierDriverRecord[] } | null>(null);
const activeTab = ref("trucks");

const truckFormVisible = ref(false);
const truckFormRef = ref<FormInstance>();
const truckForm = reactive<Partial<CarrierTruckRecord>>({});

const driverFormVisible = ref(false);
const driverFormRef = ref<FormInstance>();
const driverForm = reactive<Partial<CarrierDriverRecord>>({});

const capacityVisible = ref(false);
const capacityLoading = ref(false);
const capacityList = ref<Awaited<ReturnType<typeof CarrierAPI.capacity>>>([]);
const capacityQuery = reactive<{ keyword: string; truckTypeId: number | string | undefined }>({
  keyword: "",
  truckTypeId: undefined,
});

const truckTypes = ref<(TruckTypeRecord & { id: string })[]>([]);

const rules: FormRules = {
  code: [{ required: true, message: "请输入承运商编码", trigger: "blur" }],
  name: [{ required: true, message: "请输入承运商名称", trigger: "blur" }],
  settleType: [{ required: true, message: "请选择结算方式", trigger: "change" }],
};
const truckRules: FormRules = {
  licensePlate: [{ required: true, message: "请输入车牌号", trigger: "blur" }],
};
const driverRules: FormRules = {
  name: [{ required: true, message: "请输入司机姓名", trigger: "blur" }],
};

function statusTagType(status: number): "success" | "warning" | "danger" {
  return status === CarrierStatus.ACTIVE ? "success" : status === CarrierStatus.SUSPENDED ? "warning" : "danger";
}

function isExpiring(row: CarrierRecord): boolean {
  if (!row.licenseExpiry) return false;
  const expiry = new Date(row.licenseExpiry).getTime();
  if (Number.isNaN(expiry)) return false;
  const diffDays = (expiry - Date.now()) / 86400000;
  return diffDays < LICENSE_EXPIRY_WARN_DAYS;
}

function truckTypeName(typeId: number | string | null | undefined): string {
  if (typeId == null) return "-";
  const hit = truckTypes.value.find((t) => String(t.id) === String(typeId));
  return hit?.name ?? String(typeId);
}

async function loadList() {
  loading.value = true;
  try {
    const res = await CarrierAPI.page({ ...query });
    list.value = res.records ?? [];
    total.value = res.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.page = 1;
  loadList();
}

function handleReset() {
  query.code = "";
  query.name = "";
  query.status = undefined;
  handleSearch();
}

function openForm(row?: CarrierRecord) {
  Object.assign(form, row ?? {});
  form.settleType = row?.settleType ?? SettleType.CASH;
  formVisible.value = true;
}

async function handleSave() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    await CarrierAPI.save({ ...form });
    ElMessage.success("保存成功");
    formVisible.value = false;
    loadList();
  } finally {
    saving.value = false;
  }
}

async function openDetail(row: CarrierRecord) {
  const res = await CarrierAPI.detail(row.id);
  detail.value = res ?? null;
  activeTab.value = "trucks";
  detailVisible.value = true;
}

async function handleChangeStatus(row: CarrierRecord, status: CarrierStatus) {
  const action = status === CarrierStatus.ACTIVE ? "恢复合作" : status === CarrierStatus.SUSPENDED ? "暂停合作" : "拉黑";
  await ElMessageBox.confirm(`确认对承运商「${row.name}」执行「${action}」？`, "状态变更", { type: "warning" });
  await CarrierAPI.changeStatus(row.id, status);
  ElMessage.success(`${action}成功`);
  loadList();
  if (detail.value?.carrier.id === row.id) {
    detail.value.carrier.status = status;
  }
}

function openTruckForm(row?: CarrierTruckRecord) {
  Object.assign(truckForm, row ?? { carrierId: detail.value?.carrier.id, status: 1 });
  truckFormVisible.value = true;
}

async function handleSaveTruck() {
  await truckFormRef.value?.validate();
  saving.value = true;
  try {
    await CarrierAPI.saveTruck({ ...truckForm, carrierId: detail.value?.carrier.id });
    ElMessage.success("保存成功");
    truckFormVisible.value = false;
    if (detail.value) {
      const res = await CarrierAPI.detail(detail.value.carrier.id);
      detail.value = res ?? detail.value;
    }
  } finally {
    saving.value = false;
  }
}

function openDriverForm(row?: CarrierDriverRecord) {
  Object.assign(driverForm, row ?? { carrierId: detail.value?.carrier.id, status: 1 });
  driverFormVisible.value = true;
}

async function handleSaveDriver() {
  await driverFormRef.value?.validate();
  saving.value = true;
  try {
    await CarrierAPI.saveDriver({ ...driverForm, carrierId: detail.value?.carrier.id });
    ElMessage.success("保存成功");
    driverFormVisible.value = false;
    if (detail.value) {
      const res = await CarrierAPI.detail(detail.value.carrier.id);
      detail.value = res ?? detail.value;
    }
  } finally {
    saving.value = false;
  }
}

async function loadCapacity() {
  capacityLoading.value = true;
  try {
    const res = await CarrierAPI.capacity({
      keyword: capacityQuery.keyword || undefined,
      truckTypeId: capacityQuery.truckTypeId,
    });
    capacityList.value = res ?? [];
  } finally {
    capacityLoading.value = false;
  }
}

async function loadTruckTypes() {
  try {
    const res = await TruckTypeAPI.page({ page: 1, pageSize: 100 });
    truckTypes.value = (res.items ?? []).filter((t): t is TruckTypeRecord & { id: string } => t.id != null);
  } catch {
    truckTypes.value = [];
  }
}

onMounted(() => {
  loadList();
  loadTruckTypes();
});
</script>

<style scoped>
.status-dropdown {
  margin-left: 12px;
  vertical-align: middle;
}
.license-expiring {
  color: var(--el-color-danger);
}
.detail-tabs {
  margin-top: 16px;
}
.tab-toolbar {
  margin-bottom: 8px;
}
</style>
