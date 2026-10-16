<template>
  <div class="page-container">
    <el-card class="page-search" shadow="never">
      <el-form :inline="true" label-width="auto">
        <el-form-item label="关键字">
          <el-input
            v-model="keywords"
            placeholder="服务名（如 xf-auth）"
            clearable
            @keyup.enter="fetchData"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">探测</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="page-content" shadow="never">
      <div class="page-toolbar" style="display: flex; align-items: center; gap: 16px">
        <el-tag type="success">UP：{{ overview.upCount }}</el-tag>
        <el-tag type="danger">DOWN：{{ overview.downCount }}</el-tag>
        <el-tag type="info">服务总数：{{ overview.total }}</el-tag>
        <div class="page-toolbar__right" style="margin-left: auto">
          <el-tooltip content="重新探测" placement="top">
            <el-button class="page-icon-btn" :loading="loading" @click="fetchData">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>
      <div class="page-table-wrapper">
        <el-table
          v-loading="loading"
          class="page-table"
          :data="list"
          height="100%"
          highlight-current-row
        >
          <el-table-column label="服务名" prop="serviceName" min-width="160" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'UP' ? 'success' : 'danger'" size="small">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="实例数" prop="instances" width="90" align="right" />
          <el-table-column label="实例地址" prop="instanceList" min-width="220" show-overflow-tooltip />
          <el-table-column label="探测耗时(ms)" width="120" align="right">
            <template #default="{ row }">
              {{ row.latency ?? "-" }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { Refresh } from "@element-plus/icons-vue";
import { onMounted } from "vue";

import HealthAPI from "@/api/system/health";
import type { HealthItem, HealthOverview } from "@/api/system/health";

defineOptions({
  name: "ServiceHealth",
  inheritAttrs: false,
});

const loading = ref(false);
const keywords = ref("");
const list = ref<HealthItem[]>([]);
const overview = ref<HealthOverview>({ total: 0, upCount: 0, downCount: 0 });

const fetchData = async () => {
  loading.value = true;
  try {
    const params = { keywords: keywords.value || undefined };
    const [items, ov] = await Promise.all([
      HealthAPI.getList(params),
      HealthAPI.getOverview(params),
    ]);
    list.value = items;
    overview.value = ov;
  } finally {
    loading.value = false;
  }
};

onMounted(fetchData);
</script>
