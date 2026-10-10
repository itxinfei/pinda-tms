<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <el-form class="page-search" :model="query" inline>
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
          <el-table-column prop="allowableLoad" label="准载重(t)" min-width="100" align="right" />
          <el-table-column prop="allowableVolume" label="准体积(m³)" min-width="110" align="right" />
          <el-table-column prop="deviceGpsId" label="GPS 设备" min-width="120" show-overflow-tooltip />
          <el-table-column label="操作" fixed="right" width="90" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openDetail(scope.row as TruckRecord)">详情</el-button>
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
            <el-descriptions-item label="准载重量(t)">{{ detail.allowableLoad ?? "-" }}</el-descriptions-item>
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
import { ref, onMounted } from "vue";
import TruckAPI from "@/api/truck";
import CommonSimpleAPI from "@/api/common-simple";
import type { TruckRecord, TruckLicenseRecord, TruckPageQuery } from "@/api/truck";
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
