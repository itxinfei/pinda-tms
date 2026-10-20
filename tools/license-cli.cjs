#!/usr/bin/env node
/**
 * 心飞TMS 授权密钥签发工具（SEA 单文件版，可打包为 license-generator.exe）。
 *
 * 两种用法：
 *   1. 双击 exe / 无参数运行 → 交互式问答，适合"有人购买时在笔记本上点开就签"；
 *   2. 命令行参数（与 license-batch.mjs 一致）：
 *        license-generator.exe --count 30 --type TRIAL --days 30 \
 *            --batch 20261101 --licensee "公众号体验用户" --out keys.txt
 *      （--interactive 可在脚本/管道中强制走交互问答）
 *
 * 私钥查找顺序（找到即用，绝不外传）：
 *   --private 路径 → 环境变量 XINFEI_LICENSE_KEY → exe 同目录 license-private.pem
 *   → 当前目录 .workbuddy/tmp/license-private.pem
 */

const { createSign, createHash } = require("node:crypto");
const { readFileSync, writeFileSync, appendFileSync, mkdirSync } = require("node:fs");
const path = require("node:path");
const readline = require("node:readline");

const EXE_DIR = path.dirname(process.execPath);
const DAY_MS = 86_400_000;

// ---------- 行队列 asker（TTY 可回显；管道提前结束则回落默认值，稳定不挂） ----------
function makeAsker() {
  const queue = [];
  const waiters = [];
  let closed = false;
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdin.isTTY ? process.stdout : undefined,
    terminal: !!process.stdin.isTTY,
  });
  rl.on("line", (l) => {
    const w = waiters.shift();
    if (w) w(l.trim());
    else queue.push(l.trim());
  });
  rl.on("close", () => {
    closed = true;
    while (waiters.length) waiters.shift()("");
  });
  return async function ask(question, def) {
    const suffix = def !== undefined && def !== "" ? `（回车=${def}）` : "";
    process.stdout.write(`  ${question}${suffix}: `);
    let ans;
    if (queue.length) ans = queue.shift();
    else if (closed) ans = "";
    else ans = await new Promise((res) => waiters.push(res));
    return ans === "" ? def : ans;
  };
}

let askFn = null;
const isTTY = !!(process.stdin && process.stdin.isTTY);

async function pauseIfTTY() {
  if (!isTTY) return;
  try {
    askFn = askFn || makeAsker();
    await askFn("按回车键退出", "");
  } catch {
    /* ignore */
  }
}

// ---------- 私钥解析 ----------
function findPrivateKey(explicit) {
  const candidates = [];
  if (explicit) candidates.push(explicit);
  if (process.env.XINFEI_LICENSE_KEY) candidates.push(process.env.XINFEI_LICENSE_KEY);
  candidates.push(path.join(EXE_DIR, "license-private.pem"));
  candidates.push(path.resolve(".workbuddy/tmp/license-private.pem"));
  for (const c of candidates) {
    try {
      return { file: c, key: readFileSync(c, "utf8") };
    } catch {
      /* try next */
    }
  }
  return null;
}

// ---------- 签发核心（与 license-batch.mjs / LicenseVerifier 契约一致） ----------
function generate(opt, privateKeyPem) {
  const now = Date.now();
  const expiresAt = now + opt.days * DAY_MS;
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
    const sigB64 = signer.sign(privateKeyPem).toString("base64url");
    keys.push(payloadB64 + "." + sigB64);
  }

  const header = [
    `# 码种=${opt.type} 有效期=${opt.days}天 数量=${opt.count}`,
    opt.renewable ? `# 模式=可重复激活（每轮${opt.trialDays || 7}天，总开关${opt.days}天）` : `# 模式=一次性（激活后用到到期）`,
    `# 授权对象=${opt.licensee} 到期=${new Date(expiresAt).toISOString()}`,
    opt.batch ? `# 批次=${opt.batch} 序号=01..${String(opt.count).padStart(2, "0")}` : `# 无批次`,
    `# 签发时间=${new Date(now).toISOString()}`,
  ].join("\n");
  const body = keys.map((k, i) => `# ${String(i + 1).padStart(2, "0")}\n${k}`).join("\n\n");

  const expiryDay = new Date(expiresAt).toISOString().slice(0, 10);
  const registryRows = keys.map((k) => {
    const fp = createHash("sha256").update(k, "utf8").digest("hex").slice(0, 16);
    return `| \`${fp}\` | ${opt.type} | ${expiryDay} | ${opt.instances} | ${opt.licensee}（署名需客户同意，默认可脱敏） |`;
  });

  return { keys, text: header + "\n\n" + body, registryRows, expiresAt };
}

// ---------- 参数模式 ----------
function parseArgs() {
  const argv = process.argv.slice(2);
  const get = (name, def) => {
    const i = argv.indexOf(`--${name}`);
    return i >= 0 && argv[i + 1] !== undefined && !argv[i + 1].startsWith("--") ? argv[i + 1] : def;
  };
  return {
    count: parseInt(get("count", "1"), 10),
    type: get("type", "TRIAL"),
    days: parseInt(get("days", "30"), 10),
    renewable: argv.includes("--renewable"),
    trialDays: parseInt(get("trial-days", "7"), 10),
    licensee: get("licensee", "公众号体验用户"),
    instances: parseInt(get("instances", "1"), 10),
    batch: get("batch", ""),
    aud: get("aud", "xinfei-tms"),
    priv: get("private", ""),
    out: get("out", ""),
  };
}

// ---------- 交互模式（双击友好） ----------
async function interactive(opt, found) {
  const ask = askFn || (askFn = makeAsker());
  console.log("\n==============================================");
  console.log("   心飞TMS 授权密钥签发工具");
  console.log("==============================================");
  console.log(`  私钥：${found.file}\n`);
  console.log("  码种：");
  console.log("    1) 体验码 TRIAL      —— 公众号公开发布（可一次性或可重复激活固定码）");
  console.log("    2) 个人码 PERSONAL   —— 个人购买者");
  console.log("    3) 企业码 ENTERPRISE —— 企业客户");
  console.log("    4) OEM 码 OEM        —— 二次开发/贴牌（建议绑定机器指纹再谈）");

  const t = await ask("选择码种 [1-4]", "1");
  opt.type = { 1: "TRIAL", 2: "PERSONAL", 3: "ENTERPRISE", 4: "OEM" }[t] || "TRIAL";

  if (opt.type === "TRIAL") {
    const mode = await ask("体验模式 [1]一次性(激活用到到期) [2]可重复激活(固定码,每轮N天)", "1");
    opt.renewable = mode === "2";
  }
  if (opt.renewable) {
    opt.trialDays = parseInt(await ask("每轮体验天数", "7"), 10) || 7;
    const years = parseInt(await ask("总开关（年，到期后码彻底失效）", "10"), 10) || 10;
    opt.days = years * 365;
  } else {
    const defDays = opt.type === "TRIAL" ? 30 : 365;
    opt.days = parseInt(await ask("有效期（天）", String(defDays)), 10) || defDays;
  }
  opt.count = parseInt(await ask("数量", opt.type === "TRIAL" ? "20" : "1"), 10) || 1;
  opt.licensee =
    (await ask("授权对象（客户全称/公众号体验用户）", opt.type === "TRIAL" ? "公众号体验用户" : "")) ||
    (opt.type === "TRIAL" ? "公众号体验用户" : "未署名客户");
  opt.instances = parseInt(await ask("实例数上限", "1"), 10) || 1;
  if (opt.type !== "TRIAL") {
    opt.batch = await ask("批次号（可空，如 20261101-C001）", "");
  }
  opt.out = await ask("输出文件名（回车=自动命名到 exe 同目录 keys/）", "");
  return opt;
}

// ---------- 主流程 ----------
(async () => {
  const argvRaw = process.argv.slice(2);
  const forceInteractive = argvRaw.includes("--interactive");
  const hasArgs = argvRaw.filter((a) => a !== "--interactive").length > 0;
  let opt = parseArgs();

  const found = findPrivateKey(opt.priv);
  if (!found) {
    console.error("\n[错误] 找不到私钥 license-private.pem。");
    console.error("查找位置：--private 参数 → 环境变量 XINFEI_LICENSE_KEY → exe 同目录 → .workbuddy/tmp/");
    console.error("把作者私钥放到 exe 同目录（命名 license-private.pem）后重试。");
    await pauseIfTTY();
    process.exit(1);
  }

  if ((!hasArgs && isTTY) || forceInteractive) {
    opt = await interactive(opt, found);
  }

  if (!opt.count || opt.count < 1 || opt.count > 500) {
    console.error("count 需在 1-500 之间");
    await pauseIfTTY();
    process.exit(1);
  }

  let result;
  try {
    result = generate(opt, found.key);
  } catch (e) {
    console.error(`[错误] 签发失败：${e.message}（私钥文件可能损坏或不匹配）`);
    await pauseIfTTY();
    process.exit(1);
  }

  console.log("\n" + result.text);
  console.log(
    "\n==== 授权登记簿条目（正式码请复制追加到仓库 AUTH-REGISTRY.md；体验码无需登记）====\n" +
      result.registryRows.join("\n")
  );

  // 输出文件：显式 --out，或交互模式默认自动命名到 exe 同目录 keys/ 下
  let outPath = opt.out;
  if (!outPath && !hasArgs) {
    const stamp = new Date().toISOString().replace(/[-:T]/g, "").slice(0, 14);
    outPath = path.join(EXE_DIR, "keys", `keys-${opt.type.toLowerCase()}-${stamp}.txt`);
  }
  if (outPath) {
    mkdirSync(path.dirname(path.resolve(outPath)), { recursive: true });
    writeFileSync(outPath, result.text + "\n\n" + result.registryRows.join("\n") + "\n", "utf8");
    console.log(`\n✔ 密钥已写入：${path.resolve(outPath)}`);
  }

  // 正式码登记行自动追加到 exe 同目录 registry-pending.txt（防忘抄）
  if (opt.type !== "TRIAL") {
    const pending = path.join(EXE_DIR, "registry-pending.txt");
    mkdirSync(EXE_DIR, { recursive: true });
    appendFileSync(
      pending,
      `\n<!-- ${new Date().toISOString()} ${opt.licensee} -->\n` + result.registryRows.join("\n") + "\n",
      "utf8"
    );
    console.log(`✔ 登记行已追加：${pending}（复制到仓库 AUTH-REGISTRY.md 后可清空）`);
  }

  console.log(
    "\n提示：体验码发公众号自动回复即可；正式码邮件发送并在本地台账记录收款信息（台账勿入公开仓库）。"
  );
  await pauseIfTTY();
})().catch(async (e) => {
  console.error(`[错误] ${e.message}`);
  await pauseIfTTY();
  process.exit(1);
});
