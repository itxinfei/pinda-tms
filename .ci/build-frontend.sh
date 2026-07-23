#!/bin/bash
# ============================================
# 品达TMS - 前端管理界面 Docker 镜像构建
# 该脚本由 Jenkins CI/CD 调用，在 pd-admin-ui 目录执行
# ============================================

set -e

echo "=========================================="
echo "  前端管理界面构建 + Docker 镜像打包"
echo "=========================================="

SCRIPT_NAME="build-frontend.sh"
WORK_DIR="$(cd "$(dirname "$0")/../pd-admin-ui" && pwd)"

# 从环境变量获取参数
DOCKER_REGISTRY="${DOCKER_REGISTRY:-localhost:5000}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
GIT_COMMIT_SHORT="${GIT_COMMIT_SHORT:-$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')}"

IMAGE_NAME="pinda-admin-ui"
FULL_IMAGE="${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"

echo "  工作目录: ${WORK_DIR}"
echo "  Docker 镜像: ${FULL_IMAGE}"
echo "  Git Commit: ${GIT_COMMIT_SHORT}"
echo ""

# 检查 Node.js 环境
if ! command -v node &> /dev/null; then
    echo "错误: Node.js 未安装"
    exit 1
fi

echo "[1/3] 安装前端依赖..."
cd "${WORK_DIR}"

# 优先使用 cnpm，fallback 到 npm
if command -v cnpm &> /dev/null; then
    cnpm install
else
    npm install
fi

echo "  依赖安装完成"
echo ""

echo "[2/3] 构建前端静态资源..."

# 修改点：生产环境敏感配置由 CI/CD 注入，仓库不固化生产 IP。
# CI 平台需在构建环境中定义以下变量（例如 Jenkins 凭据 / 流水线变量）：
#   PD_DRUID_AUTHORITY_PROD      生产 Druid 鉴权地址，例如 http://<prod-ip>:8764/druid
#   PD_DRUID_FILE_PROD          生产 Druid 文件地址，例如 http://<prod-ip>:8765/druid
#   PD_PROD_REQUEST_DOMAIN_PREFIX 生产请求域名前缀，例如 http://<prod-ip>:8760
# 未注入时为空字符串，由 .env.* 中的同名变量兜底（本地开发用 .env.*.local 覆盖）。
export VUE_APP_DRUID_AUTHORITY_PROD="${PD_DRUID_AUTHORITY_PROD:-}"
export VUE_APP_DRUID_FILE_PROD="${PD_DRUID_FILE_PROD:-}"
export VUE_APP_PROD_REQUEST_DOMAIN_PREFIX="${PD_PROD_REQUEST_DOMAIN_PREFIX:-}"

npm run build:docker

if [ $? -ne 0 ]; then
    echo "错误: 前端构建失败!"
    exit 1
fi

echo "  前端构建完成 (输出到 pinda/ 目录)"
echo ""

echo "[3/3] 构建 Docker 镜像..."
cd "${WORK_DIR}"

docker build \
    -t "${FULL_IMAGE}" \
    -f Dockerfile \
    .

if [ $? -ne 0 ]; then
    echo "错误: Docker 镜像构建失败!"
    exit 1
fi

echo "  镜像构建完成: ${FULL_IMAGE}"
echo ""

# 推送镜像
if [ "$DOCKER_REGISTRY" != "localhost:5000" ] && [ "$DOCKER_REGISTRY" != "" ]; then
    echo "  推送镜像到 Docker 仓库..."
    docker push "${FULL_IMAGE}"
    echo "  推送完成!"
fi

echo ""
echo "=========================================="
echo "  前端构建完成!"
echo "  镜像: ${FULL_IMAGE}"
echo "=========================================="
