<template>
  <el-tooltip :content="t('login.theme')" placement="bottom">
    <el-icon :size="20" class="click-switch" @click="cycleTheme">
      <component :is="currentThemeIcon" />
    </el-icon>
  </el-tooltip>
</template>
<script setup lang="ts">
import { useSettingsStore } from "@/stores";
import { ThemeMode } from "@/enums";
import { Moon, Sunny, Monitor } from "@element-plus/icons-vue";

const { t } = useI18n();
const settingsStore = useSettingsStore();

// 自动模式跟随系统，故显示显示器图标；否则按实际生效主题显示太阳/月亮
const currentThemeIcon = computed(() => {
  if (settingsStore.theme === ThemeMode.AUTO) {
    return Monitor;
  }

  return settingsStore.resolvedTheme === ThemeMode.DARK ? Moon : Sunny;
});

/**
 * 点击直接切换主题：亮色 -> 暗色 -> 自动 -> 亮色
 */
const cycleTheme = () => {
  const order = [ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.AUTO];
  const idx = order.indexOf(settingsStore.theme);
  const next = order[(idx + 1) % order.length];
  settingsStore.theme = next;
};
</script>

<style scoped>
.click-switch {
  cursor: pointer;
}
</style>
