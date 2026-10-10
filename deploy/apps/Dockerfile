# ============================================
# 品达TMS 微服务通用镜像（一份复用，全部 14 个 Spring Boot 服务）
#
# 构建上下文 = 只含该服务 jar 的目录（CI 里用硬链接准备，见 .gitea/workflows/deploy-backend.yml）：
#   mkdir -p .ctx/pd-base && ln -f deploy/jars/pd-base.jar .ctx/pd-base/app.jar
#   docker build -f deploy/apps/Dockerfile -t pinda/pd-base:<tag> .ctx/pd-base
#
# 为什么不用仓库根目录当上下文：legacy builder 每次 build 都要把整个上下文打包传给
# daemon，而 deploy/jars 里放着 14 个 fat jar，实测上下文 1.119GB、每个镜像白传一次
# （14 个服务 ≈ 9GB 传输，单镜像多花约 70 秒）。上下文只放一个 jar 后是 ~80MB。
#
# 字体库/时区在 pinda/jre21-fontconfig:1（见 Dockerfile.base，只建一次）。
# ============================================
FROM pinda/jre21-fontconfig:1

# JVM 参数由运行时环境变量 JAVA_OPTS 注入（便于在 compose 中限制内存）
COPY app.jar /app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar"]
