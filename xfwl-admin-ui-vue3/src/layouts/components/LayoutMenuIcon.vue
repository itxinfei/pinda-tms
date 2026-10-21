<template>
  <!-- Element Plus 图标：el-icon-xxx -->
  <el-icon v-if="isElementIcon" class="layout-menu-icon layout-menu-icon--element menu-icon">
    <component :is="elementIconName" />
  </el-icon>
  <!-- 空图标 fallback：用 Element Plus 默认图标，避免黑色方块 -->
  <el-icon v-else-if="!hasValidIcon" class="layout-menu-icon layout-menu-icon--element menu-icon">
    <Document />
  </el-icon>
  <!-- 自定义 SVG 图标 -->
  <div v-else :class="['layout-menu-icon', 'layout-menu-icon--svg', 'menu-icon', svgIconClass]" />
</template>

<script setup lang="ts">
import { Document } from "@element-plus/icons-vue";

const props = withDefaults(
  defineProps<{
    icon?: string;
  }>(),
  {
    icon: "",
  }
);

const hasValidIcon = computed(() => Boolean(props.icon));
const isElementIcon = computed(() => (props.icon || "").startsWith("el-icon"));
const elementIconName = computed(() => (props.icon || "").replace("el-icon-", ""));
const svgIconClass = computed(() => `i-svg:${props.icon || "menu"}`);
</script>
