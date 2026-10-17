<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <el-form class="page-search" :model="query" inline>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="query.name" placeholder="司机姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="工号" prop="username">
          <el-input v-model="query.username" placeholder="工号/账号" clearable @keyup.enter="handleSearch" />
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

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="name" label="姓名" min-width="100" />
          <el-table-column prop="workNumber" label="工号" min-width="110" show-overflow-tooltip />
          <el-table-column prop="mobile" label="手机号" min-width="130" />
          <el-table-column label="车队" min-width="120">
            <template #default="scope">{{ scope.row.fleet?.name || "-" }}</template>
          </el-table-column>
          <el-table-column label="使用车辆" min-width="110">
            <template #default="scope">{{ scope.row.truck?.licensePlate || "-" }}</template>
          </el-table-column>
          <el-table-column label="线路" min-width="160" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.transportLine?.name || scope.row.truckTransportLine?.name || "-" }}</template>
          </el-table-column>
          <el-table-column prop="age" label="年龄" min-width="80" align="center" />
          <el-table-column label="操作" fixed="right" width="140" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openDetail(scope.row as DriverRecord)">详情</el-button>
              <el-button
                v-hasPerm="['driver:update']"
                type="primary"
                link
                size="small"
                @click="openEdit(scope.row as DriverRecord)"
              >编辑</el-button>
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

    <!-- 司机详情 -->
    <el-drawer v-model="detailVisible" title="司机详情" size="640px">
      <div v-loading="detailLoading" class="driver-detail">
        <template v-if="detail">
          <el-descriptions title="基本信息" :column="2" border>
            <el-descriptions-item label="姓名">{{ detail.name || "-" }}</el-descriptions-item>
            <el-descriptions-item label="工号">{{ detail.workNumber || "-" }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ detail.mobile || "-" }}</el-descriptions-item>
            <el-descriptions-item label="年龄">{{ detail.age ?? "-" }}</el-descriptions-item>
            <el-descriptions-item label="所属机构">{{ detail.agency?.name || "-" }}</el-descriptions-item>
            <el-descriptions-item label="所属车队">{{ detail.fleet?.name || "-" }}</el-descriptions-item>
            <el-descriptions-item label="工作状态" :span="2">{{ detail.workStatus || "-" }}</el-descriptions-item>
          </el-descriptions>

          <el-descriptions title="当前车辆与线路" :column="2" border class="driver-detail__block">
            <el-descriptions-item label="使用车辆">{{ detail.truck?.licensePlate || "-" }}</el-descriptions-item>
            <el-descriptions-item label="车牌品牌">{{ detail.truck?.brand || "-" }}</el-descriptions-item>
            <el-descriptions-item label="所属车次">{{ detail.truckTransportTrip?.name || "-" }}</el-descriptions-item>
            <el-descriptions-item label="发车/到达">
              {{ detail.truckTransportTrip?.departureTime || "-" }} / {{ detail.truckTransportTrip?.arrivalTime || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="线路" :span="2">{{ detail.transportLine?.name || detail.truckTransportLine?.name || "-" }}</el-descriptions-item>
          </el-descriptions>

          <el-descriptions title="驾驶证信息" :column="2" border class="driver-detail__block">
            <el-descriptions-item label="准驾车型">{{ license?.allowableType || "-" }}</el-descriptions-item>
            <el-descriptions-item label="驾驶证类型">{{ license?.licenseType || "-" }}</el-descriptions-item>
            <el-descriptions-item label="驾驶证号">{{ license?.licenseNumber || "-" }}</el-descriptions-item>
            <el-descriptions-item label="驾龄">{{ license?.driverAge ?? "-" }}</el-descriptions-item>
            <el-descriptions-item label="初次领证日期">{{ license?.initialCertificateDate || "-" }}</el-descriptions-item>
            <el-descriptions-item label="有效期限">{{ license?.validPeriod || "-" }}</el-descriptions-item>
            <el-descriptions-item label="从业资格证" :span="2">{{ license?.qualificationCertificate || "-" }}</el-descriptions-item>
            <el-descriptions-item label="入场证信息" :span="2">{{ license?.passCertificate || "-" }}</el-descriptions-item>
          </el-descriptions>

          <div v-if="license?.picture" class="driver-detail__block">
            <div class="driver-detail__title">证件图片</div>
            <el-image :src="license.picture" class="driver-detail__image" fit="contain" :preview-src-list="[license.picture]" />
          </div>
        </template>
      </div>
    </el-drawer>

    <!-- 编辑司机档案（年龄/车队 + 驾驶证） -->
    <el-dialog v-model="editVisible" title="编辑司机档案" width="640px" :close-on-click-modal="false">
      <el-form ref="editFormRef" v-loading="editLoading" :model="editForm" :rules="editRules" label-width="110px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="姓名">
          <el-input :model-value="editTarget?.name" disabled />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input :model-value="editTarget?.mobile" disabled placeholder="手机号属用户账号域，请在用户管理中修改" />
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="editForm.age" :min="18" :max="65" controls-position="right" style="width: 200px" />
        </el-form-item>
        <el-form-item label="所属车队" prop="fleetId">
          <el-select v-model="editForm.fleetId" placeholder="请选择车队" clearable filterable style="width: 200px">
            <el-option v-for="item in fleetOptions" :key="item.id" :value="item.id" :label="item.name" />
          </el-select>
        </el-form-item>

        <el-divider content-position="left">驾驶证信息</el-divider>
        <el-form-item label="驾驶证号" prop="licenseNumber">
          <el-input v-model="editForm.licenseNumber" placeholder="请输入驾驶证号" maxlength="30" />
        </el-form-item>
        <el-form-item label="准驾车型">
          <el-input v-model="editForm.allowableType" placeholder="如 A2/B2" maxlength="10" style="width: 200px" />
        </el-form-item>
        <el-form-item label="驾驶证类型">
          <el-input v-model="editForm.licenseType" placeholder="请输入驾驶证类型" maxlength="20" style="width: 200px" />
        </el-form-item>
        <el-form-item label="驾龄" prop="driverAge">
          <el-input-number v-model="editForm.driverAge" :min="0" :max="50" controls-position="right" style="width: 200px" />
        </el-form-item>
        <el-form-item label="初次领证日期">
          <el-date-picker
            v-model="editForm.initialCertificateDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="有效期限">
          <el-input v-model="editForm.validPeriod" placeholder="如 2028-12-31" maxlength="20" style="width: 200px" />
        </el-form-item>
        <el-form-item label="从业资格证">
          <el-input v-model="editForm.qualificationCertificate" placeholder="请输入从业资格证号" maxlength="30" />
        </el-form-item>
        <el-form-item label="入场证信息">
          <el-input v-model="editForm.passCertificate" placeholder="请输入入场证信息" maxlength="30" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { FormInstance, ElMessage } from "element-plus";
import DriverAPI from "@/api/driver";
import CommonSimpleAPI from "@/api/common-simple";
import type { DriverRecord, DriverLicenseRecord, DriverPageQuery } from "@/api/driver";
import type { FleetSimple } from "@/api/common-simple";

/** 车队下拉 */
const fleetOptions = ref<FleetSimple[]>([]);

/** 查询与列表状态 */
const loading = ref(false);
const list = ref<DriverRecord[]>([]);
const total = ref(0);
const query = ref<DriverPageQuery>({ page: 1, pageSize: 10 });

/** 组装查询参数：剔除空字符串/空值，仅提交有效过滤条件 */
function buildParams(): DriverPageQuery {
  const { page, pageSize, ...filters } = query.value;
  const params: DriverPageQuery = { page, pageSize };
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
    const res = await DriverAPI.page(buildParams());
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

/** 详情（司机基本信息 + 驾驶证并行加载） */
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<DriverRecord>();
const license = ref<DriverLicenseRecord>();

async function openDetail(row: DriverRecord) {
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = undefined;
  license.value = undefined;
  try {
    const [detailRes, licenseRes] = await Promise.all([
      DriverAPI.getDetail(row.userId),
      DriverAPI.getLicense(row.userId),
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
});

/** 编辑司机档案（基本信息：年龄/车队；驾驶证：按 userId upsert） */
interface DriverEditForm {
  age?: number;
  fleetId?: string;
  licenseNumber?: string;
  allowableType?: string;
  licenseType?: string;
  driverAge?: number;
  initialCertificateDate?: string;
  validPeriod?: string;
  qualificationCertificate?: string;
  passCertificate?: string;
}

const editVisible = ref(false);
const editLoading = ref(false);
const editSubmitting = ref(false);
const editFormRef = ref<FormInstance>();
const editTarget = ref<DriverRecord>();
const editForm = ref<DriverEditForm>({});

const editRules = {
  age: [{ required: true, message: "请输入年龄", trigger: "blur" }],
  licenseNumber: [
    { required: true, message: "请输入驾驶证号", trigger: "blur" },
    { min: 6, max: 30, message: "长度 6~30 个字符", trigger: "blur" },
  ],
};

async function openEdit(row: DriverRecord) {
  editVisible.value = true;
  editLoading.value = true;
  editTarget.value = row;
  editForm.value = {};
  try {
    // 基本信息来自行数据（姓名/手机号只读），车队与年龄取详情；驾驶证并行加载
    const [detailRes, licenseRes] = await Promise.all([
      DriverAPI.getDetail(row.userId),
      DriverAPI.getLicense(row.userId),
    ]);
    editForm.value = {
      age: detailRes?.age,
      fleetId: detailRes?.fleet?.id,
      licenseNumber: licenseRes?.licenseNumber,
      allowableType: licenseRes?.allowableType,
      licenseType: licenseRes?.licenseType,
      driverAge: licenseRes?.driverAge,
      initialCertificateDate: licenseRes?.initialCertificateDate,
      validPeriod: licenseRes?.validPeriod,
      qualificationCertificate: licenseRes?.qualificationCertificate,
      passCertificate: licenseRes?.passCertificate,
    };
  } finally {
    editLoading.value = false;
  }
}

async function handleSaveEdit() {
  const valid = await editFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  editSubmitting.value = true;
  try {
    const form = editForm.value;
    // 档案与驾驶证分两个后端端点，任一失败都会抛错中断提交
    await DriverAPI.update(editTarget.value!.userId, {
      age: form.age,
      fleet: form.fleetId ? { id: form.fleetId } : undefined,
    });
    await DriverAPI.saveLicense({
      userId: editTarget.value!.userId,
      licenseNumber: form.licenseNumber,
      allowableType: form.allowableType,
      licenseType: form.licenseType,
      driverAge: form.driverAge,
      initialCertificateDate: form.initialCertificateDate,
      validPeriod: form.validPeriod,
      qualificationCertificate: form.qualificationCertificate,
      passCertificate: form.passCertificate,
    });
    ElMessage.success("保存成功");
    editVisible.value = false;
    loadList();
  } finally {
    editSubmitting.value = false;
  }
}
</script>

<style lang="scss" scoped>
/* 司机详情抽屉：容器作为 loading 遮罩定位上下文；区块留白；自定义标题；证件图片尺寸约束 */
.driver-detail {
  position: relative;
  min-height: 100%;
}

.driver-detail__block {
  margin-top: 16px;
}

.driver-detail__title {
  margin: 16px 0 12px;
  font-size:14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.driver-detail__image {
  display: block;
  max-width: 100%;
  max-height: 280px;
  border-radius: var(--card-radius);
  border: 1px solid var(--card-border);
}
</style>
