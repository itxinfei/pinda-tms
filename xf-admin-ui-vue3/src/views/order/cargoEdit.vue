<template>
  <div class="page-container">
    <el-card class="page-content" shadow="never">
      <!-- 订单信息 + 独立维护入口说明 -->
      <div class="page-toolbar">
        <div class="page-toolbar__left">
          <span class="cargo-order-no">订单号：<b>{{ orderNo || orderId || "-" }}</b></span>
          <el-tag class="cargo-order-no__tag" type="info" effect="plain" size="small">
            订单详情内已可直接维护货物，此页适合批量处理
          </el-tag>
        </div>
        <div class="page-toolbar__right">
          <el-button @click="goBack">返回</el-button>
          <el-button :icon="Refresh" @click="reload">刷新</el-button>
        </div>
      </div>

      <CargoPanel v-if="orderId" ref="panelRef" :order-id="orderId" />
      <el-empty v-else description="缺少订单参数，无法加载货物明细" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Refresh } from "@element-plus/icons-vue";
import CargoPanel from "./components/CargoPanel.vue";

const route = useRoute();
const router = useRouter();

const orderId = computed(() => (route.query.orderId as string) || "");
const orderNo = computed(() => (route.query.orderNo as string) || "");

const panelRef = ref<InstanceType<typeof CargoPanel>>();

function goBack() {
  router.back();
}

function reload() {
  panelRef.value?.reload();
}
</script>

<style scoped lang="scss">
.cargo-order-no {
  margin-right: 12px;

  &__tag {
    font-weight: 400;
  }
}
</style>