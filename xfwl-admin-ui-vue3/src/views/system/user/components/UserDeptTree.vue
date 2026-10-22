<template>
  <el-card shadow="never" class="dept-card">
    <el-input v-model="deptName" class="dept-card__search" placeholder="搜索组织" clearable>
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>

    <el-tree
      ref="deptTreeRef"
      v-loading="loading"
      class="dept-card__tree"
      :data="deptList"
      :props="{ children: 'children', label: 'label', disabled: '' }"
      :expand-on-click-node="false"
      :filter-node-method="handleFilter"
      default-expand-all
      @node-click="handleNodeClick"
    >
      <template #empty>
        <el-empty description="暂无组织" :image-size="60" />
      </template>
    </el-tree>
  </el-card>
</template>

<script setup lang="ts">
import OrgAPI from "@/api/org";
import type { OptionItem } from "@/api/common";
import type { TreeNodeData } from "element-plus/es/components/tree";

/**
 * 用户组织树：按组织筛选用户（组件名/UserDeptTree 与 dept* 命名为历史遗留，数据源实为 OrgAPI）
 */
const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: undefined,
  },
});

const deptList = ref<OptionItem[]>();
const deptTreeRef = ref();
const deptName = ref("");
const loading = ref(false);

/**
 * 选中组织时上抛组织 ID（v-model 名 deptId 为跨端契约，承载 orgId）
 */
const emits = defineEmits(["node-click"]);

const deptId = useVModel(props, "modelValue", emits);

watchEffect(
  () => {
    deptTreeRef.value?.filter(deptName.value);
  },
  {
    flush: "post",
  }
);

/**
 * 组织树的节点过滤
 */
function handleFilter(value: string, data: TreeNodeData): boolean {
  if (!value) {
    return true;
  }
  return String(data.label ?? "").includes(value);
}

/**
 * 点击组织树节点
 */
function handleNodeClick(data: OptionItem): void {
  deptId.value = data.value;
  emits("node-click");
}

onBeforeMount(() => {
  loading.value = true;
  OrgAPI.getOptions()
    .then((data) => {
      deptList.value = data;
    })
    .catch(() => {
      deptList.value = [];
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>

<style lang="scss" scoped>
.dept-card {
  height: 100%;
  background: transparent;
  border: 0;
  border-radius: inherit;
  box-shadow: none;

  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
    padding: 12px;
  }
}

.dept-card__search {
  margin-bottom: 8px;
}

.dept-card__tree {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;

  :deep(.el-tree-node__content) {
    height: 32px;
  }
}
</style>
