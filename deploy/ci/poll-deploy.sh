#!/usr/bin/env bash
#
# poll-deploy.sh —— 轮询 Gitea 部署流水线(commit status)状态，可选做容器端口健康检查
#
# 用法:
#   ./poll-deploy.sh [COMMIT_SHA] [--health]
#
#   COMMIT_SHA   要监控的提交(默认取当前 HEAD)。也可用分支名，脚本会解析为 sha。
#   --health     流水线成功后，额外对容器内网端口做 HTTP 健康检查。
#
# 环境变量(均可选，默认值来自内网 Gitea):
#   GITEA_URL      默认 http://192.168.20.130:3000
#   REPO           默认 pinda/pinda-tms
#   GITEA_USER     默认 pinda
#   GITEA_PASS     默认 pinda123
#   POLL_INTERVAL  轮询间隔(秒) 默认 15
#   TIMEOUT        总超时(秒) 默认 600
#   DEPLOY_HOST    健康检查目标主机 默认 192.168.20.130
#
set -euo pipefail

GITEA_URL="${GITEA_URL:-http://192.168.20.130:3000}"
REPO="${REPO:-pinda/pinda-tms}"
GITEA_USER="${GITEA_USER:-pinda}"
GITEA_PASS="${GITEA_PASS:-pinda123}"
POLL_INTERVAL="${POLL_INTERVAL:-15}"
TIMEOUT="${TIMEOUT:-600}"
HOST="${DEPLOY_HOST:-192.168.20.130}"
# 容器端口清单：前端 4 端 + 关键后端(gateway/auth)
PORTS=(8081 8082 8083 8084 8760 9000)

HEALTH=0
ARGS=()
for a in "$@"; do
  case "$a" in
    --health) HEALTH=1 ;;
    *) ARGS+=("$a") ;;
  esac
done

SHA="${ARGS[0]:-}"
if [ -z "$SHA" ]; then
  SHA=$(git rev-parse HEAD 2>/dev/null || true)
fi
if [ -z "$SHA" ]; then
  echo "错误: 未提供 COMMIT_SHA 且当前不在 git 仓库" >&2
  exit 1
fi

echo ">> 轮询 $REPO @ $SHA 的部署状态 (每 ${POLL_INTERVAL}s, 超时 ${TIMEOUT}s)"

elapsed=0
result=""
while [ "$elapsed" -lt "$TIMEOUT" ]; do
  resp=$(curl -s -u "$GITEA_USER:$GITEA_PASS" \
    "$GITEA_URL/api/v1/repos/$REPO/commits/$SHA/statuses")
  # 取最后一条 status（即最新一次更新）
  last=$(printf '%s' "$resp" | grep -o '"status":"[a-zA-Z]*"' | tail -1 | sed 's/"status":"//;s/"//')
  if [ -z "$last" ]; then
    echo "   [$(date +%H:%M:%S)] 暂无 status 记录，继续等待…"
  else
    echo "   [$(date +%H:%M:%S)] status=$last"
    case "$last" in
      success) result="success"; break ;;
      failure|error) result="failure"; break ;;
    esac
  fi
  sleep "$POLL_INTERVAL"
  elapsed=$((elapsed + POLL_INTERVAL))
done

if [ "$result" = "success" ]; then
  echo "✅ 部署流水线成功"
elif [ "$result" = "failure" ]; then
  echo "❌ 部署流水线失败" >&2
  exit 1
else
  echo "⏱ 超时未得出结论 (仍 pending 或未知)" >&2
  exit 2
fi

if [ "$HEALTH" -eq 1 ]; then
  echo ">> 容器端口健康检查 ($HOST):"
  for p in "${PORTS[@]}"; do
    code=$(curl -s -o /dev/null -m 5 -w '%{http_code}' "http://$HOST:$p/" || echo "000")
    case "$code" in
      200|401|404) echo "   :$p -> $code ✅" ;;
      *)           echo "   :$p -> $code ❌" ;;
    esac
  done
fi
