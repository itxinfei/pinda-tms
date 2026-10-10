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
          <el-table-column label="操作" fixed="right" width="90" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openDetail(scope.row as DriverRecord)">详情</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
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
</script>
