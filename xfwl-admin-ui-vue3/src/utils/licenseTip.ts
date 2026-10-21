import { ElNotification } from "element-plus";
import LicenseAPI, { type LicenseInfo } from "@/api/license";
import { hasStoredLicenseKey } from "@/utils/license";
import { useLicenseStore } from "@/stores";

/**
 * 未授权提示的展示频率限制：
 * 同一浏览器每 3 天最多弹一次，避免一直打扰用户（"偶然弹一下"）。
 */
const TIP_LAST_KEY = "xinfei_license_tip_last";
const TIP_INTERVAL_MS = 3 * 24 * 60 * 60 * 1000;

const BUY_URL = "https://gitee.com/itxinfei/xinfei-tms#商业授权";

function throttleAllowed(): boolean {
  try {
    const last = Number(localStorage.getItem(TIP_LAST_KEY) || "0");
    if (last && Date.now() - last < TIP_INTERVAL_MS) return false;
  } catch {
    // localStorage 不可用时仍允许提示
  }
  return true;
}

function markTipShown(): void {
  try {
    localStorage.setItem(TIP_LAST_KEY, String(Date.now()));
  } catch {
    /* 忽略写入失败 */
  }
}

/**
 * 拉取授权状态，若未授权且距上次提示已超 3 天，则弹一次通知。
 * 已授权（VALID）不弹；可重复激活体验码本轮到期时提示"可重新激活"；
 * 接口不可达静默失败，不影响业务。
 */
export async function maybeShowLicenseTip(): Promise<void> {
  const store = useLicenseStore();
  try {
    await store.restore();
  } catch {
    /* 恢复失败按未授权处理 */
  }
  if (store.activated) return;

  // 可重复激活体验码本轮到期：提示用同一码重新激活（密钥仍在本地）
  if (store.expired) {
    if (!throttleAllowed()) return;
    markTipShown();
    ElNotification({
      title: "本轮体验已到期",
      message: "进入「系统管理 → 授权管理」用同一体验码重新激活，可再体验 7 天；如需正式授权请购买商业授权。",
      type: "error",
      duration: 8000,
      showClose: true,
      onClick: () => window.open(BUY_URL, "_blank", "noopener"),
    });
    return;
  }

  if (hasStoredLicenseKey()) return; // 前端已持有有效授权，不再弹体验提示
  if (!throttleAllowed()) return;

  try {
    const info: LicenseInfo = await LicenseAPI.getStatus();
    if (info.status === "VALID") return;
    markTipShown();

    const expired = info.status === "TRIAL_EXPIRED";
    ElNotification({
      title: expired ? "试用已到期" : "未授权提示",
      message: `${info.message ?? ""} 如需商用，请购买商业授权。`,
      type: expired ? "error" : "warning",
      duration: 8000,
      showClose: true,
      onClick: () => window.open(BUY_URL, "_blank", "noopener"),
    });
  } catch {
    /* 接口不可达，静默 */
  }
}
