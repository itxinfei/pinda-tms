#!/bin/bash
# ============================================
# 品达TMS - Nacos 配置一键推送脚本（多库版）
# 适配：Ubuntu 24.04 + MySQL5.7(192.168.20.130) + Nacos1.4.1
# 用法：先启动 Nacos，再执行  bash scripts/push-nacos-config.sh
# ============================================
set -e

# ---------- 环境参数（按需修改） ----------
NACOS_HOST="${NACOS_HOST:-192.168.20.130}"
NACOS_PORT="${NACOS_PORT:-8848}"
NACOS_URL="http://${NACOS_HOST}:${NACOS_PORT}/nacos/v1"
NACOS_USERNAME="${NACOS_USERNAME:-nacos}"
NACOS_PASSWORD="${NACOS_PASSWORD:-nacos}"

# 与各微服务 bootstrap-<profile>.yml 中的 namespace / group 完全一致
NAMESPACE="${NAMESPACE:-1cb93ce4-dc0e-4730-b759-d35fd7ed93c3}"
GROUP="${GROUP:-pinda-tms}"

DB_HOST=192.168.20.130
DB_PORT=3306
DB_USER=root
DB_PASS=123456

echo "=========================================="
echo " 推送配置到 Nacos"
echo " 地址: ${NACOS_HOST}:${NACOS_PORT}  命名空间: ${NAMESPACE}  分组: ${GROUP}"
echo "=========================================="

# ---------- 登录获取 accessToken（鉴权关闭时为空也不影响） ----------
TOKEN=""
resp=$(curl -s --connect-timeout 5 --data "username=${NACOS_USERNAME}&password=${NACOS_PASSWORD}" "${NACOS_URL}/auth/login" || true)
TOKEN=$(echo "$resp" | sed -n 's/.*"accessToken":"*\([^",}]*\)"*.*/\1/p')
[ -n "$TOKEN" ] && echo "已获取 accessToken" || echo "未取到 token（若 Nacos 未开启鉴权属正常）"

# ---------- 推送函数：dataId + 内容文件路径 ----------
push_file() {
  local data_id="$1"
  local file="$2"
  local out
  out=$(curl -s -X POST "${NACOS_URL}/cs/configs" \
    --data-urlencode "dataId=${data_id}" \
    --data-urlencode "group=${GROUP}" \
    --data-urlencode "tenant=${NAMESPACE}" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content=$(cat "$file")" \
    ${TOKEN:+--data-urlencode "accessToken=${TOKEN}"} || true)
  # Nacos 成功返回 true
  if echo "$out" | grep -q "true"; then
    echo "  [OK]   ${data_id}"
  else
    echo "  [FAIL] ${data_id}  返回: ${out}"
  fi
}

TMPD="$(mktemp -d)"
trap 'rm -rf "$TMPD"' EXIT

# ---------- 公共片段：jackson + multipart（web 聚合层使用） ----------
gen_common_head() {
cat <<'YML'
spring:
  jackson:
    time-zone: GMT+8
    date-format: yyyy-MM-dd HH:mm:ss
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
      enabled: true
YML
}

# ---------- 数据源 + MyBatis 片段 ----------
# 参数1: 库名   参数2: mapper-locations(默认 /mapper/**)
gen_db_block() {
local DB="$1"
local ML="${2:-classpath*:/mapper/**/*Mapper.xml}"
cat <<EOF
  datasource:
    druid:
      type: com.alibaba.druid.pool.DruidDataSource
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      username: ${DB_USER}
      password: "${DB_PASS}"
      initial-size: 5
      max-active: 50
      min-idle: 10
      max-wait: 60000
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 20
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
      filter:
        stat:
          log-slow-sql: true
          slow-sql-millis: 1000
          merge-sql: true
        wall:
          config:
            multi-statement-allow: true
mybatis-plus:
  mapper-locations: ${ML}
  typeAliasesPackage: com.itheima.pinda
  global-config:
    db-config:
      id-type: auto
      field-strategy: not_null
      column-underline: true
      logic-delete-value: 1
      logic-not-delete-value: 0
    banner: false
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    call-setters-on-nulls: true
    jdbc-type-for-null: 'null'
EOF
}

# ---------- Seata 客户端片段 ----------
# 凡依赖 pd-common 的服务（7个后端 + 4个web）都会传递引入 seata-spring-boot-starter，需统一配置
gen_seata_block() {
cat <<'YML'
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: pinda-tms-group
  enable-auto-data-source-proxy: true
  registry:
    type: nacos
    nacos:
      server-addr: 192.168.20.130:8848
      namespace: 1cb93ce4-dc0e-4730-b759-d35fd7ed93c3
      group: SEATA_GROUP
      cluster: default
  config:
    type: nacos
    nacos:
      server-addr: 192.168.20.130:8848
      namespace: 1cb93ce4-dc0e-4730-b759-d35fd7ed93c3
      group: SEATA_GROUP
YML
}

# ============================================================
# 1) 共享配置（pd-auth-server / pd-gateway 的 shared-dataids 引用）
# ============================================================
echo "[1/3] 推送共享配置 common.yml / redis.yml / mysql.yml"

gen_common_head > "$TMPD/common.yml"
cat >> "$TMPD/common.yml" <<'YML'
  redis:
    host: 192.168.20.130
    port: 6379
YML

cat > "$TMPD/redis.yml" <<'YML'
spring:
  redis:
    host: 192.168.20.130
    port: 6379
YML

gen_common_head > "$TMPD/mysql.yml"
gen_db_block pd_auth >> "$TMPD/mysql.yml"

push_file common.yml "$TMPD/common.yml"
push_file redis.yml  "$TMPD/redis.yml"
push_file mysql.yml  "$TMPD/mysql.yml"

# ============================================================
# 2) 基础微服务（数据源指向各自独立库）
#    dataId = <应用名>-<profile>.yml，profile 以各 bootstrap 中 active 为准
# ============================================================
echo "[2/3] 推送基础微服务配置"

# ---- pd-auth-server（profile=test，mapper 在 mapper_authority） ----
{ gen_common_head;
  echo "server:"; echo "  port: 9000";
  gen_db_block pd_auth "classpath*:/mapper_authority/**/*Mapper.xml";
} > "$TMPD/pd-auth-server-test.yml"
push_file pd-auth-server-test.yml "$TMPD/pd-auth-server-test.yml"

# ---- pd-base（prod） ----
{ gen_common_head; gen_db_block pd_base; gen_seata_block; } > "$TMPD/pd-base-prod.yml"
push_file pd-base-prod.yml "$TMPD/pd-base-prod.yml"

# ---- pd-oms（prod） ----
{ gen_common_head; gen_db_block pd_oms; gen_seata_block; } > "$TMPD/pd-oms-prod.yml"
push_file pd-oms-prod.yml "$TMPD/pd-oms-prod.yml"

# ---- pd-work（dev） ----
{ gen_common_head; gen_db_block pd_work; gen_seata_block; } > "$TMPD/pd-work-dev.yml"
push_file pd-work-dev.yml "$TMPD/pd-work-dev.yml"

# ---- pd-user（prod） ----
{ gen_common_head; gen_db_block pd_users; gen_seata_block; } > "$TMPD/pd-user-prod.yml"
push_file pd-user-prod.yml "$TMPD/pd-user-prod.yml"

# ---- pd-aggregation（prod） ----
{ gen_common_head; gen_db_block pd_aggregation; gen_seata_block; } > "$TMPD/pd-aggregation-prod.yml"
push_file pd-aggregation-prod.yml "$TMPD/pd-aggregation-prod.yml"

# ---- pd-dispatch（prod） + RabbitMQ ----
{ gen_common_head;
  cat <<'YML'
  rabbitmq:
    host: 192.168.20.130
    port: 5672
    username: pinda
    password: pinda123
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 3
YML
  gen_db_block pd_dispatch;
  gen_seata_block;
} > "$TMPD/pd-dispatch-prod.yml"
push_file pd-dispatch-prod.yml "$TMPD/pd-dispatch-prod.yml"

# ---- pd-netty（prod）：Kafka + 数据源(pd_oms 存轨迹) ----
{ gen_common_head;
  cat <<'YML'
  kafka:
    bootstrap-servers: 192.168.20.130:9092
    listener:
      concurrency: 3
    producer:
      retries: 3
      batch-size: 16384
      buffer-memory: 33554432
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      group-id: pd-netty-group
      auto-offset-reset: latest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
YML
  echo "server:"; echo "  port: 8192";
  gen_db_block pd_oms;
  gen_seata_block;
} > "$TMPD/pd-netty-prod.yml"
push_file pd-netty-prod.yml "$TMPD/pd-netty-prod.yml"

# ---- pd-druid（prod）：车辆位置实时查询，数据源指向 pd_oms ----
# 注意其 bootstrap-prod 引用的是标准 ${spring.datasource.url} / ${spring.datasource.driver-class-name}
gen_common_head > "$TMPD/pd-druid-prod.yml"
cat >> "$TMPD/pd-druid-prod.yml" <<EOF
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/pd_oms?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: ${DB_USER}
    password: "${DB_PASS}"
EOF
push_file pd-druid-prod.yml "$TMPD/pd-druid-prod.yml"

# ============================================================
# 3) Web 聚合层（无数据源，仅 jackson/multipart 占位符）
# ============================================================
echo "[3/3] 推送 Web 聚合层配置"

# pd-web-manager 为 dev，其余为 prod
{ gen_common_head; gen_seata_block; } > "$TMPD/pd-web-manager-dev.yml"
push_file pd-web-manager-dev.yml "$TMPD/pd-web-manager-dev.yml"

for pair in "pd-web-driver:prod" "pd-web-courier:prod" "pd-web-customer:prod"; do
  name="${pair%%:*}"; prof="${pair##*:}"
  { gen_common_head; gen_seata_block; } > "$TMPD/${name}-${prof}.yml"
  push_file "${name}-${prof}.yml" "$TMPD/${name}-${prof}.yml"
done

# ============================================================
# 网关（zuul 路由）
# ============================================================
cat > "$TMPD/pd-gateway-test.yml" <<'YML'
server:
  port: 8760
zuul:
  routes:
    pd-oms:         { path: /api/oms/**,         serviceId: pd-oms }
    pd-work:        { path: /api/work/**,        serviceId: pd-work }
    pd-dispatch:    { path: /api/dispatch/**,    serviceId: pd-dispatch }
    pd-base:        { path: /api/base/**,        serviceId: pd-base }
    pd-user:        { path: /api/user/**,        serviceId: pd-user }
    pd-aggregation: { path: /api/aggregation/**, serviceId: pd-aggregation }
    pd-auth:        { path: /api/auth/**,        serviceId: pd-auth-server }
  ignored-services: '*'
  sensitive-headers:
ribbon:
  ReadTimeout: 60000
  ConnectTimeout: 60000
hystrix:
  command:
    default:
      execution:
        isolation:
          thread:
            timeoutInMilliseconds: 60000
YML
push_file pd-gateway-test.yml "$TMPD/pd-gateway-test.yml"

echo ""
echo "=========================================="
echo " 全部配置推送完成，共推送到 Nacos。"
echo " 控制台：http://${NACOS_HOST}:${NACOS_PORT}/nacos  (${NACOS_USERNAME}/${NACOS_PASSWORD})"
echo " 启动微服务前建议执行："
echo "   export NACOS_USERNAME=nacos NACOS_PASSWORD=nacos"
echo "=========================================="
