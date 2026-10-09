// GFM 表格结构体检：缺表头分隔行 / 列数与表头不一致 / 单元格内裸竖线切列
// 用法: node lint-md-tables.js <文件或目录> [...]     目录 = 递归全部 .md
// 退出码: 0 = 全部通过, 1 = 有问题（可直接进 CI）
const fs = require('fs');
const path = require('path');

const fence = /^\s*(```|~~~)/;
function isRow(l) { return /^\s*\|/.test(l); }
function isDelim(l) { return /^\s*\|[\s:|-]+\|\s*$/.test(l); }
function pipes(l) { const m = l.match(/(?:^|[^\\])\|/g); return m ? m.length : 0; }

function checkFile(p) {
  const raw = fs.readFileSync(p, 'utf8');
  const lines = raw.split(/\r?\n/);
  const inFence = new Array(lines.length).fill(false);
  let open = false;
  for (let i = 0; i < lines.length; i++) {
    if (fence.test(lines[i])) { open = !open; inFence[i] = true; continue; }
    inFence[i] = open;
  }
  let blocks = 0, issues = 0;
  const hit = (no, msg) => { issues++; console.log('  L' + no + ' ' + msg); };
  let i = 0;
  while (i < lines.length) {
    if (!inFence[i] && isRow(lines[i])) {
      const start = i;
      const rows = [];
      while (i < lines.length && !inFence[i] && isRow(lines[i])) { rows.push(lines[i]); i++; }
      if (rows.length === 1 && !/^\s*\|[-\s|:]+\|\s*$/.test(rows[0])) {
        // 单行"伪表格"多为引用块里的竖线（如 `> |a|b|`），不当表块
        continue;
      }
      blocks++;
      if (!(rows.length > 1 && isDelim(rows[1]))) {
        hit(start + 1, '缺表头分隔行（' + rows.length + ' 行，整块无法渲染）: ' + rows[0].slice(0, 60));
        continue;
      }
      const base = pipes(rows[0]);
      rows.forEach((r, k) => {
        if (pipes(r) !== base) {
          hit(start + 1 + k, '竖线=' + pipes(r) + '（表头 ' + base + '）: ' + r.slice(0, 110));
        }
      });
      continue;
    }
    i++;
  }
  const cr = (raw.match(/\r/g) || []).length, lf = (raw.match(/\n/g) || []).length;
  const mixed = cr > 0 && cr !== lf;
  console.log('=> ' + p.split(/[\\/]/).pop() + '  表块=' + blocks + '  问题=' + issues + (mixed ? '  ⚠️ 行尾混排 CR=' + cr + ' LF=' + lf : ''));
  return issues;
}

function collect(p, out) {
  const st = fs.statSync(p);
  if (st.isDirectory()) {
    for (const e of fs.readdirSync(p, { withFileTypes: true })) {
      if (e.name === 'node_modules' || e.name === '.git' || e.name === 'target') continue;
      collect(path.join(p, e.name), out);
    }
  } else if (p.endsWith('.md')) out.push(p);
  return out;
}

const args = process.argv.slice(2);
if (args.length === 0) {
  console.log('用法: node lint-md-tables.js <文件或目录> [...]');
  process.exit(2);
}
let files = [];
for (const a of args) {
  if (!fs.existsSync(a)) { console.log('!!! 不存在: ' + a); process.exit(2); }
  files = files.concat(collect(a, []));
}
let total = 0;
for (const f of files) total += checkFile(f);
console.log('### 文件数=' + files.length + '  问题总数=' + total);
process.exit(total === 0 ? 0 : 1);
