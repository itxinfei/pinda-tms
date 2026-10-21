<template>
  <div class="page-container page-container--split area-page">
    <!-- 左侧：层级树 / 搜索结果 -->
    <aside class="page-aside" :class="{ 'is-collapsed': asideCollapsed }">
      <div class="page-aside__inner">
        <div class="area-aside">
          <el-input
            v-model="keyword"
            class="area-aside__search"
            placeholder="搜索区划名称，如：朝阳"
            clearable
            @clear="resetSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <!-- 搜索结果（有搜索词时替换树视图） -->
          <template v-if="searchMode">
            <div class="area-aside__results-head">
              <span>搜索结果（{{ results.length }}）</span>
              <el-link type="primary" :underline="false" @click="resetSearch">返回层级树</el-link>
            </div>
            <ElScrollbar class="area-aside__results">
              <div
                v-for="item in results"
                :key="item.id"
                class="area-result"
                :class="{ 'is-active': current?.id === item.id }"
                @click="handleResultClick(item)"
              >
                <div class="area-result__row">
                  <span class="area-result__name" :title="item.name">{{ item.name }}</span>
                  <el-tag :type="levelTag(item.level)" size="small" effect="light" round>
                    {{ levelLabel(item.level) }}
                  </el-tag>
                </div>
                <div class="area-result__sub" :title="item.mergerName || item.name">
                  {{ item.mergerName || item.name }}
                </div>
              </div>
              <el-empty
                v-if="!searching && results.length === 0"
                :description="searchError ? '加载失败' : '未找到相关区划'"
                :image-size="80"
              >
                <el-button v-if="searchError" type="primary" @click="handleSearch">重试</el-button>
              </el-empty>
            </ElScrollbar>
          </template>

          <!-- 层级树（懒加载） -->
          <el-tree
            v-else
            ref="treeRef"
            class="area-aside__tree"
            :props="treeProps"
            :load="loadNode"
            lazy
            node-key="id"
            highlight-current
            :expand-on-click-node="false"
            @node-click="handleNodeClick"
          >
            <template #default="{ data }">
              <span class="area-node">
                <span class="area-node__name">{{ data.name }}</span>
                <el-tag :type="levelTag(data.level)" size="small" effect="plain" round>
                  {{ levelLabel(data.level) }}
                </el-tag>
              </span>
            </template>
          </el-tree>
        </div>
      </div>
      <button
        class="page-aside__toggle"
        :title="asideCollapsed ? '展开区划列表' : '收起区划列表'"
        @click="asideCollapsed = !asideCollapsed"
      >
        <el-icon :size="14">
          <Fold v-if="!asideCollapsed" />
          <Expand v-else />
        </el-icon>
      </button>
    </aside>

    <!-- 右侧：面包屑 + 详情 + 直属下级 -->
    <div class="page-main">
      <!-- 当前节点详情 -->
      <el-card class="area-detail-card" shadow="never">
        <el-breadcrumb v-if="chain.length > 0" class="area-breadcrumb" separator="/">
          <el-breadcrumb-item v-for="(item, idx) in chain" :key="item.id">
            <span
              class="area-breadcrumb__item"
              :class="{ 'is-current': idx === chain.length - 1 }"
              @click="handleCrumbClick(item, idx)"
            >
              {{ item.name }}
            </span>
          </el-breadcrumb-item>
        </el-breadcrumb>

        <el-descriptions v-if="current" class="area-detail" :column="3" border size="small">
          <el-descriptions-item label="名称">{{ current.name }}</el-descriptions-item>
          <el-descriptions-item label="全称">{{ current.mergerName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="简称">{{ current.shortName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="层级">
            <el-tag :type="levelTag(current.level)" size="small" effect="light">
              {{ levelLabel(current.level) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="行政区划码">
            <span class="area-code">
              {{ current.areaCode || "-" }}
              <el-icon
                v-if="current.areaCode"
                class="area-code__copy"
                title="复制"
                @click="copyText(current.areaCode)"
              >
                <CopyDocument />
              </el-icon>
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="城市编码">
            {{ current.cityCode || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="邮政编码">{{ current.zipCode || "-" }}</el-descriptions-item>
          <el-descriptions-item label="拼音">{{ current.pinyin || "-" }}</el-descriptions-item>
          <el-descriptions-item label="经纬度">
            <span v-if="current.lng != null && current.lat != null">
              {{ current.lng }}, {{ current.lat }}
            </span>
            <span v-else>-</span>
          </el-descriptions-item>
        </el-descriptions>
        <el-skeleton v-else :rows="3" animated />
      </el-card>

      <!-- 直属下级 -->
      <el-card class="area-children-card" shadow="never">
        <div class="area-children__toolbar">
          <span class="area-children__title">
            直属下级
            <el-tag v-if="current" type="info" size="small" effect="plain" round>
              {{ childrenList.length }}
            </el-tag>
          </span>
          <el-tooltip content="刷新" placement="top">
            <el-button class="page-icon-btn" :loading="childrenLoading" @click="reloadChildren">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </el-tooltip>
        </div>

        <div v-loading="childrenLoading" class="area-children__body">
          <el-table v-if="childrenList.length > 0" :data="childrenList" class="page-table" border>
            <el-table-column label="名称" min-width="180">
              <template #default="scope">
                <el-link type="primary" :underline="false" @click="selectArea(scope.row as Area)">
                  {{ scope.row.name }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column label="层级" width="90" align="center">
              <template #default="scope">
                <el-tag :type="levelTag(scope.row.level)" size="small" effect="plain">
                  {{ levelLabel(scope.row.level) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="areaCode" label="行政区划码" width="130" />
            <el-table-column prop="cityCode" label="城市编码" width="110" />
            <el-table-column prop="zipCode" label="邮编" width="100" />
            <el-table-column prop="pinyin" label="拼音" min-width="120" show-overflow-tooltip />
            <el-table-column label="操作" width="90" align="center" fixed="right">
              <template #default="scope">
                <el-button
                  v-if="scope.row.level < 4"
                  link
                  type="primary"
                  size="small"
                  @click="selectArea(scope.row as Area)"
                >
                  下级
                </el-button>
                <span v-else class="color-text-placeholder">末级</span>
              </template>
            </el-table-column>
          </el-table>

          <el-empty
            v-else-if="!childrenLoading && current"
            :description="current.level >= 4 ? '已到末级行政区划' : '该区划暂无下级数据'"
            :image-size="90"
          />
          <el-empty v-else-if="!childrenLoading" description="请选择左侧区划" :image-size="90" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import type { LoadFunction, TreeNodeData } from "element-plus/es/components/tree";
import AreaAPI from "@/api/area";
import type { Area, AreaLevel } from "@/api/area";

defineOptions({
  name: "Area",
  inheritAttrs: false,
});

/* ---------- 层级映射：0省 1市 2县 3镇 4乡村 ---------- */
const LEVEL_LABELS = ["省", "市", "区县", "乡镇", "乡村"];
const LEVEL_TAGS = ["primary", "success", "warning", "danger", "info"] as const;
const levelLabel = (level: AreaLevel) => LEVEL_LABELS[level] ?? "未知";
const levelTag = (level: AreaLevel) => LEVEL_TAGS[level] ?? "info";

/* ---------- 左侧面板 ---------- */
const asideCollapsed = ref(window.innerWidth < 992);

const treeRef = ref();
const treeProps = {
  label: "name",
  isLeaf: (data: TreeNodeData) => (data as unknown as Area).level >= 4,
};

/** 懒加载直属下级：根节点返回省级 */
const loadNode: LoadFunction = async (node, resolve) => {
  const area = node.level === 0 ? undefined : (node.data as unknown as Area);
  try {
    const list = (await AreaAPI.children(area?.id)) ?? [];
    resolve(list as unknown as TreeNodeData[]);
  } catch {
    resolve([]);
  }
};

async function handleNodeClick(data: Area) {
  await selectArea(data);
}

/* ---------- 搜索 ---------- */
const keyword = ref("");
const searchMode = ref(false);
const searching = ref(false);
const searchError = ref(false);
const results = ref<Area[]>([]);

async function handleSearch() {
  const name = keyword.value.trim();
  if (!name) return;
  searching.value = true;
  searchError.value = false;
  try {
    results.value = (await AreaAPI.search(name)) ?? [];
    searchMode.value = true;
  } catch {
    results.value = [];
    searchError.value = true;
    searchMode.value = true;
    ElMessage.error("加载失败");
  } finally {
    searching.value = false;
  }
}

function resetSearch() {
  keyword.value = "";
  searchMode.value = false;
  searchError.value = false;
  results.value = [];
}

async function handleResultClick(area: Area) {
  await selectArea(area);
}

/* ---------- 右侧：当前节点 / 面包屑链 / 下级 ---------- */
const current = ref<Area>();
const chain = ref<Area[]>([]);
const childrenList = ref<Area[]>([]);
const childrenLoading = ref(false);

/** 选中区划：刷新详情、面包屑与直属下级 */
async function selectArea(area: Area) {
  current.value = area;
  childrenLoading.value = true;
  try {
    const [detail, list] = await Promise.all([
      AreaAPI.detail(area.id).catch(() => area),
      AreaAPI.children(area.id).catch(() => [] as Area[]),
    ]);
    current.value = detail;
    childrenList.value = list ?? [];
  } finally {
    childrenLoading.value = false;
  }
  await buildChain(area);
  // 最佳努力同步左侧树定位（搜索结果点击时联动展开）
  syncTreeTo(area);
}

/** 自下而上回溯祖先链，供面包屑展示与回跳 */
async function buildChain(area: Area) {
  const arr: Area[] = [area];
  let cur = area;
  let guard = 0;
  while (cur.parentId && cur.level > 0 && guard++ < 6) {
    try {
      cur = await AreaAPI.detail(cur.parentId);
      arr.unshift(cur);
    } catch {
      break;
    }
  }
  chain.value = arr;
}

/** 面包屑回跳：点击祖先节点重新定位（末级仅展示） */
function handleCrumbClick(item: Area, idx: number) {
  if (idx === chain.value.length - 1) return;
  selectArea(item);
}

async function reloadChildren() {
  if (!current.value) return;
  childrenLoading.value = true;
  try {
    childrenList.value = (await AreaAPI.children(current.value.id)) ?? [];
  } finally {
    childrenLoading.value = false;
  }
}

/** 将左侧树逐级展开并高亮到目标节点（懒加载树需逐级等待加载完成） */
async function syncTreeTo(area: Area) {
  const tree = treeRef.value;
  if (!tree || searchMode.value) return;
  try {
    for (const ancestor of chain.value.slice(0, -1)) {
      const node = tree.getNode(ancestor.id);
      if (!node) break;
      if (!node.loaded) {
        await new Promise<void>((resolve) => node.expand(() => resolve()));
      } else if (!node.expanded) {
        node.expand();
      }
    }
    tree.setCurrentKey(area.id);
  } catch {
    /* 树定位失败不影响右侧展示 */
  }
}

/* ---------- 工具 ---------- */
async function copyText(text?: string) {
  if (!text) return;
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success(`已复制：${text}`);
  } catch {
    ElMessage.error("复制失败，请手动复制");
  }
}

onMounted(async () => {
  // 默认定位到第一个省，避免右侧空屏
  try {
    const provinces = (await AreaAPI.children()) ?? [];
    if (provinces.length > 0) {
      await selectArea(provinces[0]);
    }
  } catch {
    /* 保持空态 */
  }
});
</script>

<style scoped>
.area-page {
  --page-aside-width: 288px;
}

/* ---------- 左侧 ---------- */
.area-aside {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  padding: 12px;
}

.area-aside__tree {
  flex: 1;
  overflow: auto;

  :deep(.el-tree-node__content) {
    height: 32px;
    border-radius: 6px;
  }
}

.area-node {
  display: flex;
  flex: 1;
  gap: 4px;
  align-items: center;
  min-width: 0;
  padding-right: 6px;
}

.area-node__name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 搜索结果列表 */
.area-aside__results-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.area-aside__results {
  flex: 1;
  min-height: 0;
}

.area-result {
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 6px;
  transition: background-color 0.15s;

  &:hover {
    background: var(--el-fill-color-light);
  }

  &.is-active {
    background: var(--el-color-primary-light-9);
  }
}

.area-result__row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.area-result__name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
  color: var(--el-text-color-primary);
  white-space: nowrap;
}

.area-result__sub {
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

/* ---------- 右侧 ---------- */
.area-detail-card {
  flex-shrink: 0;

  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 12px 16px;
  }
}

.area-breadcrumb {
  font-size: 14px;
  line-height: 1.5;

  :deep(.el-breadcrumb__item) {
    float: none;
    display: inline-flex;
    align-items: center;
  }
}

.area-breadcrumb__item {
  cursor: pointer;

  &:hover {
    color: var(--el-color-primary);
  }

  &.is-current {
    font-weight: 600;
    color: var(--el-text-color-primary);
    cursor: default;
  }
}

.area-code {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  font-family: var(--el-font-family, inherit);
  font-variant-numeric: tabular-nums;
}

.area-code__copy {
  color: var(--el-text-color-secondary);
  cursor: pointer;
  transition: color 0.15s;

  &:hover {
    color: var(--el-color-primary);
  }
}

/* 下级表格 */
.area-children-card {
  flex: 1 1 auto;
  min-height: 0;

  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    min-height: 0;
  }
}

.area-children__toolbar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.area-children__title {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.area-children__body {
  flex: 1;
  min-height: 0;
}
</style>
