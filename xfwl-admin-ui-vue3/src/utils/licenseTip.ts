import { ElNotification } from "element-plus";
import { useLicenseStore } from "@/stores";

/**
 * 未授权"购买商业授权"提示的展示频率限制：
 * 同一浏览器每 3 天最多弹一次，避免打扰（"偶然弹一下"）。
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
 * 拉取后端授权状态；未激活（试用/到期/机器不匹配等）且距上次提示超 3 天，
 * 则弹一次购买通知。体验版水印严格绑定本通知：弹出期间显示，关闭（手动/超时/点击）
 * 立即消失，正常试用期不显示水印。接口不可达静默失败，不影响业务。
 */
export async function maybeShowLicenseTip(): Promise<void> {
  const store = useLicenseStore();
  try {
    const info = await store.fetchStatus();
    if (info.activated) return;
    if (!throttleAllowed()) return;
    markTipShown();

    const expired = info.status === "TRIAL_EXPIRED" || info.status === "EXPIRED";
    const hideWatermark = () => store.setPurchaseTipVisible(false);
    store.setPurchaseTipVisible(true);
    ElNotification({
      title: expired ? "试用已到期" : "未授权提示",
      message: `${info.message ?? "当前为体验版。"} 如需商用，请购买商业授权。`,
      type: expired ? "error" : "warning",
      duration: 8000,
      showClose: true,
      onClick: () => {
        hideWatermark();
        window.open(BUY_URL, "_blank", "noopener");
      },
      onClose: hideWatermark,
    });
  } catch {
    store.setPurchaseTipVisible(false);
    /* 接口不可达，静默 */
  }
}
