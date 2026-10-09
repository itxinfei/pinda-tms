#!/usr/bin/env bash
# 生成"当前部署了什么 / 最近改了什么"页面，让 push 之后在浏览器里就能核对结果。
# 产物：/var/www/deployinfo/deploy.html 与 deploy.json（宿主 nginx 用 location = 直出）
# 用法: bash deploy/ci/deploy-info.sh [TAG=abc1234]
set -uo pipefail

GITEA=${GITEA:-http://127.0.0.1:3000}
REPO=${REPO:-pinda/pinda-tms}
# 凭据不写进仓库（这是开源仓库）。机器上放 /etc/pinda-gitea-cred（root 0600，内容 user:pass）。
AUTH=${AUTH:-$(cat /etc/pinda-gitea-cred 2>/dev/null || true)}
OUT_DIR=${OUT_DIR:-/var/www/deployinfo}
DEPLOY_TAG=${TAG:-${1:-}}
# 没显式给 TAG 时，用运行中容器实际在用的镜像 tag 当"当前部署版本"
if [ -z "$DEPLOY_TAG" ]; then
  for c in pd-gateway pd-oms pd-admin-h5; do
    t=$(docker inspect -f '{{.Config.Image}}' "$c" 2>/dev/null | awk -F: '{print $2}')
    [ -n "$t" ] && { DEPLOY_TAG="$t"; break; }
  done
fi

# 流水线状态：有没有 job 容器在跑
if docker ps --format '{{.Names}}' 2>/dev/null | grep -q 'GITEA-ACTIONS-TASK'; then
  pipe_state="正在运行"
  pipe_cls="warn"
else
  pipe_state="空闲"
  pipe_cls="ok"
fi

mkdir -p "$OUT_DIR"
now=$(date '+%F %T %Z')

# ---------- 服务清单（镜像 tag / 资源上限 / 健康） ----------
svc_rows=""
svc_json=""
for c in $(docker ps --format '{{.Names}}' | grep -E '^pd-' | sort); do
  img=$(docker inspect -f '{{.Config.Image}}' "$c")
  m=$(docker inspect -f '{{.HostConfig.Memory}}' "$c")
  fd=$(docker inspect -f '{{range .HostConfig.Ulimits}}{{if eq .Name "nofile"}}{{.Hard}}{{end}}{{end}}' "$c")
  hp=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}无探针{{end}}' "$c")
  up=$(docker inspect -f '{{.State.StartedAt}}' "$c" | cut -c1-16)
  mem=$([ "${m:-0}" -gt 0 ] && awk -v x="$m" 'BEGIN{printf "%.0fM", x/1048576}' || echo "无上限")
  [ -n "$fd" ] || fd="默认1024"
  cls="ok"; { [ "${m:-0}" -eq 0 ] || [ "$hp" = "unhealthy" ]; } && cls="warn"
  svc_rows+="<tr class=\"$cls\"><td>$c</td><td>${img##*:}</td><td>$mem</td><td>$fd</td><td>$hp</td><td>$up</td></tr>"
  svc_json+="{\"name\":\"$c\",\"image\":\"$img\",\"mem\":\"$mem\",\"nofile\":\"$fd\",\"health\":\"$hp\"},"
done
svc_json="[${svc_json%,}]"

# ---------- 中间件 ----------
mid_rows=""; mid_ok=0; mid_all=0
for c in mysql57 redis nacos rabbitmq; do
  st=$(docker inspect -f '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{end}}' "$c" 2>/dev/null || echo 缺失)
  mid_all=$((mid_all+1)); case "$st" in *healthy*) mid_ok=$((mid_ok+1));; esac
  mid_rows+="<tr><td>$c</td><td>$st</td></tr>"
done

# ---------- 宿主可达性（网桥网关地址） ----------
bridge_bad=""
for n in $(docker network ls --format '{{.Name}}' | grep -vE '^(host|bridge|none)$'); do
  id=$(docker network inspect "$n" -f '{{.Id}}')
  gw=$(docker network inspect "$n" -f '{{range .IPAM.Config}}{{.Gateway}}{{end}}')
  sub=$(docker network inspect "$n" -f '{{range .IPAM.Config}}{{.Subnet}}{{end}}')
  br="br-${id:0:12}"
  [ -n "$gw" ] || continue
  ip -4 -o addr show dev "$br" 2>/dev/null | grep -q "inet ${gw%/*}/" || bridge_bad+="$n($gw) "
done
[ -z "$bridge_bad" ] && bridge_state="全部网桥网关地址正常" || bridge_state="缺地址的网桥: $bridge_bad"

disk=$(df -h / | awk 'NR==2{print $3" 已用 / "$5" / 剩 "$4}')
memline=$(free -m | awk 'NR==2{printf "用 %dM / 共 %dM（可用 %dM）", $3, $2, $7}')
swapline=$(free -m | awk 'NR==3{printf "%dM / %dM", $3, $2}')

# ---------- 最近提交（含本次部署高亮）+ 本次改了哪些文件 ----------
commit_rows=""; commit_json=""; changed_block=""
tf=$(mktemp); trap 'rm -f "$tf"' EXIT
curl -s -m 12 -u "$AUTH" "$GITEA/api/v1/repos/$REPO/commits?limit=12" -o "$tf" 2>/dev/null
if [ -s "$tf" ] && command -v jq >/dev/null 2>&1 && jq -e '.[0]' "$tf" >/dev/null 2>&1; then
  while IFS=$'\t' read -r sha msg author when; do
    [ -n "$sha" ] || continue
    hl=""; [ -n "$DEPLOY_TAG" ] && [ "$sha" = "$DEPLOY_TAG" ] && hl=' class="cur"'
    commit_rows+="<tr$hl><td><code>$sha</code></td><td>${msg:0:110}</td><td>$author</td><td>$when</td></tr>"
    commit_json+="{\"sha\":\"$sha\",\"msg\":\"${msg//\"/}\",\"author\":\"$author\",\"date\":\"$when\"},"
  done < <(jq -r '.[] | (.sha[0:7]) + "\t" + ((.commit.message // "") | split("\n")[0]) + "\t" + (.commit.author.name // "") + "\t" + ((.commit.author.date // "") | sub("T";" ") | split("+")[0])' "$tf")
  src="Gitea API + jq"
  # 本次部署那次提交的改动文件与增删行（列表接口里就带 files/stats，单个提交端点在 Gitea 1.21 是 404）
  if [ -n "$DEPLOY_TAG" ]; then
    nfile=$(jq -r --arg t "$DEPLOY_TAG" '[.[] | select(.sha | startswith($t)) | (.files // []) | length] | add // 0' "$tf")
    add=$(jq -r --arg t "$DEPLOY_TAG" '[.[] | select(.sha | startswith($t)) | .stats.additions // 0] | add // 0' "$tf")
    del=$(jq -r --arg t "$DEPLOY_TAG" '[.[] | select(.sha | startswith($t)) | .stats.deletions // 0] | add // 0' "$tf")
    files=$(jq -r --arg t "$DEPLOY_TAG" '.[] | select(.sha | startswith($t)) | (.files // [])[] | (.status[0:1] | ascii_upcase) + "  " + .filename + (if .previous_filename then "   (原 " + .previous_filename + ")" else "" end)' "$tf" | head -25)
    if [ "${nfile:-0}" -gt 0 ]; then
      changed_block="<h2>本次部署 <code>$DEPLOY_TAG</code> 改了哪些文件</h2><div class=\"kv\"><div>共 $nfile 个文件</div><div>+$add / -$del 行</div></div><pre>$files</pre>"
      [ "$nfile" -gt 25 ] && changed_block+="<div class=\"note\">仅列出前 25 个，完整清单见 <a href=\"$GITEA/$REPO/commit/$DEPLOY_TAG\">Gitea</a></div>"
    fi
  fi
else
  commit_rows="<tr><td colspan=\"4\">拉取提交列表失败（Gitea API 不可达或未认证）</td></tr>"
  src="不可用"
fi
commit_json="[${commit_json%,}]"

# ---------- 前端与镜像 tag 是否一致 ----------
img_tags=$(docker images --format '{{.Repository}}:{{.Tag}}' | grep '^pinda/pd-' | awk -F: '{print $2}' | sort -u | tr '\n' ' ')

cat > "$OUT_DIR/deploy.html" <<HTML
<!doctype html><html lang="zh"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>品达TMS 部署状态</title>
<style>
 body{font:14px/1.6 -apple-system,"Segoe UI","Microsoft YaHei",sans-serif;margin:24px;color:#222;background:#fafafa}
 h1{font-size:20px;margin:0 0 4px} h2{font-size:15px;margin:22px 0 8px;color:#444}
 .sub{color:#777;margin-bottom:18px}
 table{border-collapse:collapse;width:100%;background:#fff;box-shadow:0 1px 2px rgba(0,0,0,.08)}
 th,td{border-bottom:1px solid #eee;padding:6px 10px;text-align:left;font-size:13px}
 th{background:#f2f2f2;font-weight:600}
 tr.warn td{background:#fff7e6} tr.cur td{background:#e6f4ea;font-weight:600}
 .kv{display:flex;flex-wrap:wrap;gap:8px 26px;background:#fff;padding:12px 14px;box-shadow:0 1px 2px rgba(0,0,0,.08)}
 code{background:#f2f2f2;padding:1px 5px;border-radius:3px}
 .note{color:#888;font-size:12px;margin-top:8px}
 .ok{color:#1a7f37} .warn{color:#b66000;font-weight:600}
 pre{background:#fff;padding:10px 14px;overflow:auto;font-size:12px;box-shadow:0 1px 2px rgba(0,0,0,.08);white-space:pre-wrap}
</style></head><body>
<h1>品达TMS · 部署状态</h1>
<div class="sub">生成于 $now 　|　 本次部署 TAG <code>${DEPLOY_TAG:-未提供}</code> 　|　 数据来自 docker 与 Gitea API，只读</div>

<div class="kv">
 <div>流水线：<span class="$pipe_cls">$pipe_state</span></div>
 <div>根分区：$disk</div>
 <div>内存：$memline</div>
 <div>交换：$swapline</div>
 <div>中间件：$mid_ok/$mid_all healthy</div>
</div>
<div class="note">宿主→容器可达性：$bridge_state</div>

<h2>应用服务（15）</h2>
<table><tr><th>服务</th><th>镜像 tag</th><th>内存上限</th><th>nofile</th><th>健康</th><th>启动时间</th></tr>$svc_rows</table>
<div class="note">标黄 = 无内存上限或探针异常。已用 tag：$img_tags</div>

<h2>中间件</h2>
<table><tr><th>容器</th><th>状态</th></tr>$mid_rows</table>

<h2>最近提交（高亮 = 当前部署版本）</h2>
<table><tr><th>commit</th><th>说明</th><th>作者</th><th>时间</th></tr>$commit_rows</table>
<div class="note">提交清单来源：$src　机器可读版本：<a href="/deploy.json">/deploy.json</a>　刷新本页即可看到最新状态</div>
$changed_block
</body></html>
HTML

cat > "$OUT_DIR/deploy.json" <<JSON
{"generatedAt":"$now","deployTag":"$DEPLOY_TAG","services":$svc_json,"middlewareHealthy":$mid_ok,"middlewareTotal":$mid_all,"bridgeCheck":"$bridge_state","disk":"$disk","memory":"$memline","commits":$commit_json}
JSON

echo "已生成 $OUT_DIR/deploy.html 与 deploy.json（TAG=${DEPLOY_TAG:-未提供}）"
[ -n "$bridge_bad" ] && echo "警告: $bridge_state"
exit 0
