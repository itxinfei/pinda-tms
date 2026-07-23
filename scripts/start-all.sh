#!/bin/bash
# ============================================
# 品达TMS - Docker 全栈启动脚本
# ============================================
set -e

echo "=========================================="
echo "   品达TMS - Docker 全栈启动"
echo "=========================================="
echo ""

# 检查依赖
echo "[1/4] 检查环境..."
if ! command -v docker &> /dev/null; then
    echo "错误: Docker 未安装"
    exit 1
fi
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "错误: Docker Compose 未安装"
    exit 1
fi
if ! command -v mvn &> /dev/null; then
    echo "错误: Maven 未安装"
    exit 1
fi

echo "   Docker: $(docker --version)"
echo ""

# 停止旧容器
echo "[2/4] 清理旧容器..."
docker-compose down 2>/dev/null || true
echo ""

# 构建Java服务JAR包
echo "[3/4] 构建项目..."
if [ -f "mvnw" ]; then
    MVN="./mvnw"
else
    MVN="mvn"
fi

# 跳过测试，打包所有模块
echo "   正在编译打包 (跳过测试)..."
$MVN clean package -DskipTests -Dmaven.javadoc.skip=true -B

if [ $? -ne 0 ]; then
    echo "错误: Maven 构建失败!"
    exit 1
fi
echo "   构建完成"
echo ""

# 启动基础设施
echo "[4/4] 启动服务 (先基础设施，再微服务)..."
docker-compose up -d

echo ""
echo "=========================================="
echo "   启动完成!"
echo "=========================================="
echo ""
echo "服务访问地址:"
echo "  - Nacos 控制台:    http://localhost:8848/nacos  (用户名:nacos / 密码:nacos)"
echo "  - 管理端前端:      http://localhost:8080"
echo "  - 管理端API:       http://localhost:8161"
echo "  - 网关:            http://localhost:8760"
echo "  - MySQL:           localhost:3306 (root/root123)"
echo "  - Redis:           localhost:6379"
echo "  - Kafka:           localhost:9092"
echo ""
echo "微服务端口映射:"
echo "  - 权限认证(pd-auth):     9000"
echo "  - 基础数据(pd-base):     8185"
echo "  - 订单服务(pd-oms):      8186"
echo "  - 配送作业(pd-work):     8187"
echo "  - 用户服务(pd-user):     8189"
echo "  - 智能调度(pd-dispatch): 8190"
echo "  - 数据聚合(pd-aggregation): 8191"
echo "  - GPS轨迹(pd-netty):     8192"
echo "  - Druid监控(pd-druid):   8193"
echo ""
echo "前端端口中转:"
echo "  - 司机端(pd-driver):     8162"
echo "  - 快递员端(pd-courier):  8163"
echo "  - 客户端(pd-customer):   8164"
echo ""
echo "下一步:"
echo "  1. 等待 60 秒让所有服务启动"
echo "  2. 在 Nacos 控制台查看服务是否注册成功"
echo "  3. 访问 http://localhost:8080 使用管理端"
echo ""
echo "常用命令:"
echo "  docker-compose logs -f               # 查看所有日志"
echo "  docker-compose logs -f pd-gateway    # 查看指定服务日志"
echo "  docker-compose ps                     # 查看运行状态"
echo "  docker-compose stop                   # 停止所有服务"
echo "  docker-compose down                   # 停止并删除容器"
