// 统计方法级 HTTP 映射注解数量（《08-后端接口文档》接口数的可复跑口径）
// 用法: node count-apis.js [相对仓库根的模块路径 ...]   不带参数 = 脚本里 MODULES 的默认清单
// 规则: 类级 @RequestMapping 不计入; 目录含 /feign/ 的接口声明一律剔除（它们也带映射注解，计入会虚增对外接口数）。
// 2026-10-09 基线（与《08-后端接口文档》一致，复跑须逐字相等）:
//   manager 82/13 · driver 11/4 · courier 16/5 · customer 20/8 · netty 8/3 · pd-auth-server 91/14
const fs = require('fs');
const path = require('path');

// 从 cwd 向上找仓库根（以 pd-web + docs 同时存在为标记），避免 __dirname/相对路径带来的坑
function findRoot() {
  let dir = path.resolve(process.cwd());
  for (let i = 0; i < 8; i++) {
    if (fs.existsSync(path.join(dir, 'pd-web')) && fs.existsSync(path.join(dir, 'docs'))) return dir;
    const up = path.dirname(dir);
    if (up === dir) break;
    dir = up;
  }
  throw new Error('找不到仓库根（需同时有 pd-web 与 docs 目录），请在项目内运行或修正');
}

const ROOT = findRoot();

const MODULES = [
  'pd-web/pd-web-manager',
  'pd-web/pd-web-driver',
  'pd-web/pd-web-courier',
  'pd-web/pd-web-customer',
  'pd-netty',
  'pd-authority/pd-apps/pd-auth/pd-auth-server',
  'pd-authority/pd-apps/pd-gateway',
  'pd-dispatch',
  'pd-oms',
  'pd-work',
  'pd-base',
];

function walk(dir, out) {
  let entries;
  try {
    entries = fs.readdirSync(dir, { withFileTypes: true });
  } catch (e) {
    return out;
  }
  for (const e of entries) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) {
      if (e.name === 'target' || e.name === 'node_modules' || e.name === '.git') continue;
      walk(p, out);
    } else if (e.name.endsWith('.java')) {
      out.push(p);
    }
  }
  return out;
}

function countFile(file) {
  const src = fs.readFileSync(file, 'utf8');
  // @FeignClient 声明的接口也带映射注解，但它是"调用方声明"不是"对外端点" ⇒ 整文件剔除
  const isFeign = /@FeignClient\b/.test(src);
  const lines = src.split(/\r?\n/);
  let methods = 0;
  let seenClass = false;
  for (let i = 0; i < lines.length; i++) {
    const t = lines[i].trim();
    if (t.startsWith('//')) continue;
    const m = t.match(/^@(Get|Post|Put|Delete|Patch|Request)Mapping\b/);
    if (!m) continue;
    if (!seenClass) {
      let j = i + 1;
      while (j < lines.length) {
        const n = lines[j].trim();
        if (n === '' || n.startsWith('@') || n.startsWith('*') || n.startsWith('/*') || n.startsWith('//')) {
          j++;
          continue;
        }
        break;
      }
      const next = (lines[j] || '').trim();
      if (/^(public |abstract |final |@).*\b(class|interface|enum)\b/.test(next) || /\bclass\b.*\{/.test(next)) {
        seenClass = true;
        continue;
      }
    }
    methods++;
  }
  return { methods, isFeign };
}

function report(label, rel) {
  const dir = path.join(ROOT, rel);
  if (!fs.existsSync(dir)) {
    console.log('!!! ' + rel + ' 不存在，请修正 MODULES 清单');
    return 0;
  }
  let total = 0;
  let feign = 0;
  const per = [];
  for (const f of walk(dir, [])) {
    const r = countFile(f);
    if (r.methods > 0) {
      const relPath = path.relative(ROOT, f).replace(/\\/g, '/');
      // Feign 声明计入会虚增对外接口数 ⇒ 单独统计，不并入 total
      if (r.isFeign || /\/feign\//.test(relPath)) { feign += r.methods; continue; }
      total += r.methods;
      per.push({ file: relPath, n: r.methods });
    }
  }
  console.log('=== ' + label + '  total=' + total + '  controllers=' + per.length + (feign ? '  (另有 feign 声明 ' + feign + ' 条，已剔除)' : ''));
  per.sort((a, b) => b.n - a.n).forEach((p) => console.log('    ' + String(p.n).padStart(3) + '  ' + p.file));
  return total;
}

const targets = process.argv.slice(2);
let sum = 0;
if (targets.length === 0) {
  for (const m of MODULES) sum += report(m, m);
} else {
  for (const m of targets) sum += report(m, m);
}
console.log('--- 合计 = ' + sum);
