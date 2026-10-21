<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="license-header">
          <span>授权管理</span>
          <el-tag :type="licenseStore.activated ? 'success' : licenseStore.expired ? 'danger' : 'warning'">
            {{ licenseStore.activated ? "已授权" : licenseStore.expired ? "体验已到期（可重新激活）" : "体验版（未授权）" }}
          </el-tag>
        </div>
      </template>

      <el-alert
        v-if="licenseStore.expired"
        type="error"
        :closable="false"
        show-icon
        title="本轮体验已到期"
        description="使用同一体验码重新激活即可再获得 7 天完整体验（无需新码）：点击下方「重新激活」按钮，或重新粘贴体验码激活。如需正式授权请购买商业授权。"
        class="license-alert"
      />

      <el-alert
        v-else-if="!licenseStore.activated"
        type="warning"
        :closable="false"
        show-icon
        title="当前为体验版"
        description="体验版功能不受限，但所有页面会显示「体验版」水印。输入有效的授权密钥并激活后，水印与体验提示将自动消失，且在刷新或重新登录后保持生效。"
        class="license-alert"
      />

      <el-descriptions v-if="licenseStore.activated || licenseStore.expired" :column="1" border class="license-info">
        <el-descriptions-item label="授权对象">
          {{ licenseStore.licensee || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="许可类型">
          {{ licenseStore.type || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="到期时间">
          {{ expiresAtText }}
        </el-descriptions-item>
      </el-descriptions>

      <el-form label-width="90px" @submit.prevent>
        <el-form-item label="授权密钥">
          <el-input
            v-model="licenseKey"
            type="textarea"
            :rows="4"
            placeholder="粘贴作者提供的授权密钥（形如 xxx.yyy 的签名串）"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="activating" @click="handleActivate">
            激活授权
          </el-button>
          <el-button v-if="licenseStore.expired" type="success" :loading="activating" @click="handleReactivate">
            重新激活（同一体验码再体验 7 天）
          </el-button>
          <el-button v-if="licenseStore.activated" @click="handleDeactivate">
            清除授权
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from "element-plus";
import { useLicenseStore } from "@/stores";

const licenseStore = useLicenseStore();
const licenseKey = ref("");
const activating = ref(false);

const expiresAtText = computed(() => formatExpires(licenseStore.expiresAt));

async function handleActivate() {
  const key = licenseKey.value.trim();
  if (!key) {
    ElMessage.warning("请先粘贴授权密钥");
    return;
  }
  activating.value = true;
  try {
    const info = await licenseStore.activate(key);
    licenseKey.value = "";
    ElMessage.success(`授权激活成功（${info.licensee ?? "未知授权对象"}），体验版水印已移除`);
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : "授权密钥无效");
  } finally {
    activating.value = false;
  }
}

async function handleReactivate() {
  activating.value = true;
  try {
    const info = await licenseStore.reactivate();
    ElMessage.success(`已重新激活（${info.licensee ?? "体验用户"}），本轮体验至 ${formatExpires(licenseStore.expiresAt)}`);
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : "重新激活失败");
  } finally {
    activating.value = false;
  }
}

async function handleDeactivate() {
  try {
    await ElMessageBox.confirm("清除后将回到体验版（重新显示水印），确定继续？", "提示", {
      type: "warning",
    });
  } catch {
    return; // 用户取消
  }
  licenseStore.deactivate();
  ElMessage.success("已清除授权，回到体验版");
}

function formatExpires(ts: number | null): string {
  if (!ts) return "永久有效";
  const d = new Date(ts);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
</script>

<style lang="scss" scoped>
.license-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.license-alert {
  margin-bottom: 16px;
}

.license-info {
  margin-bottom: 16px;
}
</style>
