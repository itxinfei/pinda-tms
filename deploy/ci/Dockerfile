# ============================================
# 品达TMS Gitea Actions 作业镜像
# 内含：JDK8 + Maven3.6 + Docker CLI + Compose v2 + Node14
#   - Maven     ：一次构建全部后端 fat jar
#   - Docker CLI：通过挂载的 /var/run/docker.sock 操作宿主机 Docker 构建/部署
#   - Node14    ：构建 pd-admin-ui（node-sass 4.x 仅兼容 node <=14）
# 采用静态二进制安装，避免各 Linux 发行版 apt 源差异带来的构建失败。
# ============================================
FROM maven:3.6.3-jdk-8

ENV DOCKER_VERSION=24.0.6 \
    COMPOSE_VERSION=v2.20.3 \
    NODE_VERSION=v14.21.3

# Docker CLI（静态包）+ Compose v2 插件
RUN curl -fsSL https://download.docker.com/linux/static/stable/x86_64/docker-${DOCKER_VERSION}.tgz | tar xz -C /tmp \
    && mv /tmp/docker/docker /usr/local/bin/docker \
    && mkdir -p /usr/local/lib/docker/cli-plugins \
    && curl -fsSL -o /usr/local/lib/docker/cli-plugins/docker-compose \
       https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-x86_64 \
    && chmod +x /usr/local/lib/docker/cli-plugins/docker-compose \
    && rm -rf /tmp/docker

# Node 14（含 npm 6，与前端 package-lock v1 兼容）
RUN curl -fsSL https://nodejs.org/dist/${NODE_VERSION}/node-${NODE_VERSION}-linux-x64.tar.xz | tar -xJ -C /opt \
    && ln -s /opt/node-${NODE_VERSION}-linux-x64/bin/node /usr/local/bin/node \
    && ln -s /opt/node-${NODE_VERSION}-linux-x64/bin/npm /usr/local/bin/npm \
    && ln -s /opt/node-${NODE_VERSION}-linux-x64/bin/npx /usr/local/bin/npx

WORKDIR /workspace
