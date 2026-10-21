import { defineStore } from "pinia";
import { ref } from "vue";
import {
  verifyLicenseKey,
  getStoredLicenseKey,
  setStoredLicenseKey,
  clearStoredLicenseKey,
  type DecodedLicense,
} from "@/utils/license";

const ACTIVATED_AT_STORAGE = "xinfei_license_activated_at";
const DAY_MS = 86_400_000;
const DEFAULT_TRIAL_DAYS = 7;

function readActivatedAt(): number {
  try {
    return Number(localStorage.getItem(ACTIVATED_AT_STORAGE)) || 0;
  } catch {
    return 0;
  }
}

function writeActivatedAt(ts: number): void {
  try {
    localStorage.setItem(ACTIVATED_AT_STORAGE, String(ts));
  } catch {
    /* 存储不可用时本轮时长只在会话内有效 */
  }
}

function clearActivatedAt(): void {
  try {
    localStorage.removeItem(ACTIVATED_AT_STORAGE);
  } catch {
    /* ignore */
  }
}

/**
 * 授权状态 store：前端验签（与后端 LicenseVerifier 同算法同公钥），
 * 激活密钥持久化 localStorage，刷新/重新登录后由 restore() 重新验签恢复。
 * activated 驱动全局「体验版」水印与体验提示的显隐。
 *
 * 可重复激活码（renewable）：固定体验码每轮 trialDays（默认 7）天，
 * 到期后不清除密钥、置 expired=true（回体验版水印），用户用同一密钥
 * 重新激活（页面点"重新激活"或重贴密钥）即开启新一轮。
 */
export const useLicenseStore = defineStore("license", () => {
  const activated = ref(false);
  const licensee = ref<string | null>(null);
  const type = ref<string | null>(null);
  const expiresAt = ref<number | null>(null);
  const renewable = ref(false);
  /** renewable 码本轮已到期（密钥仍保留，可重新激活） */
  const expired = ref(false);

  let restorePromise: Promise<void> | null = null;

  function apply(info: DecodedLicense, effectiveExpiresAt: number | null): void {
    activated.value = true;
    licensee.value = info.licensee;
    type.value = info.type;
    expiresAt.value = effectiveExpiresAt;
    renewable.value = info.renewable;
    expired.value = false;
  }

  /** 本轮到期态：保留密钥供重新激活，但回到体验版（水印/提示） */
  function applyExpiredRound(info: DecodedLicense, roundEnd: number): void {
    activated.value = false;
    expired.value = true;
    renewable.value = true;
    licensee.value = info.licensee;
    type.value = info.type;
    expiresAt.value = roundEnd;
  }

  function reset(): void {
    activated.value = false;
    licensee.value = null;
    type.value = null;
    expiresAt.value = null;
    renewable.value = false;
    expired.value = false;
  }

  /** 应用启动时恢复授权状态：读持久化密钥并重新验签（并发调用只验一次） */
  function restore(): Promise<void> {
    restorePromise ??= (async () => {
      const key = getStoredLicenseKey();
      if (!key) return;
      try {
        const info = await verifyLicenseKey(key);
        if (info.renewable) {
          const trialDays = info.trialDays ?? DEFAULT_TRIAL_DAYS;
          let at = readActivatedAt();
          if (!at) {
            // 兼容：激活时间未记录（旧版本激活），从现在起算新一轮
            at = Date.now();
            writeActivatedAt(at);
          }
          const roundEnd = at + trialDays * DAY_MS;
          if (Date.now() < roundEnd) {
            apply(info, roundEnd);
          } else {
            applyExpiredRound(info, roundEnd);
          }
        } else {
          apply(info, info.expiresAt);
        }
      } catch {
        // 密钥失效/总开关过期：清除并回到体验版
        clearStoredLicenseKey();
        clearActivatedAt();
        reset();
      }
    })();
    return restorePromise;
  }

  /** 激活：验签通过才持久化，返回解析出的授权信息。renewable 码从现在起算新一轮 */
  async function activate(rawKey: string): Promise<DecodedLicense> {
    const info = await verifyLicenseKey(rawKey);
    const key = rawKey.trim();
    setStoredLicenseKey(key);
    const now = Date.now();
    let effectiveExpiresAt: number | null;
    if (info.renewable) {
      writeActivatedAt(now);
      effectiveExpiresAt = now + (info.trialDays ?? DEFAULT_TRIAL_DAYS) * DAY_MS;
    } else {
      clearActivatedAt();
      effectiveExpiresAt = info.expiresAt;
    }
    apply(info, effectiveExpiresAt);
    return info;
  }

  /** renewable 码本轮到期后，用已保存的同一密钥重新激活新一轮 */
  async function reactivate(): Promise<DecodedLicense> {
    const key = getStoredLicenseKey();
    if (!key) {
      throw new Error("没有已保存的体验码，请粘贴密钥激活");
    }
    return activate(key);
  }

  /** 清除授权（回到体验版，供作者调试/换绑密钥） */
  function deactivate(): void {
    clearStoredLicenseKey();
    clearActivatedAt();
    reset();
    restorePromise = Promise.resolve();
  }

  return {
    activated,
    licensee,
    type,
    expiresAt,
    renewable,
    expired,
    restore,
    activate,
    reactivate,
    deactivate,
  };
});
