#!/bin/bash
# ============================================
# 品达TMS DevOps 一键初始化
#   构建 CI 作业镜像 -> 启动 Gitea -> 创建管理员
# 用法：在【仓库根目录】执行
#   bash deploy/server/setup-devops.sh
# 可选变量：
#   GITEA_ADMIN_USER=pinda GITEA_ADMIN_PASS=pinda123 bash deploy/server/setup-devops.sh
# ============================================
set -e

GITEA_URL="http://192.168.20.130:3000"
GITEA_ADMIN_USER="${GITEA_ADMIN_USER:-pinda}"
GITEA_ADMIN_PASS="${GITEA_ADMIN_PASS:-pinda123}"
GITEA_ADMIN_EMAIL="${GITEA_ADMIN_EMAIL:-pinda@local}"

echo "[1/3] 构建 CI 作业镜像 ci/pinda-ci:latest"
docker build -f deploy/ci/Dockerfile -t ci/pinda-ci:latest .

echo "[2/3] 启动 Gitea"
docker compose -f deploy/gitea/docker-compose.gitea.yml up -d gitea
echo "等待 Gitea 就绪..."
for i in $(seq 1 30); do
  curl -fsS "$GITEA_URL" >/dev/null 2>&1 && break
  sleep 2
done

echo "[3/3] 创建管理员（已存在则提示跳过）"
docker exec --user git gitea gitea admin user create \
  --username "$GITEA_ADMIN_USER" \
  --password "$GITEA_ADMIN_PASS" \
  --email "$GITEA_ADMIN_EMAIL" \
  --must-change-password=false || echo "管理员可能已存在，跳过"

cat <<EOF

Gitea 已就绪： ${GITEA_URL}
登录账号： ${GITEA_ADMIN_USER} / ${GITEA_ADMIN_PASS}

下一步：
  1. 登录后新建仓库 pinda-tms（建空仓库，不要勾选初始化 README）。
  2. 右上角头像 -> 管理面板 -> Actions -> Runners，创建 Runner 并复制【注册令牌】。
  3. 把令牌填入 deploy/gitea/docker-compose.gitea.yml 的 GITEA_RUNNER_REGISTRATION_TOKEN，执行：
       docker compose -f deploy/gitea/docker-compose.gitea.yml up -d
     回到 Runners 页面应看到 pinda-runner 在线（绿点）。
EOF
