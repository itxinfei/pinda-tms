<template>
  <div class="app-container license-page">
    <el-card shadow="never" class="license-card">
      <template #header>
        <div class="license-header">
          <span>授权管理</span>
          <el-tag :type="statusMeta.tag" size="large">{{ statusMeta.text }}</el-tag>
        </div>
      </template>

      <!-- 本机机器码：客户复制后发给作者，签发绑定本机的授权码 -->
      <div class="machine-box">
        <div class="machine-label">
          本机机器码
          <el-tooltip content="把机器码整段发给供应商，获取仅能在本机激活的授权码" placement="top">
            <el-icon class="machine-help"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
        <div class="machine-row">
          <code class="machine-code">{{ info?.fingerprintMasked ?? "生成中…" }}</code>
          <el-button :icon="CopyDocument" plain size="small" @click="copyFingerprint">复制</el-button>
        </div>
      </div>

      <el-alert
        :type="statusMeta.alert"
        :closable="false"
        show-icon
        :title="statusMeta.text"
        :description="info?.message"
        class="license-alert"
      />

      <el-descriptions v-if="hasLicense" :column="2" border class="license-info">
        <el-descriptions-item label="授权对象">{{ info?.licensee || "-" }}</el-descriptions-item>
        <el-descriptions-item label="许可类型">{{ typeText }}</el-descriptions-item>
        <el-descriptions-item label="实例数">{{ info?.instances ?? "-" }}</el-descriptions-item>
        <el-descriptions-item label="授权来源">{{ sourceText }}</el-descriptions-item>
        <el-descriptions-item label="签发时间">{{ formatTs(info?.issuedAt) }}</el-descriptions-item>
        <el-descriptions-item label="到期时间">{{ expiresText }}</el-descriptions-item>
      </el-descriptions>

      <!-- 环境变量托管：只读，不允许页面激活/清除 -->
      <el-alert
        v-if="info?.source === 'ENV'"
        type="info"
        :closable="false"
        show-icon
        title="当前授权由环境变量 XINFEI_LICENSE 托管"
        description="环境变量授权为只读模式，不能在页面激活或清除；如需变更，请由运维修改部署配置后重新部署。"
        class="license-alert"
      />

      <el-form v-else label-width="90px" @submit.prevent>
        <el-form-item label="授权码">
          <el-input
            v-model="licenseKey"
            type="textarea"
            :rows="4"
            placeholder="粘贴供应商提供的授权码（形如 载荷.签名 的一串，绑定本机机器码）"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleActivate">激活授权</el-button>
          <el-button v-if="info?.source === 'FILE'" :loading="loading" @click="handleClear">
            清除授权
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { CopyDocument, QuestionFilled } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { LicenseStatus } from "@/api/license";
import { useLicenseStore } from "@/stores";

const licenseStore = useLicenseStore();
const licenseKey = ref("");
const loading = ref(false);

const info = computed(() => licenseStore.info);
const hasLicense = computed(() => !!info.value && info.value.source !== "NONE");

type TagType = "success" | "danger" | "warning" | "info";
type AlertType = "success" | "error" | "warning" | "info";
interface StatusMeta {
  tag: TagType;
  alert: AlertType;
  text: string;
}

const STATUS_META: Record<LicenseStatus, StatusMeta> = {
  VALID: { tag: "success", alert: "success", text: "已授权" },
  EMERGENCY: { tag: "danger", alert: "error", text: "应急授权中" },
  GRACE: { tag: "warning", alert: "warning", text: "宽限期" },
  TRIAL: { tag: "warning", alert: "warning", text: "试用中（未授权）" },
  TRIAL_EXPIRED: { tag: "danger", alert: "error", text: "试用已到期" },
  MISMATCH: { tag: "danger", alert: "error", text: "机器不匹配" },
  EXPIRED: { tag: "danger", alert: "error", text: "授权已过期" },
};

const LOADING_META: StatusMeta = { tag: "info", alert: "info", text: "获取中" };

const statusMeta = computed<StatusMeta>(() => {
  const s = info.value?.status;
  return s ? STATUS_META[s] : LOADING_META;
});

const TYPE_TEXT: Record<string, string> = {
  PERSONAL: "个人版",
  ENTERPRISE: "企业版",
  OEM: "OEM 定制版",
  EMERGENCY: "应急码",
};
const typeText = computed(() => (info.value?.type ? TYPE_TEXT[info.value.type] ?? info.value.type : "-"));

const sourceText = computed(() => {
  switch (info.value?.source) {
    case "ENV":
      return "环境变量（只读）";
    case "FILE":
      return "页面激活";
    default:
      return "无";
  }
});

const expiresText = computed(() => {
  if (!hasLicense.value) return "-";
  return info.value?.expiresAt ? formatTs(info.value.expiresAt) : "永久有效";
});

onMounted(() => {
  licenseStore.fetchStatus().catch(() => undefined);
});

async function handleActivate() {
  const key = licenseKey.value.trim();
  if (!key) {
    ElMessage.warning("请先粘贴授权码");
    return;
  }
  loading.value = true;
  try {
    const data = await licenseStore.activate(key);
    licenseKey.value = "";
    ElMessage.success(`授权激活成功（${data.licensee ?? "未知授权对象"}）`);
  } catch (err) {
    ElMessage.error(extractError(err));
  } finally {
    loading.value = false;
  }
}

async function handleClear() {
  try {
    await ElMessageBox.confirm("清除后将回到试用态，确定继续？", "提示", { type: "warning" });
  } catch {
    return; // 用户取消
  }
  loading.value = true;
  try {
    await licenseStore.clear();
    ElMessage.success("已清除授权，回到试用态");
  } catch (err) {
    ElMessage.error(extractError(err));
  } finally {
    loading.value = false;
  }
}

async function copyFingerprint() {
  const fp = info.value?.fingerprint;
  if (!fp) {
    ElMessage.warning("机器码尚未生成，请稍后再试");
    return;
  }
  try {
    await navigator.clipboard.writeText(fp);
    ElMessage.success("机器码已复制");
  } catch {
    ElMessage.warning("复制失败，请手动选择机器码复制");
  }
}

function extractError(err: unknown): string {
  if (typeof err === "object" && err !== null) {
    const e = err as { msg?: string; message?: string };
    return e.msg || e.message || "操作失败";
  }
  return "操作失败";
}

function formatTs(ts?: number | null): string {
  if (!ts) return "-";
  const d = new Date(ts);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
</script>

<style lang="scss" scoped>
.license-page {
  .license-card {
    max-width: 860px;
  }
}

.license-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.machine-box {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
}

.machine-label {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.machine-help {
  cursor: help;
}

.machine-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.machine-code {
  flex: 1;
  font-family: "Consolas", "Menlo", monospace;
  font-size: 14px;
  word-break: break-all;
  user-select: all;
}

.license-alert {
  margin-bottom: 16px;
}

.license-info {
  margin-bottom: 16px;
}
</style>
