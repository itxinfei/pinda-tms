# ============================================
# 品达TMS 微服务通用镜像（一份复用，全部 14 个 Spring Boot 服务）
# 构建上下文为【仓库根目录】，通过 JAR_FILE 传入各模块产出的 fat jar：
#   docker build -f deploy/apps/Dockerfile \
#     --build-arg JAR_FILE=pd-base/target/pd-base-1.0.0-SNAPSHOT.jar \
#     -t pinda/pd-base:<tag> .
# ============================================
# 基础镜像: Temurin 8 JRE(Ubuntu 22.04, 源持续维护; openjdk:8-jre-slim的Debian11已EOL, security源404)
FROM eclipse-temurin:8-jre-jammy

ARG JAR_FILE

# 字体库: 验证码(Java2D/SunFontManager)与图表生成必需, 缺失会导致NoClassDefFoundError
RUN apt-get update \
    && apt-get install -y --no-install-recommends fontconfig \
    && rm -rf /var/lib/apt/lists/*

# 时区设置（容器默认 UTC，统一为东八区）
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/${TZ} /etc/localtime && echo ${TZ} > /etc/timezone

# JVM 参数由运行时环境变量 JAVA_OPTS 注入（便于在 compose 中限制内存）
COPY ${JAR_FILE} /app.jar

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar"]
