/**
 * 前端授权校验：与后端 xfwl-auth LicenseVerifier 同算法（RS256）、同公钥。
 *
 * license key 格式：base64url(payloadJson) + "." + base64url(signature)
 * 公钥随前端发布（公钥本就公开，仅能验签不能伪造签名）；私钥仅作者本地保管。
 * 激活密钥持久化 localStorage，刷新/重新登录后重新验签恢复。
 */

const LICENSE_KEY_STORAGE = "xinfei_license_key";

/** 与 xfwl-auth/src/main/resources/license-public.pem 保持一致 */
const PUBLIC_KEY_PEM = `-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu/zuYuVrE8kitPs3kuQh
cmSsEKd4jj7yjYnQQ1nNFjp6ySEYvSEEJV+JUPuq1EevcqFhD+hhfIWHTH6WaI5B
tSG+bjMjNT/OoFwVxk0PDvayOtPcAww+eZUIEDMmHQhdiZbGFDtTINUBjXHdg+7S
6EdE9CBj72p8069OMemV4mIaqyUTZfKMJFLuw7Bs0xIOu92j2qqznAuFs+Mi/ZZS
/C+ihI2LaqL+0VUvGvtiJVO9sIsh8n/KjwulFXirVhyGHQosziET0bIgyfQYukQs
E5zdTquQrxzgxC9pYaI4MvAjpNPtvCHRg416PNiG2i2iTVjkzBnMyRBJtBMC00yP
mwIDAQAB
-----END PUBLIC KEY-----`;

/** 验签通过后的授权明文（对齐后端 DecodedLicense） */
export interface DecodedLicense {
  /** 授权对象（企业/个人名称） */
  licensee: string | null;
  /** 许可类型：ENTERPRISE / PERSONAL / OEM */
  type: string | null;
  /**
   * 到期时间戳(ms)；null 表示永久。
   * 可重复激活码（renewable）中此字段是"总开关"：到期后码彻底失效；
   * 每轮体验时长由 trialDays 决定，从激活时刻起算。
   */
  expiresAt: number | null;
  /** 可重复激活码：到期后用同一密钥再次激活即可开启新一轮 */
  renewable: boolean;
  /** 可重复激活码每轮时长（天），默认 7 */
  trialDays: number | null;
}

/** base64url（兼容标准 base64）解码为字节 */
function base64UrlToBytes(input: string): Uint8Array {
  const b64 = input.replace(/-/g, "+").replace(/_/g, "/");
  const pad = b64.length % 4 ? "=".repeat(4 - (b64.length % 4)) : "";
  const bin = atob(b64 + pad);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) {
    bytes[i] = bin.charCodeAt(i);
  }
  return bytes;
}

function pemToDer(pem: string): Uint8Array {
  return base64UrlToBytes(pem.replace(/-----[^-]+-----/g, "").replace(/\s+/g, ""));
}

/** Web Crypto 入参需要真正的 ArrayBuffer（避免 TS 下 Uint8Array 缓冲区泛型不兼容） */
function toBuf(bytes: Uint8Array): ArrayBuffer {
  const out = new ArrayBuffer(bytes.byteLength);
  new Uint8Array(out).set(bytes);
  return out;
}

let cachedPublicKey: CryptoKey | null = null;

async function importPublicKey(): Promise<CryptoKey> {
  cachedPublicKey ??= await crypto.subtle.importKey(
    "spki",
    toBuf(pemToDer(PUBLIC_KEY_PEM)),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["verify"]
  );
  return cachedPublicKey;
}

/**
 * 校验授权密钥：签名 + 有效期。失败抛 Error（message 面向用户展示）。
 */
export async function verifyLicenseKey(rawKey: string): Promise<DecodedLicense> {
  const key = rawKey.trim();
  const parts = key.split(".");
  if (parts.length !== 2 || !parts[0] || !parts[1]) {
    throw new Error("授权密钥格式不正确");
  }
  const [payloadB64, sigB64] = parts;

  const signatureValid = await crypto.subtle.verify(
    { name: "RSASSA-PKCS1-v1_5" },
    await importPublicKey(),
    toBuf(base64UrlToBytes(sigB64)),
    toBuf(new TextEncoder().encode(payloadB64))
  );
  if (!signatureValid) {
    throw new Error("授权密钥无效或已被篡改");
  }

  let payload: Record<string, unknown>;
  try {
    payload = JSON.parse(new TextDecoder().decode(base64UrlToBytes(payloadB64)));
  } catch {
    throw new Error("授权密钥载荷解析失败");
  }

  const expiresAt =
    typeof payload.expiresAt === "number" && payload.expiresAt > 0
      ? payload.expiresAt
      : null;
  if (expiresAt && expiresAt < Date.now()) {
    // 对 renewable 码这是"总开关"（签发时通常给 10 年）；对普通码就是绝对到期
    throw new Error("授权已过期，请联系作者续期");
  }

  return {
    licensee: typeof payload.licensee === "string" ? payload.licensee : null,
    type: typeof payload.type === "string" ? payload.type : null,
    expiresAt,
    renewable: payload.renewable === true,
    trialDays: typeof payload.trialDays === "number" && payload.trialDays > 0 ? payload.trialDays : null,
  };
}

export function getStoredLicenseKey(): string | null {
  try {
    return localStorage.getItem(LICENSE_KEY_STORAGE);
  } catch {
    return null;
  }
}

export function hasStoredLicenseKey(): boolean {
  return !!getStoredLicenseKey();
}

export function setStoredLicenseKey(key: string): void {
  try {
    localStorage.setItem(LICENSE_KEY_STORAGE, key);
  } catch {
    /* 存储不可用时仅本次会话生效 */
  }
}

export function clearStoredLicenseKey(): void {
  try {
    localStorage.removeItem(LICENSE_KEY_STORAGE);
  } catch {
    /* ignore */
  }
}
