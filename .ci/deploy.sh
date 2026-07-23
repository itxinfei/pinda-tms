#!/bin/bash
# ============================================
# 品达TMS - 服务器端一键部署脚本
# 在 CentOS 服务器上执行此脚本进行手动部署
# 使用：bash deploy.sh [IMAGE_TAG]
# 示例：bash deploy.sh v1.0.0-abc12345
# ============================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
DEPLOY_DIR="/opt/pinda-tms"

# 镜像标签：优先使用参数，默认 latest
IMAGE_TAG="${1:-latest}"

echo "=========================================="
echo "  品达TMS 部署脚本"
echo "  镜像标签: ${IMAGE_TAG}"
echo "=========================================="

# ============================================
# 第一步：同步代码到部署目录
# ============================================
echo ""
echo "[1/5] 同步代码到部署目录..."

mkdir -p "${DEPLOY_DIR}"

if [ -d "${DEPLOY_DIR}/.git" ]; then
    echo "  拉取最新代码..."
    cd "${DEPLOY_DIR}"
    git fetch origin
    git reset --hard origin/master
    git clean -fd
else
    echo "  克隆仓库..."
    git clone https://gitee.com/your-org/pinda-tms.git "${DEPLOY_DIR}"
fi

echo "  代码就绪"

# ============================================
# 第二步：同步配置和脚本
# ============================================
echo ""
echo "[2/5] 同步部署配置..."

# 同步 CI 目录（docker-compose, .env, 脚本）
cp -f "${SCRIPT_DIR}/../docker-compose.yml" "${DEPLOY_DIR}/docker-compose.yml"
cp -f "${SCRIPT_DIR}/../.env" "${DEPLOY_DIR}/.env"

# 备份旧配置
if [ -f "${DEPLOY_DIR}/.env" ]; then
    cp "${DEPLOY_DIR}/.env" "${DEPLOY_DIR}/.env.bak.$(date +%Y%m%d%H%M%S)"
fi

echo "  配置同步完成"

# ============================================
# 第三步：初始化/检查 Nacos 配置
# ============================================
echo ""
echo "[3/5] 检查 Nacos 配置..."

if curl -s -o /dev/null -w "%{http_code}" http://localhost:8848/nacos/v1/console/health/readiness | grep -q "200"; then
    echo "  Nacos 已就绪，检查配置..."

    EXISTING=$(curl -s "http://localhost:8848/nacos/v1/cs/configs?dataId=common.yml&group=pinda-tms&namespace=09d0e14f-107f-4fea-8f80-e59e0cc63694" 2>/dev/null || echo "")

    if [ -z "${EXISTING}" ]; then
        echo "  首次部署，推送 Nacos 配置..."
        cd "${DEPLOY_DIR}"
        bash scripts/push-nacos-config.sh
    else
        echo "  Nacos 配置已存在，跳过"
    fi
else
    echo "  [WARN] Nacos 未就绪，请稍后手动执行：cd ${DEPLOY_DIR} && bash scripts/push-nacos-config.sh"
fi

# ============================================
# 第四步：启动/更新 Docker 服务
# ============================================
echo ""
echo "[4/5] 部署微服务..."

cd "${DEPLOY_DIR}"

# 拉取最新镜像（如果有远程仓库）
echo "  拉取镜像..."
docker-compose pull 2>/dev/null || echo "  [INFO] 使用本地镜像"

# 启动/更新服务
echo "  启动服务..."
docker-compose up -d --remove-orphans

echo "  服务已启动"

# ============================================
# 第五步：健康检查
# ============================================
echo ""
echo "[5/5] 健康检查..."

sleep 30

echo ""
echo "  容器状态:"
docker-compose -f "${DEPLOY_DIR}/docker-compose.yml" ps 2>/dev/null || docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "  服务端口检查:"
SERVICES=(
    "Nacos:8848"
    "MySQL:3306"
    "Redis:6379"
    "Kafka:9092"
    "RabbitMQ:5672"
    "网关:8760"
    "管理端:8080"
)

for svc in "${SERVICES[@]}"; do
    name="${svc%%:*}"
    port="${svc##*:}"
    if nc -z -w3 localhost "${port}" 2>/dev/null; then
        echo "    [OK] ${name} (${port})"
    else
        echo "    [--] ${name} (${port})"
    fi
done

# 清理旧镜像
echo ""
echo "清理旧 Docker 镜像..."
docker image prune -f --filter "until=72h" 2>/dev/null || true

# ============================================
# 完成
# ============================================
SERVER_IP=$(hostname -I | awk '{print $1}')

echo ""
echo "=========================================="
echo "  部署完成!"
echo "=========================================="
echo ""
echo "访问地址:"
echo "  Nacos 控制台: http://${SERVER_IP}:8848/nacos  (nacos/nacos)"
echo "  管理端前端:   http://${SERVER_IP}:8080"
echo "  管理端 API:   http://${SERVER_IP}:8161"
echo "  API 网关:     http://${SERVER_IP}:8760"
echo ""
echo "RabbitMQ 管理面板: http://${SERVER_IP}:15672  (admin/admin)"
echo ""
echo "查看日志:  cd ${DEPLOY_DIR} && docker-compose logs -f [服务名]"
echo "重启服务:  cd ${DEPLOY_DIR} && docker-compose restart [服务名]"
echo "停止服务:  cd ${DEPLOY_DIR} && docker-compose stop"
