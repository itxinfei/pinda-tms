#!/bin/bash
# ============================================
# 品达TMS - 后端 Java 服务 Docker 镜像构建
# 该脚本由 Jenkins CI/CD 调用，在项目根目录执行
# ============================================

set -e

echo "=========================================="
echo "  后端 Java 服务构建 + Docker 镜像打包"
echo "=========================================="

# 从环境变量获取参数（Jenkins 传入）
DOCKER_REGISTRY="${DOCKER_REGISTRY:-localhost:5000}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
GIT_COMMIT_SHORT="${GIT_COMMIT_SHORT:-$(git rev-parse --short HEAD)}"
BUILD_PROFILE="${BUILD_PROFILE:-docker}"

echo "  Docker 镜像仓库: ${DOCKER_REGISTRY}"
echo "  镜像标签: ${IMAGE_TAG}"
echo "  Git Commit: ${GIT_COMMIT_SHORT}"
echo "  构建环境: ${BUILD_PROFILE}"
echo ""

# 需要构建 Docker 镜像的 Maven 模块（排除 pd-common 和 pd-service-api 这类纯依赖模块）
MODULES=(
    "pd-aggregation"
    "pd-base"
    "pd-dispatch"
    "pd-druid"
    "pd-netty"
    "pd-oms"
    "pd-user"
    "pd-work"
    "pd-authority/pd-apps/pd-auth/pd-auth-server"
    "pd-authority/pd-apps/pd-gateway"
    "pd-web/pd-web-manager"
    "pd-web/pd-web-driver"
    "pd-web/pd-web-courier"
    "pd-web/pd-web-customer"
)

# Dockerfile 路径映射：模块路径 -> Dockerfile 所在目录
declare -A DOCKERFILE_DIRS
DOCKERFILE_DIRS=(
    ["pd-aggregation"]="src/main/docker"
    ["pd-base"]="src/main/docker"
    ["pd-dispatch"]="src/main/docker"
    ["pd-druid"]="src/main/docker"
    ["pd-netty"]="src/main/docker"
    ["pd-oms"]="src/main/docker"
    ["pd-user"]="src/main/docker"
    ["pd-work"]="src/main/docker"
    ["pd-authority/pd-apps/pd-auth/pd-auth-server"]="."
    ["pd-authority/pd-apps/pd-gateway"]="."
    ["pd-web/pd-web-manager"]="src/main/docker"
    ["pd-web/pd-web-driver"]="src/main/docker"
    ["pd-web/pd-web-courier"]="src/main/docker"
    ["pd-web/pd-web-customer"]="src/main/docker"
)

# JAR 包名称映射（模块 -> JAR 文件名）
declare -A JAR_NAMES
JAR_NAMES=(
    ["pd-aggregation"]="pd-aggregation-1.0.0-SNAPSHOT"
    ["pd-base"]="pd-base-1.0.0-SNAPSHOT"
    ["pd-dispatch"]="pd-dispatch-1.0.0-SNAPSHOT"
    ["pd-druid"]="pd-druid-1.0.0-SNAPSHOT"
    ["pd-netty"]="pd-netty-1.0.0-SNAPSHOT"
    ["pd-oms"]="pd-oms-1.0.0-SNAPSHOT"
    ["pd-user"]="pd-user-1.0.0-SNAPSHOT"
    ["pd-work"]="pd-work-1.0.0-SNAPSHOT"
    ["pd-authority/pd-apps/pd-auth/pd-auth-server"]="pd-auth-server-1.0-SNAPSHOT"
    ["pd-authority/pd-apps/pd-gateway"]="pd-gateway-1.0-SNAPSHOT"
    ["pd-web/pd-web-manager"]="pd-web-manager-1.0.0-SNAPSHOT"
    ["pd-web/pd-web-driver"]="pd-web-driver-1.0.0-SNAPSHOT"
    ["pd-web/pd-web-courier"]="pd-web-courier-1.0.0-SNAPSHOT"
    ["pd-web/pd-web-customer"]="pd-web-customer-1.0.0-SNAPSHOT"
)

# 服务名称映射（模块 -> 镜像名）
declare -A IMAGE_NAMES
IMAGE_NAMES=(
    ["pd-aggregation"]="pinda-aggregation"
    ["pd-base"]="pinda-base"
    ["pd-dispatch"]="pinda-dispatch"
    ["pd-druid"]="pinda-druid"
    ["pd-netty"]="pinda-netty"
    ["pd-oms"]="pinda-oms"
    ["pd-user"]="pinda-user"
    ["pd-work"]="pinda-work"
    ["pd-authority/pd-apps/pd-auth/pd-auth-server"]="pinda-auth"
    ["pd-authority/pd-apps/pd-gateway"]="pinda-gateway"
    ["pd-web/pd-web-manager"]="pinda-web-manager"
    ["pd-web/pd-web-driver"]="pinda-web-driver"
    ["pd-web/pd-web-courier"]="pinda-web-courier"
    ["pd-web/pd-web-customer"]="pinda-web-customer"
)

# ============================================
# 第一步：Maven 打包（跳过测试）
# ============================================
echo "[1/2] Maven 打包 Java 服务..."
mvn clean package -DskipTests -Dmaven.javadoc.skip=true -B

if [ $? -ne 0 ]; then
    echo "错误: Maven 构建失败!"
    exit 1
fi
echo "  Maven 构建完成"
echo ""

# ============================================
# 第二步：构建 Docker 镜像
# ============================================
echo "[2/2] 构建 Docker 镜像..."
SUCCESS_COUNT=0
FAIL_COUNT=0

for module in "${MODULES[@]}"; do
    dockerfile_dir="${DOCKERFILE_DIRS[$module]}"
    jar_name="${JAR_NAMES[$module]}"
    image_name="${IMAGE_NAMES[$module]}"

    jar_path="${module}/target/${jar_name}.jar"
    dockerfile_path="${module}/${dockerfile_dir}/Dockerfile"

    # 检查 JAR 包是否存在
    if [ ! -f "$jar_path" ]; then
        echo "  [SKIP] ${module}: JAR 包不存在 (${jar_path})"
        ((FAIL_COUNT++))
        continue
    fi

    # 检查 Dockerfile 是否存在
    if [ ! -f "$dockerfile_path" ]; then
        echo "  [SKIP] ${module}: Dockerfile 不存在 (${dockerfile_path})"
        ((FAIL_COUNT++))
        continue
    fi

    # 构建镜像
    echo "  构建: ${image_name}:${IMAGE_TAG}"
    docker build \
        --build-arg JAR_FILE="target/${jar_name}.jar" \
        -t "${DOCKER_REGISTRY}/${image_name}:${IMAGE_TAG}" \
        -f "${dockerfile_path}" \
        "${module}/${dockerfile_dir#src/main/docker}"

    if [ $? -eq 0 ]; then
        echo "    -> 构建成功"
        ((SUCCESS_COUNT++))
    else
        echo "    -> 构建失败"
        ((FAIL_COUNT++))
    fi
done

echo ""
echo "=========================================="
echo "  构建结果: 成功 ${SUCCESS_COUNT}, 失败 ${FAIL_COUNT}"
echo "=========================================="

if [ $FAIL_COUNT -gt 0 ]; then
    echo "错误: 有 ${FAIL_COUNT} 个模块构建失败"
    exit 1
fi

# ============================================
# 第三步：推送镜像到仓库
# ============================================
echo ""
echo "[3/3] 推送镜像到 Docker 仓库..."

if [ "$DOCKER_REGISTRY" = "localhost:5000" ] || [ "$DOCKER_REGISTRY" = "" ]; then
    echo "  跳过推送（本地仓库模式）"
    exit 0
fi

for module in "${MODULES[@]}"; do
    image_name="${IMAGE_NAMES[$module]}"

    if [ -n "${DOCKERFILE_DIRS[$module]}" ] && [ -f "${module}/target/${JAR_NAMES[$module]}.jar" ]; then
        echo "  推送: ${image_name}:${IMAGE_TAG}"
        docker push "${DOCKER_REGISTRY}/${image_name}:${IMAGE_TAG}"
    fi
done

echo ""
echo "  所有镜像推送完成!"
echo "=========================================="
