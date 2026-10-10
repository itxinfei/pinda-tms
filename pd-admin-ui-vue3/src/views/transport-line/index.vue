<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 查询条件 -->
      <el-form class="page-search" :model="query" inline>
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

      <div class="page-table-wrapper">
        <el-table v-loading="loading" :data="list" class="page-table" border height="100%">
          <el-table-column prop="name" label="线路名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="lineNumber" label="线路编号" min-width="120" show-overflow-tooltip />
          <el-table-column label="起始 → 目的机构" min-width="240" show-overflow-tooltip>
            <template #default="scope">{{ routeText(scope.row as TransportLineRecord) }}</template>
          </el-table-column>
          <el-table-column label="线路类型" min-width="120">
            <template #default="scope">{{ scope.row.transportLineType?.name || "-" }}</template>
          </el-table-column>
          <el-table-column prop="distance" label="距离(km)" min-width="100" align="right" />
          <el-table-column prop="cost" label="成本(元)" min-width="100" align="right" />
          <el-table-column prop="estimatedTime" label="预计时间" min-width="100" align="right" />
          <el-table-column label="操作" fixed="right" width="90" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="openDetail(scope.row as TransportLineRecord)">详情</el-button>
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
            <el-descriptions-item label="预计时间">{{ detail.estimatedTime ?? "-" }}</el-descriptions-item>
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
import { ref, onMounted } from "vue";
import TransportLineAPI from "@/api/transport-line";
import CommonSimpleAPI from "@/api/common-simple";
import type { TransportLineRecord, TransportLinePageQuery } from "@/api/transport-line";
import type { TransportLineTypeSimple } from "@/api/common-simple";

/** 线路类型下拉 */
const lineTypeOptions = ref<TransportLineTypeSimple[]>([]);

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
});
</script>
