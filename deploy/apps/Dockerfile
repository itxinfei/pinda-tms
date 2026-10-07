# ============================================
# 品达TMS 微服务通用镜像（一份复用，全部 14 个 Spring Boot 服务）
# 构建上下文为【仓库根目录】，通过 JAR_FILE 传入各模块产出的 fat jar：
#   docker build -f deploy/apps/Dockerfile \
#     --build-arg JAR_FILE=deploy/jars/pd-base.jar -t pinda/pd-base:<tag> .
#
# 字体/时区等重活放在 pinda/jre8-fontconfig:1（见 Dockerfile.base，只建一次）。
# 注意：ARG 必须放在最后一条会用到它的指令前——放在 RUN 之前会让 ARG 值一变
# 就把后面所有层缓存打掉，14 个服务各自重跑 apt（实测多花约 15 分钟）。
# ============================================
FROM pinda/jre8-fontconfig:1

# JVM 参数由运行时环境变量 JAVA_OPTS 注入（便于在 compose 中限制内存）
ARG JAR_FILE
COPY ${JAR_FILE} /app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar"]
