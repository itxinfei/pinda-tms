<template>
  <el-config-provider :locale="locale" :size="size">
    <el-watermark
      :font="{ color: fontColor }"
      :content="watermarkContent"
      :z-index="9999"
      class="wh-full"
    >
      <router-view />
    </el-watermark>
  </el-config-provider>
</template>

<script setup lang="ts">
import { useRoute } from "vue-router";
import { useAppStore, useSettingsStore, useLicenseStore } from "@/stores";
import { appConfig } from "@/settings";
import { ThemeMode, ComponentSize } from "@/enums";
import { useVersionCheck } from "@/composables/useVersionCheck";

const appStore = useAppStore();
const settingsStore = useSettingsStore();

const locale = computed(() => appStore.locale);
const size = computed(() => appStore.size as ComponentSize);
const licenseStore = useLicenseStore();
// 应用启动即恢复授权状态：读 localStorage 密钥重新验签，刷新/重新登录后保持生效
licenseStore.restore();

// 水印内容：未授权（体验版）所有页面强制显示「体验版」水印；已授权回落到用户水印设置
const watermarkContent = computed(() => {
  if (!licenseStore.activated) return "心飞TMS · 体验版";
  return settingsStore.showWatermark ? appConfig.name : "";
});

// 浏览器标签标题：「译名 · 应用名」；meta.title 为菜单原文，route.<原文> 命中语言包则翻译，
// 依赖 locale 与路由，切换语言/页面即时刷新
const route = useRoute();
const { t, te } = useI18n();
const translatedTitle = computed(() => {
  const raw = route.meta?.title as string | undefined;
  if (!raw) return "";
  const key = `route.${raw}`;
  return te(key) ? t(key) : raw;
});
watchEffect(() => {
  document.title = translatedTitle.value
    ? `${translatedTitle.value} · ${appConfig.title}`
    : appConfig.title;
});

// 明亮/暗黑主题水印字体颜色适配
const fontColor = computed(() => {
  return settingsStore.resolvedTheme === ThemeMode.DARK
    ? "rgba(255, 255, 255, .15)"
    : "rgba(0, 0, 0, .15)";
});

// 初始化版本检查
if (!import.meta.env.DEV) {
  useVersionCheck(5 * 60_000);
}
</script>
