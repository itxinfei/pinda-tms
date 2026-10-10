<template>
  <div class="i-svg:language click-switch" :class="size" @click="toggleLanguage" />
</template>

<script setup lang="ts">
import { useAppStore } from "@/stores/app";
import { LanguageEnum } from "@/enums/settings";

/**
 * 语言切换：点击直接在中英文之间切换
 */
defineProps({
  size: {
    type: String,
    required: false,
  },
});

const appStore = useAppStore();
const { locale, t } = useI18n();

/**
 * 处理语言切换（中 <-> 英 直接切换）
 */
function toggleLanguage() {
  const next =
    locale.value === LanguageEnum.ZH_CN
      ? LanguageEnum.EN
      : LanguageEnum.ZH_CN;
  locale.value = next;
  appStore.changeLanguage(next);

  ElMessage.success(t("langSelect.message.success"));
}
</script>

<style scoped>
.click-switch {
  cursor: pointer;
}
</style>
