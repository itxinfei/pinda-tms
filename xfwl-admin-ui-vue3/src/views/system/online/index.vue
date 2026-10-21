<template>
  <div class="page-container">
    <el-card class="page-search" shadow="never">
      <el-form :model="params" :inline="true" label-width="auto">
        <el-form-item prop="keywords" label="关键字">
          <el-input
            v-model="params.keywords"
            placeholder="账号/姓名/租户编码"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleQuery">搜索</el-button>
          <el-button @click="handleResetQuery">重置</el-button>
        </el-form-item>
      </el-form>
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
          <template #empty>
            <el-empty v-if="error" description="加载失败">
              <el-button type="primary" @click="fetchData">重试</el-button>
            </el-empty>
            <el-empty v-else description="暂无数据" />
          </template>
          <el-table-column label="账号" prop="account" width="140" show-overflow-tooltip />
          <el-table-column label="姓名" prop="name" width="120" />
          <el-table-column label="租户" prop="tenantCode" width="100" />
          <el-table-column label="用户ID" prop="userId" width="120" />
          <el-table-column label="会话凭证" prop="tokenMask" width="140" show-overflow-tooltip />
          <el-table-column label="登录时间" prop="loginTime" width="180" />
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-popconfirm
                title="确认强制下线该会话？"
                confirm-button-text="下线"
                cancel-button-text="取消"
                @confirm="handleKickout(row as OnlineItem)"
              >
                <template #reference>
                  <el-button v-hasPerm="['sys:online:kickout']" type="danger" link size="small">
                    强制下线
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

import OnlineAPI from "@/api/system/online";
import type { OnlineItem, OnlineQueryParams } from "@/api/system/online";
import { usePageTable } from "@/composables";

defineOptions({
  name: "Online",
  inheritAttrs: false,
});

const { loading, error, list, total, params, fetchData, handleQuery, handleResetQuery } =
  usePageTable<OnlineItem, OnlineQueryParams>({
    initialParams: {
      pageNum: 1,
      pageSize: 10,
      keywords: "",
    },
    request: OnlineAPI.getPage,
  });

const handleKickout = async (row: OnlineItem) => {
  await OnlineAPI.kickout(row.token);
  ElMessage.success(`已强制下线：${row.account || row.userId}`);
  fetchData();
};
</script>
