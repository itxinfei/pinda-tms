<template>
  <div class="layout-navbar">
    <div class="flex-y-center">
      <Hamburger :is-active="sidebarState.opened" @toggle-click="sidebarState.toggle" />
      <Breadcrumb />
    </div>

    <div class="layout-navbar__actions">
      <el-button :icon="Monitor" plain type="primary" @click="openBigScreen">运营大屏</el-button>
      <LayoutToolbar />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAppStore } from "@/stores";
import { Monitor } from "@element-plus/icons-vue";
import { resolveExternalUrl } from "@/utils/route";

const props = withDefaults(
  defineProps<{
    toggleTarget?: "primary" | "secondary";
  }>(),
  {
    toggleTarget: "primary",
  }
);

const appStore = useAppStore();

const sidebarState = computed(() =>
  props.toggleTarget === "secondary"
    ? {
        opened: appStore.secondarySidebar?.opened ?? true,
        toggle: () => appStore.toggleSecondarySidebar(),
      }
    : { opened: appStore.sidebar.opened, toggle: () => appStore.toggleSidebar() }
);

// 运营大屏：独立全屏路由，新标签页打开
function openBigScreen() {
  window.open(resolveExternalUrl("/dashboard/bigscreen"), "_blank");
}
</script>

<style lang="scss" scoped>
.layout-navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: $navbar-height;
  padding: 0 14px 0 6px;
  background-color: var(--content-bg);
  border-bottom: 1px solid var(--card-border);
  box-shadow: 0 1px 0 rgb(15 23 42 / 3%);

  &__actions {
    display: flex;
    gap: 10px;
    align-items: center;
    height: 100%;
  }
}
</style>
