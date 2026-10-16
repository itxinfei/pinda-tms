<template>
  <div class="page-container">
    <el-card class="page-search" shadow="never">
      <el-form :model="params" :inline="true" label-width="auto">
        <el-form-item prop="pattern" label="匹配模式">
          <el-input
            v-model="params.pattern"
            placeholder="Redis glob，如 satoken:*（空=全部）"
            clearable
            style="width: 260px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleQuery">搜索</el-button>
          <el-button @click="handleResetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="删除操作不可恢复；satoken: 前缀为登录会话键，删除即强制对应会话下线。单次最多展示 2000 个匹配键，请用更精确的模式缩小范围。"
      />
    </el-card>

    <el-card class="page-content" shadow="never">
      <div class="page-toolbar">
        <div class="page-toolbar__right" style="margin-left: auto">
          <el-tooltip content="刷新" placement="top">
            <el-button class="page-icon-btn" @click="fetchData">
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
          <el-table-column label="键名" prop="key" min-width="320" show-overflow-tooltip />
          <el-table-column label="类型" prop="type" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="剩余生存时间" width="140" align="center">
            <template #default="{ row }">
              {{ formatTtl(row.ttl) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-popconfirm
                :title="confirmTitle(row as CacheKeyItem)"
                confirm-button-text="删除"
                cancel-button-text="取消"
                @confirm="handleDelete(row as CacheKeyItem)"
              >
                <template #reference>
                  <el-button
                    v-hasPerm="['sys:cache:delete']"
                    type="danger"
                    link
                    size="small"
                  >
                    删除
                  </el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <Pagination
        v-model:total="total"
        v-model:page="params.pageNum"
        v-model:limit="params.pageSize"
        @pagination="fetchData"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { Refresh } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";

import CacheAPI from "@/api/system/cache";
import type { CacheKeyItem, CacheKeyQueryParams } from "@/api/system/cache";
import { usePageTable } from "@/composables";

defineOptions({
  name: "Cache",
  inheritAttrs: false,
});

const { loading, list, total, params, fetchData, handleQuery, handleResetQuery } = usePageTable<
  CacheKeyItem,
  CacheKeyQueryParams
>({
  initialParams: {
    pageNum: 1,
    pageSize: 10,
    pattern: "",
  },
  request: CacheAPI.getPage,
});

const formatTtl = (ttl: number) => {
  if (ttl === -1) return "永不过期";
  if (ttl < 0) return "-";
  if (ttl >= 86400) return `${Math.floor(ttl / 86400)} 天`;
  if (ttl >= 3600) return `${Math.floor(ttl / 3600)} 小时`;
  if (ttl >= 60) return `${Math.floor(ttl / 60)} 分钟`;
  return `${ttl} 秒`;
};

const confirmTitle = (row: CacheKeyItem) => {
  const suffix = row.key.startsWith("satoken:")
    ? "（会话键：删除将强制下线对应会话）"
    : "（不可恢复）";
  return `确认删除 ${row.key} ${suffix}？`;
};

const handleDelete = async (row: CacheKeyItem) => {
  await CacheAPI.delete(row.key);
  ElMessage.success("已删除");
  fetchData();
};
</script>
