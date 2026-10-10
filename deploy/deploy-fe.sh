#!/bin/bash
# ============================================
# 品达TMS 前端手动部署脚本（4 个 H5 端）
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

TAG="manual-$(date +%Y%m%d%H%M%S)"
echo ">> 本次构建 TAG=$TAG"

deploy_one() {
  local sub=$1 img=$2
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
  echo ">> [$sub] 滚动更新容器 ..."
  export PD_ADMIN_H5_TAG="$TAG" PD_DRIVER_H5_TAG="$TAG" PD_COURIER_H5_TAG="$TAG" PD_CUSTOMER_H5_TAG="$TAG"
  docker compose --env-file "$APPS_ENV" -f "$COMPOSE" up -d "$img"
  # 等健康（最多 60s）
  for i in $(seq 1 12); do
    st=$(docker inspect -f '{{.State.Health.Status}}' "$img" 2>/dev/null || echo missing)
    [ "$st" = "healthy" ] && break
    sleep 5
  done
  echo ">> [$sub] 健康状态: $(docker inspect -f '{{.State.Health.Status}}' "$img" 2>/dev/null)"
  # 清理旧镜像：保留当前 tag + 上一个，更旧的删掉
  OLD=$(docker images "pinda/$img" --format '{{.Tag}}' | grep -v "^$TAG$" | tail -n +3)
  if [ -n "$OLD" ]; then
    echo "$OLD" | xargs -r -I{} docker rmi "pinda/$img:{}" 2>/dev/null || true
  fi
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
