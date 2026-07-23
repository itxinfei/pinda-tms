#!/bin/bash
# ============================================
# Nacos 配置推送脚本
# 作用：将微服务所需的配置推送到 Nacos 配置中心
# 使用：./scripts/push-nacos-config.sh
# ============================================

NACOS_URL="http://localhost:8848/nacos/v1"
NACOS_USERNAME="nacos"
NACOS_PASSWORD="nacos"
NAMESPACE="09d0e14f-107f-4fea-8f80-e59e0cc63694"
GROUP="pinda-tms"

echo "=========================================="
echo "  推送配置到 Nacos (namespace: $NAMESPACE)"
echo "=========================================="

# 辅助函数：推送单个配置文件到 Nacos
push_config() {
    local data_id="$1"
    local content="$2"
    local file_type="yml"

    echo "推送配置: $data_id"

    curl -s -X POST "$NACOS_URL/cs/configs" \
        --data-urlencode "dataId=$data_id" \
        --data-urlencode "group=$GROUP" \
        --data-urlencode "content=$content" \
        --data-urlencode "type=$file_type" \
        --data-urlencode "namespace=$NAMESPACE" \
        -u "$NACOS_USERNAME:$NACOS_PASSWORD" | jq -r '.code // empty'

    if [ $? -eq 0 ]; then
        echo "  -> $data_id 推送成功"
    else
        echo "  -> $data_id 推送失败"
    fi
}

# ============================================
# 公共配置 (common.yml)
# ============================================
push_config "common.yml" "$(cat <<'YML'
# 品达TMS - 公共配置
# 此文件为 Nacos 共享配置，所有服务均可读取

spring:
  jackson:
    time-zone: GMT+8
    date-format: yyyy-MM-dd HH:mm:ss
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
      enabled: true

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*Mapper.xml
  typeAliasesPackage: com.itheima.pinda.entity
  global-config:
    db-config:
      id-type: auto
      table-prefix: t_
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    call-setters-on-nulls: true

# Redis 连接配置
pinda:
  redis:
    ip: redis
    port: 6379
    database: 0
    password:

# J2Cache 缓存配置
j2cache:
  open-spring-cache: true
  cache-clean-mode: passive
  redis-client: jedis
  l2-cache-open: true
  broadcast: net
  cache-region-name: pinda-tms
  default-name: default
  jedis:
    host: ${pinda.redis.ip}:${pinda.redis.port}
    database: ${pinda.redis.database}
    password: ${pinda.redis.password}
  caffeine:
    properties:
      caffeine.expire-after: 2h

# Seata 配置
seata:
  enabled: true
  application-id: pinda-tms
  tx-service-group: pinda-tms-group
  registry:
    type: nacos
    nacos:
      application: seata-server
      server-addr: nacos:8848
      group: SEATA_GROUP
      namespace: ${NACOS_ID}
  config:
    type: nacos
    nacos:
      server-addr: nacos:8848
      group: SEATA_GROUP
      namespace: ${NACOS_ID}
  service:
    vgroup-mapping:
      pinda-tms-group: default
    enable-degrade: false
    disable-global-transaction: false

# 日志配置
logging:
  level:
    com.itheima.pinda: debug
    org.springframework.web: info
  file:
    path: /data/projects/logs
    name: \${logging.file.path}/\${spring.application.name}/root.log
YML
)"

# ============================================
# MySQL 配置 (mysql.yml)
# ============================================
push_config "mysql.yml" "$(cat <<'YML'
# 品达TMS - MySQL 数据源配置
spring:
  datasource:
    druid:
      type: com.alibaba.druid.pool.DruidDataSource
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://mysql:3306/pinda_tms?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      username: root
      password: root123
      max-active: 50
      min-idle: 10
      max-wait: 60000
      initial-size: 5
      validation-query: SELECT 1
      test-on-borrow: true
      test-while-idle: true
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      keep-alive: true
      remove-abandoned: true
      remove-abandoned-timeout: 180
      log-abandoned: true
      filters: stat,wall,log4j2
      connection-properties: druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000
      web-stat-filter:
        enabled: true
        url-pattern: /*
        exclusions: "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*"
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        login-username: admin
        login-password: admin
        reset-enable: true
YML
)"

# ============================================
# Redis 配置 (redis.yml)
# ============================================
push_config "redis.yml" "$(cat <<'YML'
# 品达TMS - Redis 配置
spring:
  redis:
    host: \${pinda.redis.ip}
    password: \${pinda.redis.password}
    port: \${pinda.redis.port}
    database: \${pinda.redis.database}
    lettuce:
      pool:
        max-active: 50
        max-idle: 20
        min-idle: 5
        max-wait: 5000
    timeout: 3000
YML
)"

# ============================================
# pd-base 服务配置
# ============================================
push_config "pd-base.yml" "$(cat <<'YML'
spring:
  application:
    name: pd-base
server:
  port: 8185
YML
)"

# ============================================
# pd-oms 服务配置
# ============================================
push_config "pd-oms.yml" "$(cat <<'YML'
spring:
  application:
    name: pd-oms
  kafka:
    bootstrap-servers: kafka:9092
    listener:
      concurrency: 3
    producer:
      retries: 3
      batch-size: 16384
      buffer-memory: 33554432
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      group-id: pd-oms-group
      auto-offset-reset: latest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
server:
  port: 8186
YML
)"

# ============================================
# pd-work 服务配置
# ============================================
push_config "pd-work.yml" "$(cat <<'YML'
spring:
  application:
    name: pd-work
server:
  port: 8187
YML
)"

# ============================================
# pd-user 服务配置
# ============================================
push_config "pd-user.yml" "$(cat <<'YML'
spring:
  application:
    name: pd-user
server:
  port: 8189
YML
)"

# ============================================
# pd-dispatch 服务配置
# ============================================
push_config "pd-dispatch.yml" "$(cat <<'YML'
spring:
  application:
    name: pd-dispatch
  datasource:
    druid:
      url: jdbc:mysql://mysql:3306/pinda_tms?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
server:
  port: 8190
YML
)"

# ============================================
# pd-aggregation 服务配置
# ============================================
push_config "pd-aggregation.yml" "$(cat <<'YML'
spring:
  application:
    name: pd-aggregation
server:
  port: 8191
YML
)"

# ============================================
# pd-netty 服务配置
# ============================================
push_config "pd-netty.yml" "$(cat <<'YML'
spring:
  application:
    name: pd-netty
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      retries: 3
      batch-size: 16384
      buffer-memory: 33554432
    consumer:
      group-id: pd-netty-group
      auto-offset-reset: latest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
server:
  port: 8192
YML
)"

# ============================================
# pd-druid 服务配置
# ============================================
push_config "pd-druid.yml" "$(cat <<'YML'
spring:
  application:
    name: pd-druid
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://mysql:3306/pinda_tms?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: root123
server:
  port: 8193
YML
)"

# ============================================
# pd-auth-server 服务配置
# ============================================
push_config "pd-auth.yml" "$(cat <<'YML'
spring:
  application:
    name: pd-auth
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://mysql:3306/pinda_tms?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: root123
server:
  port: 9000
YML
)"

# ============================================
# pd-gateway 服务配置
# ============================================
push_config "pd-gateway.yml" "$(cat <<'YML'
spring:
  application:
    name: pd-gateway
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://mysql:3306/pinda_tms?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: root123
server:
  port: 8760
zuul:
  routes:
    pd-oms:
      path: /api/oms/**
      serviceId: pd-oms
    pd-work:
      path: /api/work/**
      serviceId: pd-work
    pd-dispatch:
      path: /api/dispatch/**
      serviceId: pd-dispatch
    pd-base:
      path: /api/base/**
      serviceId: pd-base
    pd-user:
      path: /api/user/**
      serviceId: pd-user
    pd-aggregation:
      path: /api/aggregation/**
      serviceId: pd-aggregation
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
)"

# ============================================
# pd-web-manager 服务配置
# ============================================
push_config "pd-web-manager.yml" "$(cat <<'YML'
spring:
  application:
    name: pd-web-manager
server:
  port: 8161
YML
)"

# ============================================
# pd-web-driver 服务配置
# ============================================
push_config "pd-web-driver.yml" "$(cat <<'YML'
spring:
  application:
    name: pd-web-driver
server:
  port: 8162
YML
)"

# ============================================
# pd-web-courier 服务配置
# ============================================
push_config "pd-web-courier.yml" "$(cat <<'YML'
spring:
  application:
    name: pd-web-courier
server:
  port: 8163
YML
)"

# ============================================
# pd-web-customer 服务配置
# ============================================
push_config "pd-web-customer.yml" "$(cat <<'YML'
spring:
  application:
    name: pd-web-customer
server:
  port: 8164
YML
)"

echo ""
echo "=========================================="
echo "  所有配置推送完成!"
echo "=========================================="
