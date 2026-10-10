<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <el-tabs v-model="activeTab">
        <!-- 层级浏览 -->
        <el-tab-pane label="层级浏览" name="browse">
          <el-cascader
            v-model="selectedPath"
            :props="cascaderProps"
            placeholder="请依次选择 省 / 市 / 县 / 乡镇"
            clearable
            style="width: 420px"
            @change="handleSelect"
          />

          <el-descriptions
            v-if="current"
            class="area-detail"
            :column="3"
            border
            title="区划详情"
          >
            <el-descriptions-item label="名称">{{ current.name }}</el-descriptions-item>
            <el-descriptions-item label="全称">{{ current.mergerName }}</el-descriptions-item>
            <el-descriptions-item label="简称">{{ current.shortName }}</el-descriptions-item>
            <el-descriptions-item label="层级">{{ levelLabel(current.level) }}</el-descriptions-item>
            <el-descriptions-item label="行政区划码">{{ current.areaCode }}</el-descriptions-item>
            <el-descriptions-item label="城市编码">{{ current.cityCode }}</el-descriptions-item>
            <el-descriptions-item label="邮政编码">{{ current.zipCode }}</el-descriptions-item>
            <el-descriptions-item label="拼音">{{ current.pinyin }}</el-descriptions-item>
            <el-descriptions-item label="经度">{{ current.lng }}</el-descriptions-item>
            <el-descriptions-item label="纬度">{{ current.lat }}</el-descriptions-item>
          </el-descriptions>

          <el-empty v-else description="请选择左侧行政区划查看详情" />
        </el-tab-pane>

        <!-- 名称搜索 -->
        <el-tab-pane label="名称搜索" name="search">
          <div class="area-search-bar">
            <el-input
              v-model="keyword"
              placeholder="请输入区划名称，如：朝阳"
              clearable
              style="width: 320px"
              @keyup.enter="handleSearch"
            />
            <el-button type="primary" :loading="searching" @click="handleSearch">搜索</el-button>
          </div>

          <el-table v-loading="searching" :data="results" border style="margin-top: 16px">
            <el-table-column prop="name" label="名称" min-width="140" />
            <el-table-column prop="mergerName" label="全称" min-width="240" />
            <el-table-column prop="level" label="层级" width="100" align="center">
              <template #default="scope">{{ levelLabel(scope.row.level) }}</template>
            </el-table-column>
            <el-table-column prop="areaCode" label="行政区划码" width="140" />
            <el-table-column prop="zipCode" label="邮编" width="100" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import type { CascaderProps } from "element-plus";
import AreaAPI from "@/api/area";
import type { Area, AreaLevel } from "@/api/area";

const activeTab = ref("browse");

const LEVEL_LABELS = ["省", "市", "区县", "乡镇", "乡村"];
const levelLabel = (level: AreaLevel) => LEVEL_LABELS[level] ?? "未知";

/** 懒加载省/市/县/乡镇 */
const cascaderProps: CascaderProps = {
  lazy: true,
  async lazyLoad(node, resolve) {
    const parentId = node.level === 0 ? undefined : (node.value as number);
    const data = (await AreaAPI.children(parentId)) ?? [];
    // 返回空数组时该节点自动成为叶子
    resolve(
      data.map((area) => ({
        value: area.id,
        label: area.name,
      }))
    );
  },
};

const selectedPath = ref<number[]>();
const current = ref<Area>();

async function handleSelect(value: unknown) {
  const path = value as number[] | null | undefined;
  if (!path || path.length === 0) {
    current.value = undefined;
    return;
  }
  current.value = await AreaAPI.detail(path[path.length - 1]);
}

const keyword = ref("");
const searching = ref(false);
const results = ref<Area[]>([]);

async function handleSearch() {
  const name = keyword.value.trim();
  if (!name) return;
  searching.value = true;
  try {
    results.value = (await AreaAPI.search(name)) ?? [];
  } finally {
    searching.value = false;
  }
}
</script>

<style scoped>
.area-detail {
  margin-top: 20px;
}
.area-search-bar {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
