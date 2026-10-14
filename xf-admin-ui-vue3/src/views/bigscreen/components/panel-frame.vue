<template>
  <section class="bs-panel">
    <span class="bs-corner bs-corner--tl" />
    <span class="bs-corner bs-corner--tr" />
    <span class="bs-corner bs-corner--bl" />
    <span class="bs-corner bs-corner--br" />
    <header v-if="title" class="bs-panel__header">
      <i class="bs-panel__marker" />
      <h2 class="bs-panel__title">{{ title }}</h2>
      <slot name="extra" />
    </header>
    <div class="bs-panel__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
defineOptions({ name: "PanelFrame" });

defineProps<{
  /** 面板标题，不传则无标题区 */
  title?: string;
}>();
</script>

<style scoped lang="scss">
.bs-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding: 14px 16px 12px;
  background: var(--bs-panel-bg);
  border: 1px solid var(--bs-panel-border);
  box-shadow: inset 0 0 24px rgba(0, 110, 255, 0.12);
  backdrop-filter: blur(2px);

  &__header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
  }

  &__marker {
    width: 4px;
    height: 16px;
    background: linear-gradient(180deg, var(--bs-cyan), rgba(0, 234, 255, 0.15));
    box-shadow: 0 0 8px rgba(0, 234, 255, 0.8);
  }

  &__title {
    margin: 0;
    font-size: 17px;
    font-weight: 600;
    letter-spacing: 2px;
    color: var(--bs-panel-title);
  }

  &__body {
    position: relative;
    flex: 1;
    min-height: 0;
  }
}
</style>
