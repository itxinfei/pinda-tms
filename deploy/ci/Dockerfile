# ============================================
# 品达TMS Gitea Actions 作业镜像
# 内含：JDK21 + Maven3.9 + Docker CLI + Compose v2 + Buildx 插件
#   - Maven     ：一次构建全部后端 fat jar（JDK21 / Spring Boot 3.3.5）
#   - Docker CLI：通过挂载的 /var/run/docker.sock 操作宿主机 Docker 构建/部署
#   - Buildx    ：docker 29 CLI 无 buildx 时 docker build 回退 legacy builder，
#                 遇 Dockerfile 的 RUN --mount=type=cache 报
#                 "the --mount option requires BuildKit"（2026-10-10 CI 失败根因）
#   - 前端构建在各自 Dockerfile 内的 node 镜像完成，本镜像不再内置 node
# 构建方式（服务器侧，零网络下载——docker/compose/buildx 二进制直接复制宿主机）：
#   mkdir -p /tmp/ci-ctx && cp /usr/bin/docker /tmp/ci-ctx/ \
#   && cp /usr/libexec/docker/cli-plugins/docker-compose /tmp/ci-ctx/ \
#   && cp /usr/libexec/docker/cli-plugins/docker-buildx /tmp/ci-ctx/
#   docker build -f deploy/ci/Dockerfile -t ci/pinda-ci:latest /tmp/ci-ctx
# 为什么不用静态包下载：download.docker.com / github.com 直连及 DaoCloud 加速
# 在该网络下均 short read / HTTP 22 中断，改为复制宿主机已装二进制（版本更新，
# 功能只多不少），彻底移除构建期的外网依赖。
# ============================================
FROM maven:3.9.9-eclipse-temurin-21

# Docker CLI + Compose v2 + Buildx 插件（来自宿主机，构建上下文提供）
COPY docker /usr/local/bin/docker
COPY docker-compose /usr/local/lib/docker/cli-plugins/docker-compose
COPY docker-buildx /usr/local/lib/docker/cli-plugins/docker-buildx
RUN chmod +x /usr/local/bin/docker \
        /usr/local/lib/docker/cli-plugins/docker-compose \
        /usr/local/lib/docker/cli-plugins/docker-buildx \
    && docker --version && docker compose version && docker buildx version

WORKDIR /workspace
