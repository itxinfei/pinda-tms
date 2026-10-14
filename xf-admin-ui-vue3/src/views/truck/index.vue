<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 + 新增 -->
      <div class="page-toolbar">
        <div class="page-toolbar__left">
          <el-form :model="query" inline>
            <el-form-item label="车牌" prop="licensePlate">
              <el-input v-model="query.licensePlate" placeholder="请输入车牌" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item label="车辆类型" prop="truckTypeId">
              <el-select v-model="query.truckTypeId" placeholder="全部类型" clearable filterable style="width: 160px">
                <el-option
                  v-for="item in truckTypeOptions"
                  :key="item.id"
                  :value="item.id"
                  :label="item.name"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="车队" prop="fleetId">
              <el-select v-model="query.fleetId" placeholder="全部车队" clearable filterable style="width: 160px">
                <el-option
                  v-for="item in fleetOptions"
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
          <el-button type="primary" :icon="Plus" @click="openCreate">新增车辆</el-button>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="licensePlate" label="车牌" min-width="120" show-overflow-tooltip />
          <el-table-column prop="brand" label="品牌" min-width="120" show-overflow-tooltip />
          <el-table-column label="车辆类型" min-width="120">
            <template #default="scope">{{ scope.row.truckType?.name || "-" }}</template>
          </el-table-column>
          <el-table-column label="车队" min-width="120">
            <template #default="scope">{{ scope.row.fleet?.name || "-" }}</template>
          </el-table-column>
          <el-table-column label="准载重(t)" min-width="100" align="right">
            <template #default="scope">{{ formatLoadT(scope.row.allowableLoad) }}</template>
          </el-table-column>
          <el-table-column prop="allowableVolume" label="准体积(m³)" min-width="110" align="right">
            <template #default="scope">{{ scope.row.allowableVolume ?? "-" }}</template>
          </el-table-column>
          <el-table-column prop="deviceGpsId" label="GPS 设备" min-width="120" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.deviceGpsId || "-" }}</template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="150" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openDetail(scope.row as TruckRecord)">详情</el-button>
              <el-button type="primary" link size="small" @click="openEdit(scope.row as TruckRecord)">编辑</el-button>
              <el-button type="danger" link size="small" @click="handleDelete(scope.row as TruckRecord)">删除</el-button>
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
    <el-dialog v-model="formVisible" :title="formTitle" width="560px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="车牌" prop="licensePlate">
          <el-input v-model="form.licensePlate" placeholder="请输入车牌" maxlength="20" />
        </el-form-item>
        <el-form-item label="品牌" prop="brand">
          <el-input v-model="form.brand" placeholder="选填，请输入品牌" maxlength="32" />
        </el-form-item>
        <el-form-item label="车辆类型" prop="truckTypeId">
          <el-select v-model="form.truckTypeId" placeholder="选填，请选择车辆类型" clearable filterable style="width: 100%">
            <el-option
              v-for="item in truckTypeOptions"
              :key="item.id"
              :value="item.id"
              :label="item.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属车队" prop="fleetId">
          <el-select v-model="form.fleetId" placeholder="选填，请选择所属车队" clearable filterable style="width: 100%">
            <el-option
              v-for="item in fleetOptions"
              :key="item.id"
              :value="item.id"
              :label="item.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="GPS 设备号" prop="deviceGpsId">
          <el-input v-model="form.deviceGpsId" placeholder="选填，请输入 GPS 设备号" maxlength="64" />
        </el-form-item>
        <el-form-item label="准载重量(t)" prop="allowableLoadT">
          <el-input-number
            v-model="form.allowableLoadT"
            :precision="2"
            :min="0"
            :step="1"
            controls-position="right"
            placeholder="选填"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="准载体积(m³)" prop="allowableVolume">
          <el-input-number
            v-model="form.allowableVolume"
            :precision="2"
            :min="0"
            :step="1"
            controls-position="right"
            placeholder="选填"
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

    <!-- 车辆详情 -->
    <el-drawer v-model="detailVisible" title="车辆详情" size="640px">
      <div v-loading="detailLoading" class="truck-detail">
        <template v-if="detail">
          <el-descriptions title="基本信息" :column="2" border>
            <el-descriptions-item label="车牌">{{ detail.licensePlate || "-" }}</el-descriptions-item>
            <el-descriptions-item label="品牌">{{ detail.brand || "-" }}</el-descriptions-item>
            <el-descriptions-item label="车辆类型">{{ detail.truckType?.name || "-" }}</el-descriptions-item>
            <el-descriptions-item label="所属车队">{{ detail.fleet?.name || "-" }}</el-descriptions-item>
            <el-descriptions-item label="所属机构">{{ detail.agency?.name || "-" }}</el-descriptions-item>
            <el-descriptions-item label="GPS 设备">{{ detail.deviceGpsId || "-" }}</el-descriptions-item>
            <el-descriptions-item label="准载重量(t)">{{ formatLoadT(detail.allowableLoad) }}</el-descriptions-item>
            <el-descriptions-item label="准载体积(m³)">{{ detail.allowableVolume ?? "-" }}</el-descriptions-item>
            <el-descriptions-item label="工作状态">{{ detail.workStatus || "-" }}</el-descriptions-item>
            <el-descriptions-item label="装载状态">{{ detail.loadStatus || "-" }}</el-descriptions-item>
            <el-descriptions-item label="证照状态" :span="2">{{ detail.expireStatus || "-" }}</el-descriptions-item>
          </el-descriptions>

          <el-descriptions title="行驶证信息" :column="2" border class="truck-detail__block">
            <el-descriptions-item label="发动机编号">{{ license?.engineNumber || "-" }}</el-descriptions-item>
            <el-descriptions-item label="道路运输证号">{{ license?.transportCertificateNumber || "-" }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ license?.registrationDate || "-" }}</el-descriptions-item>
            <el-descriptions-item label="强制报废日期">{{ license?.mandatoryScrap || "-" }}</el-descriptions-item>
            <el-descriptions-item label="检验有效期">{{ license?.expirationDate || "-" }}</el-descriptions-item>
            <el-descriptions-item label="行驶证有效期">{{ license?.validityPeriod || "-" }}</el-descriptions-item>
            <el-descriptions-item label="整备质量">{{ license?.overallQuality ?? "-" }}</el-descriptions-item>
            <el-descriptions-item label="核定载质量">{{ license?.allowableWeight ?? "-" }}</el-descriptions-item>
            <el-descriptions-item label="外廓尺寸" :span="2">{{ license?.outsideDimensions || "-" }}</el-descriptions-item>
          </el-descriptions>

          <div v-if="license?.picture" class="truck-detail__block">
            <div class="truck-detail__title">证件图片</div>
            <el-image :src="license.picture" class="truck-detail__image" fit="contain" :preview-src-list="[license.picture]" />
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue";
import { Plus } from "@element-plus/icons-vue";
import type { FormInstance, FormRules } from "element-plus";
import TruckAPI from "@/api/truck";
import type { TruckRecord, TruckLicenseRecord, TruckPageQuery, TruckSavePayload } from "@/api/truck";
import CommonSimpleAPI from "@/api/common-simple";
import type { FleetSimple, TruckTypeSimple } from "@/api/common-simple";

/** 下拉选项 */
const fleetOptions = ref<FleetSimple[]>([]);
const truckTypeOptions = ref<TruckTypeSimple[]>([]);

/** 查询与列表状态 */
const loading = ref(false);
const list = ref<TruckRecord[]>([]);
const total = ref(0);
const query = ref<TruckPageQuery>({ page: 1, pageSize: 10 });

/** 组装查询参数：剔除空字符串/空值，仅提交有效过滤条件 */
function buildParams(): TruckPageQuery {
  const { page, pageSize, ...filters } = query.value;
  const params: TruckPageQuery = { page, pageSize };
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
    const res = await TruckAPI.page(buildParams());
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

/** 载重单位换算：后端落库 kg，页面统一吨（÷1000 回显，最多 3 位小数去掉浮点尾巴） */
function formatLoadT(kg?: number): string | number {
  if (kg === null || kg === undefined) return "-";
  return Number((kg / 1000).toFixed(3));
}

/** 新增/编辑弹窗 */
interface TruckForm {
  licensePlate: string;
  brand: string;
  deviceGpsId: string;
  truckTypeId?: string;
  fleetId?: string;
  /** 准载重量，单位吨 */
  allowableLoadT?: number;
  /** 准载体积，单位 m³ */
  allowableVolume?: number;
}

const formVisible = ref(false);
const saving = ref(false);
const editingId = ref<string>();
const formRef = ref<FormInstance>();

const createDefaultForm = (): TruckForm => ({
  licensePlate: "",
  brand: "",
  deviceGpsId: "",
  truckTypeId: undefined,
  fleetId: undefined,
  allowableLoadT: undefined,
  allowableVolume: undefined,
});
const form = reactive<TruckForm>(createDefaultForm());

const rules: FormRules<TruckForm> = {
  licensePlate: [{ required: true, message: "请输入车牌", trigger: "blur" }],
};

const formTitle = computed(() => (editingId.value ? "编辑车辆" : "新增车辆"));

function openCreate() {
  editingId.value = undefined;
  Object.assign(form, createDefaultForm());
  formVisible.value = true;
}

async function openEdit(row: TruckRecord) {
  // 编辑前拉详情：列表项不含 GPS/品牌可能被截断，且需外键对象的 id 回填下拉
  const detail = await TruckAPI.getDetail(row.id);
  editingId.value = row.id;
  Object.assign(form, createDefaultForm(), {
    licensePlate: detail.licensePlate ?? "",
    brand: detail.brand ?? "",
    deviceGpsId: detail.deviceGpsId ?? "",
    truckTypeId: detail.truckType?.id,
    fleetId: detail.fleet?.id,
    allowableLoadT: detail.allowableLoad !== null && detail.allowableLoad !== undefined
      ? Number((detail.allowableLoad / 1000).toFixed(3))
      : undefined,
    allowableVolume: detail.allowableVolume ?? undefined,
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
    // 选填字段清空必须端到端落 null：显式传 null（实体 FieldStrategy.ALWAYS 才会更新为 NULL）
    const payload: TruckSavePayload = {
      licensePlate: form.licensePlate.trim(),
      brand: form.brand.trim() || null,
      deviceGpsId: form.deviceGpsId.trim() || null,
      truckType: form.truckTypeId ? { id: form.truckTypeId } : null,
      fleet: form.fleetId ? { id: form.fleetId } : null,
      // 吨 → kg，保留 2 位小数抵消浮点误差
      allowableLoad:
        form.allowableLoadT === undefined || form.allowableLoadT === null
          ? null
          : Number((form.allowableLoadT * 1000).toFixed(2)),
      allowableVolume:
        form.allowableVolume === undefined || form.allowableVolume === null ? null : form.allowableVolume,
    };
    if (editingId.value) {
      await TruckAPI.update(editingId.value, payload);
      ElMessage.success("更新成功");
    } else {
      await TruckAPI.save(payload);
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

async function handleDelete(row: TruckRecord) {
  // BFF DELETE 实为软停用 status=0：车辆分页只查启用项，停用后列表不可见且无启用入口
  try {
    await ElMessageBox.confirm(
      `确认删除车辆「${row.licensePlate || row.id}」？删除后该车将停用并从档案列表消失，当前无恢复入口；存在未完成运输任务的车辆无法删除。`,
      "删除确认",
      { type: "warning", confirmButtonText: "删除", cancelButtonText: "取消" },
    );
  } catch {
    return;
  }
  await TruckAPI.remove(row.id);
  ElMessage.success("删除成功");
  // 末页仅剩一条时回退一页，避免停在空页
  if (list.value.length === 1 && query.value.page > 1) {
    query.value.page -= 1;
  }
  loadList();
}

/** 详情（车辆详情 + 行驶证并行加载） */
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<TruckRecord>();
const license = ref<TruckLicenseRecord>();

async function openDetail(row: TruckRecord) {
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = undefined;
  license.value = undefined;
  try {
    const [detailRes, licenseRes] = await Promise.all([
      TruckAPI.getDetail(row.id),
      TruckAPI.getLicense(row.id),
    ]);
    detail.value = detailRes;
    license.value = licenseRes;
  } finally {
    detailLoading.value = false;
  }
}

onMounted(() => {
  loadList();
  CommonSimpleAPI.fleetOptions().then((res) => {
    fleetOptions.value = res ?? [];
  });
  CommonSimpleAPI.truckTypeOptions().then((res) => {
    truckTypeOptions.value = res ?? [];
  });
});
</script>

<style lang="scss" scoped>
/* 车辆详情抽屉：容器作为 loading 遮罩定位上下文；区块留白；自定义标题；证件图片尺寸约束 */
.truck-detail {
  position: relative;
  min-height: 100%;
}

.truck-detail__block {
  margin-top: 16px;
}

.truck-detail__title {
  margin: 16px 0 12px;
  font-size:14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.truck-detail__image {
  display: block;
  max-width: 100%;
  max-height: 280px;
  border-radius: var(--card-radius);
  border: 1px solid var(--card-border);
}
</style>
