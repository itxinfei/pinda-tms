#!/bin/bash
# ============================================
# 心飞TMS 前端手动部署脚本（4 个 H5 端）
# 用法: bash deploy-fe.sh [admin|driver|courier|customer|all]
#   不带参数 = all（4 端全部署）
# 流程: 从 Gitea 拉最新代码 -> docker build 目标端镜像 -> compose 滚动更新对应容器
# 说明: 2026-10-10 起前端改为手动部署（自动 push 不再触发前端构建）
# ============================================
set -e
APP="${1:-all}"
GIT_URL="http://192.168.20.130:3000/pinda/pinda-tms.git"
REPO="${FE_REPO_DIR:-/data/pinda-tms}"
APPS_ENV="${FE_APPS_ENV:-/data/ci/apps.env}"
COMPOSE=deploy/apps/docker-compose.app.yml

# 1. 同步最新代码
if [ ! -d "$REPO/.git" ]; then
  echo ">> 首次 clone 代码到 $REPO ..."
  git clone --depth 1 -b master "$GIT_URL" "$REPO"
else
  echo ">> 拉取最新代码 ..."
  cd "$REPO" && git pull --ff-only
fi
cd "$REPO"

# TAG 绑定 commit SHA（可回溯构建来源），附时间戳区分同 commit 重复部署
TAG="manual-$(git rev-parse --short HEAD)-$(date +%Y%m%d%H%M)"
export TAG   # compose 全文件插值：非本次部署的后端服务也带 ${TAG:?}，必须可解析，否则 up 报 required variable 缺失
echo ">> 本次构建 TAG=$TAG"

deploy_one() {
  local sub=$1 img=$2
  local tagvar prev st i ok
  tagvar="$(echo "$img" | tr 'a-z-' 'A-Z_')_TAG"
  echo "===================="
  echo ">> [$sub] 构建镜像 pinda/$img:$TAG ..."
  if [ "$sub" = "admin" ]; then
    docker build -f deploy/apps/Dockerfile.admin-h5 -t "pinda/$img:$TAG" .
  else
    docker build -f deploy/apps/Dockerfile.mobile-h5 \
      --build-arg APP_PATH=pd-mobile \
      --build-arg APP_SUBDIR="$sub" \
      --build-arg APP_NAME="pd-mobile-$sub" \
      -t "pinda/$img:$TAG" .
  fi
  # 记录回滚基线：当前运行镜像 tag（无运行容器=首次部署，无基线则不回滚）
  prev=$(docker inspect -f '{{.Config.Image}}' "$img" 2>/dev/null | sed -n 's/.*://p')
  echo ">> [$sub] 回滚基线 PREV=${prev:-（首次部署，无基线）}"
  echo ">> [$sub] 滚动更新容器 ..."
  export PD_ADMIN_H5_TAG="$TAG" PD_DRIVER_H5_TAG="$TAG" PD_COURIER_H5_TAG="$TAG" PD_CUSTOMER_H5_TAG="$TAG"
  # 容器归属检查：compose 文件声明 name: pinda-apps；历史手动 docker run 的容器无归属，
  # 会让 compose 报容器名冲突。非 pinda-apps 归属的目标容器先删除，改由 compose 接管。
  proj=$(docker inspect -f '{{index .Config.Labels "com.docker.compose.project"}}' "$img" 2>/dev/null || echo "")
  if [ "$proj" != "pinda-apps" ]; then
    echo ">> [$sub] 旧容器 $img 非 pinda-apps 管理（proj='$proj'），先删除由 compose 接管"
    docker rm -f "$img" 2>/dev/null || true
  fi
  docker compose --env-file "$APPS_ENV" -f "$COMPOSE" up -d "$img"
  # 等健康（最多 60s）
  ok=0
  for i in $(seq 1 12); do
    st=$(docker inspect -f '{{.State.Health.Status}}' "$img" 2>/dev/null || echo missing)
    [ "$st" = "healthy" ] && { ok=1; break; }
    sleep 5
  done
  echo ">> [$sub] 健康状态: $(docker inspect -f '{{.State.Health.Status}}' "$img" 2>/dev/null)"
  if [ "$ok" != "1" ]; then
    echo "!! [$sub] 部署后未健康"
    if [ -n "$prev" ]; then
      echo ">> [$sub] 回滚到 PREV=$prev"
      env "$tagvar=$prev" docker compose --env-file "$APPS_ENV" -f "$COMPOSE" up -d "$img"
      for i in $(seq 1 12); do
        st=$(docker inspect -f '{{.State.Health.Status}}' "$img" 2>/dev/null || echo missing)
        [ "$st" = "healthy" ] && break
        sleep 5
      done
      echo ">> [$sub] 回滚后健康: $(docker inspect -f '{{.State.Health.Status}}' "$img" 2>/dev/null)"
    else
      echo "!! [$sub] 首次部署无回滚基线，需人工处理"
    fi
    exit 1
  fi
  # 清理旧镜像：保留当前 tag 与回滚基线 prev（其余删；代码在 git，旧构建产物无保留价值）
  OLD=$(docker images "pinda/$img" --format '{{.Tag}}' | grep -vE "^${TAG}$|^${prev}$")
  if [ -n "$OLD" ]; then
    echo "$OLD" | xargs -r -I{} docker rmi "pinda/$img:{}" 2>/dev/null || true
  fi
  # 控制前端构建缓存累积（保留 5g 近期缓存兼顾构建速度；后端流水线尾部另有全量清理）
  docker builder prune -f --keep-storage 5g >/dev/null 2>&1 || true
}

case "$APP" in
  admin)    deploy_one admin pd-admin-h5 ;;
  driver)   deploy_one driver pd-driver-h5 ;;
  courier)  deploy_one courier pd-courier-h5 ;;
  customer) deploy_one customer pd-customer-h5 ;;
  all)
    deploy_one admin pd-admin-h5
    deploy_one driver pd-driver-h5
    deploy_one courier pd-courier-h5
    deploy_one customer pd-customer-h5
    ;;
  *) echo "用法: bash deploy-fe.sh [admin|driver|courier|customer|all]"; exit 1 ;;
esac
echo ">> 部署完成！"

# ============================================================
# 【预留·暂未启用】前端部署结果通知（企业微信 / 邮件 / 通用 webhook）
#   用户暂无企微/邮件，先留坑。以后接入：取消下方注释，
#   并把 DEPLOY_NOTIFY_WEBHOOK 配到 /data/ci/apps.env（0600）或仓库 secret。
# notify() {
#   local st="$1"
#   [ -n "${DEPLOY_NOTIFY_WEBHOOK:-}" ] || { echo "未配置 DEPLOY_NOTIFY_WEBHOOK，跳过通知"; return 0; }
#   curl -sS -X POST "$DEPLOY_NOTIFY_WEBHOOK" -H 'Content-Type: application/json' \
#     -d "{\"msgtype\":\"text\",\"text\":{\"content\":\"[pinda-tms] deploy-fe $st TAG=$TAG\"}}" || true
# }
# 用法示例：部署成功后 notify "成功"；失败分支 notify "失败"
# ============================================================
