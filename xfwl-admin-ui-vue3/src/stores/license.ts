import { defineStore } from "pinia";
import { computed, ref } from "vue";
import LicenseAPI, { type LicenseInfo } from "@/api/license";

// 旧版（前端本地验签）遗留的 localStorage 键，新版授权真伪由后端判定，启动时一次性清理
const LEGACY_KEYS = ["xinfei_license_key", "xinfei_license_activated_at"];
function cleanupLegacyStorage(): void {
  try {
    LEGACY_KEYS.forEach((k) => localStorage.removeItem(k));
  } catch {
    /* localStorage 不可用则忽略 */
  }
}
cleanupLegacyStorage();

/**
 * 授权状态 store：后端状态的只读镜像（授权真伪只由后端判定，前端不验签、不存密钥）。
 * activated 驱动全局水印回落；purchaseTipVisible 仅在"购买商业授权"通知弹出期间为真，
 * 通知关闭立即复位——体验版水印只在该提示窗口内出现，其余时间不显示。
 */
export const useLicenseStore = defineStore("license", () => {
  const info = ref<LicenseInfo | null>(null);
  /** 购买授权通知是否正在展示（控制体验版水印显隐） */
  const purchaseTipVisible = ref(false);

  const status = computed(() => info.value?.status ?? null);
  const activated = computed(() => info.value?.activated ?? false);

  /** 拉取最新授权状态（应用启动、激活/清除后调用） */
  async function fetchStatus(): Promise<LicenseInfo> {
    const data = await LicenseAPI.getStatus();
    info.value = data;
    return data;
  }

  /** 激活：后端校验签名+机器绑定并落盘，成功后状态即后端返回值 */
  async function activate(licenseKey: string): Promise<LicenseInfo> {
    const data = await LicenseAPI.activate(licenseKey);
    info.value = data;
    return data;
  }

  /** 清除落盘授权码，回到试用态 */
  async function clear(): Promise<LicenseInfo> {
    const data = await LicenseAPI.clear();
    info.value = data;
    return data;
  }

  function setPurchaseTipVisible(v: boolean): void {
    purchaseTipVisible.value = v;
  }

  return {
    info,
    status,
    activated,
    purchaseTipVisible,
    fetchStatus,
    activate,
    clear,
    setPurchaseTipVisible,
  };
});
