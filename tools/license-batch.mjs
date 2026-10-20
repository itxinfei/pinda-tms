#!/usr/bin/env node
/**
 * 授权密钥批量签发工具（作者侧，需持有私钥，绝不入库私钥）。
 *
 * 用法示例：
 *   # 公众号本期 30 个 7 天体验码，输出到文件
 *   node tools/license-batch.mjs --count 30 --type TRIAL --days 7 \
 *        --batch 2026W43 --licensee "公众号体验用户" --out license-trial-2026W43.txt
 *
 *   # 给某客户签发 1 年企业码（2 实例）
 *   node tools/license-batch.mjs --count 1 --type ENTERPRISE --days 365 \
 *        --licensee "某某物流有限公司" --instances 2
 *
 * 密钥格式与后端 LicenseVerifier / 前端 utils/license.ts 一致：
 *   base64url(payloadJson) + "." + base64url(RSA-SHA256 签名)
 */

import { createSign, createHash } from "node:crypto";
import { readFileSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";

// ---------- 参数解析 ----------
function args() {
  const argv = process.argv.slice(2);
  const get = (name, def) => {
    const i = argv.indexOf(`--${name}`);
    return i >= 0 && argv[i + 1] !== undefined ? argv[i + 1] : def;
  };
  return {
    count: parseInt(get("count", "1"), 10),
    type: get("type", "TRIAL"),
    days: parseInt(get("days", "7"), 10),
    renewable: argv.includes("--renewable"),
    trialDays: parseInt(get("trial-days", "7"), 10),
    licensee: get("licensee", "公众号体验用户"),
    instances: parseInt(get("instances", "1"), 10),
    batch: get("batch", ""),
    aud: get("aud", "xinfei-tms"),
    priv: resolve(get("private", ".workbuddy/tmp/license-private.pem")),
    out: get("out", ""),
  };
}

const opt = args();

if (!opt.count || opt.count < 1 || opt.count > 500) {
  console.error("count 需在 1-500 之间");
  process.exit(1);
}

let privateKey;
try {
  privateKey = readFileSync(opt.priv, "utf8");
} catch {
  console.error(`读取私钥失败：${opt.priv}`);
  console.error("可用 --private 指定路径；私钥绝不入库、绝不外传。");
  process.exit(1);
}

const now = Date.now();
const expiresAt = now + opt.days * 86_400_000;
const keys = [];

for (let i = 1; i <= opt.count; i++) {
    const payload = {
      licensee: opt.licensee,
      type: opt.type,
      instances: opt.instances,
      issuedAt: now,
      expiresAt,
      aud: opt.aud,
    };
    if (opt.renewable) {
      // 可重复激活码：expiresAt 退化为总开关；每轮 trialDays 天从激活时刻起算
      payload.renewable = true;
      payload.trialDays = opt.trialDays || 7;
    }
    if (opt.batch) {
    payload.batchId = opt.batch;
    payload.serial = String(i).padStart(2, "0");
  }

  const payloadB64 = Buffer.from(JSON.stringify(payload), "utf8").toString("base64url");
  const signer = createSign("RSA-SHA256");
  signer.update(payloadB64, "ascii");
  const sigB64 = signer.sign(privateKey).toString("base64url");
  keys.push(payloadB64 + "." + sigB64);
}

// ---------- 输出 ----------
const header = [
  `# 码种=${opt.type} 有效期=${opt.days}天 数量=${opt.count}`,
  `# 授权对象=${opt.licensee} 到期=${new Date(expiresAt).toISOString()}`,
  opt.batch ? `# 批次=${opt.batch} 序号=01..${String(opt.count).padStart(2, "0")}` : `# 无批次`,
  `# 签发时间=${new Date(now).toISOString()}`,
].join("\n");

const body = keys.map((k, i) => `# ${String(i + 1).padStart(2, "0")}\n${k}`).join("\n\n");
console.log(header + "\n\n" + body);

// ---------- 授权登记簿条目（复制到 AUTH-REGISTRY.md；体验码无需登记） ----------
const expiryDay = new Date(expiresAt).toISOString().slice(0, 10);
const registryRows = keys.map((k) => {
  const fp = createHash("sha256").update(k, "utf8").digest("hex").slice(0, 16);
  return `| \`${fp}\` | ${opt.type} | ${expiryDay} | ${opt.instances} | ${opt.licensee}（署名需客户同意，默认可脱敏） |`;
});
console.log(
  "\n==== 授权登记簿条目（正式码请复制追加到仓库 AUTH-REGISTRY.md；体验码无需登记）====\n" +
    registryRows.join("\n")
);

if (opt.out) {
  writeFileSync(opt.out, header + "\n\n" + body + "\n", "utf8");
  console.log(`\n已写入文件：${resolve(opt.out)}`);
}

console.log(
  "\n提示：临时码直接发布到公众号即可；正式码请邮件发送并附激活指引，同时在台账记录批次/授权对象/收款单号，并登记 AUTH-REGISTRY.md（指纹+脱敏名称）。"
);
