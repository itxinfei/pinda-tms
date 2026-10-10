# ============================================
# 品达TMS Gitea Actions 作业镜像
# 内含：JDK21 + Maven3.9 + Docker CLI + Compose v2
#   - Maven     ：一次构建全部后端 fat jar（JDK21 / Spring Boot 3.3.5）
#   - Docker CLI：通过挂载的 /var/run/docker.sock 操作宿主机 Docker 构建/部署
#   - 前端构建在各自 Dockerfile 内的 node 镜像完成，本镜像不再内置 node
# 构建方式（服务器侧，零网络下载——docker/compose 二进制直接复制宿主机）：
#   mkdir -p /tmp/ci-ctx && cp /usr/bin/docker /tmp/ci-ctx/ \
#   && cp /usr/libexec/docker/cli-plugins/docker-compose /tmp/ci-ctx/
#   docker build -f deploy/ci/Dockerfile -t ci/pinda-ci:latest /tmp/ci-ctx
# 为什么不用静态包下载：download.docker.com / github.com 直连及 DaoCloud 加速
# 在该网络下均 short read / HTTP 22 中断，改为复制宿主机已装二进制（版本更新，
# 功能只多不少），彻底移除构建期的外网依赖。
# ============================================
FROM maven:3.9.9-eclipse-temurin-21

# Docker CLI + Compose v2 插件（来自宿主机，构建上下文提供）
COPY docker /usr/local/bin/docker
COPY docker-compose /usr/local/lib/docker/cli-plugins/docker-compose
RUN chmod +x /usr/local/bin/docker /usr/local/lib/docker/cli-plugins/docker-compose \
    && docker --version && docker compose version

WORKDIR /workspace
