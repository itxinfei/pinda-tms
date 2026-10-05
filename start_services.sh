#!/bin/bash
# 品达TMS 微服务启动脚本（宿主机运行）
# 用法: ./start_services.sh <all|auth|base|oms|work|user|dispatch|aggregation|netty|druid|gateway|web|status|stop>
BASE=/opt/pinda-tms
LOG=/data/projects/logs
JAVA=/usr/bin/java
XMX=512m
XMS=256m

# name:jar:profile
JARS=(
  "pd-auth-server:/opt/pinda-tms/pd-authority/pd-apps/pd-auth/pd-auth-server/target/pd-auth-server.jar:test"
  "pd-base:/opt/pinda-tms/pd-base/target/pd-base-1.0.0-SNAPSHOT.jar:prod"
  "pd-oms:/opt/pinda-tms/pd-oms/target/pd-oms-1.0.0-SNAPSHOT.jar:prod"
  "pd-work:/opt/pinda-tms/pd-work/target/pd-work-1.0.0-SNAPSHOT.jar:dev"
  "pd-user:/opt/pinda-tms/pd-user/target/pd-user-1.0.0-SNAPSHOT.jar:prod"
  "pd-dispatch:/opt/pinda-tms/pd-dispatch/target/pd-dispatch-1.0.0-SNAPSHOT.jar:prod"
  "pd-aggregation:/opt/pinda-tms/pd-aggregation/target/pd-aggregation-1.0.0-SNAPSHOT.jar:prod"
  "pd-netty:/opt/pinda-tms/pd-netty/target/pd-netty-1.0.0-SNAPSHOT.jar:prod"
  "pd-druid:/opt/pinda-tms/pd-druid/target/pd-druid-1.0.0-SNAPSHOT.jar:prod"
  "pd-gateway:/opt/pinda-tms/pd-authority/pd-apps/pd-gateway/target/pd-gateway.jar:test"
  "pd-web-manager:/opt/pinda-tms/pd-web/pd-web-manager/target/pd-web-manager-1.0.0-SNAPSHOT.jar:dev"
  "pd-web-courier:/opt/pinda-tms/pd-web/pd-web-courier/target/pd-web-courier-1.0.0-SNAPSHOT.jar:prod"
  "pd-web-driver:/opt/pinda-tms/pd-web/pd-web-driver/target/pd-web-driver-1.0.0-SNAPSHOT.jar:prod"
  "pd-web-customer:/opt/pinda-tms/pd-web/pd-web-customer/target/pd-web-customer-1.0.0-SNAPSHOT.jar:prod"
)

start_one() {
  local name=$1 jar=$2 profile=$3
  mkdir -p $LOG/$name
  if pgrep -f "$jar" > /dev/null 2>&1; then
    echo "[SKIP] $name already running"
    return
  fi
  NACOS_IP=192.168.20.130 NACOS_PORT=8848 NACOS_ID=1cb93ce4-dc0e-4730-b759-d35fd7ed93c3 nohup $JAVA -Xms$XMS -Xmx$XMX -jar $jar --spring.profiles.active=$profile > $LOG/$name/startup.log 2>&1 &
  echo "[START] $name pid=$! profile=$profile"
}

start_all() {
  for entry in "${JARS[@]}"; do
    IFS=: read -r name jar profile <<< "$entry"
    start_one "$name" "$jar" "$profile"
    sleep 3
  done
}

status() {
  echo "--- 运行中的服务 ---"
  for entry in "${JARS[@]}"; do
    IFS=: read -r name jar profile <<< "$entry"
    if pgrep -f "$jar" > /dev/null 2>&1; then
      echo "[UP] $name ($(pgrep -f "$jar" | head -1))"
    else
      echo "[DOWN] $name"
    fi
  done
}

stop_all() {
  for entry in "${JARS[@]}"; do
    IFS=: read -r name jar profile <<< "$entry"
    pkill -f "$jar" 2>/dev/null && echo "[STOP] $name"
  done
}

case "$1" in
  all) start_all ;;
  auth) start_one pd-auth-server /opt/pinda-tms/pd-authority/pd-apps/pd-auth/pd-auth-server/target/pd-auth-server.jar test ;;
  base) start_one pd-base /opt/pinda-tms/pd-base/target/pd-base-1.0.0-SNAPSHOT.jar prod ;;
  oms) start_one pd-oms /opt/pinda-tms/pd-oms/target/pd-oms-1.0.0-SNAPSHOT.jar prod ;;
  work) start_one pd-work /opt/pinda-tms/pd-work/target/pd-work-1.0.0-SNAPSHOT.jar dev ;;
  user) start_one pd-user /opt/pinda-tms/pd-user/target/pd-user-1.0.0-SNAPSHOT.jar prod ;;
  dispatch) start_one pd-dispatch /opt/pinda-tms/pd-dispatch/target/pd-dispatch-1.0.0-SNAPSHOT.jar prod ;;
  aggregation) start_one pd-aggregation /opt/pinda-tms/pd-aggregation/target/pd-aggregation-1.0.0-SNAPSHOT.jar prod ;;
  netty) start_one pd-netty /opt/pinda-tms/pd-netty/target/pd-netty-1.0.0-SNAPSHOT.jar prod ;;
  druid) start_one pd-druid /opt/pinda-tms/pd-druid/target/pd-druid-1.0.0-SNAPSHOT.jar prod ;;
  gateway) start_one pd-gateway /opt/pinda-tms/pd-authority/pd-apps/pd-gateway/target/pd-gateway.jar test ;;
  web) start_one pd-web-manager /opt/pinda-tms/pd-web/pd-web-manager/target/pd-web-manager-1.0.0-SNAPSHOT.jar dev
        start_one pd-web-courier /opt/pinda-tms/pd-web/pd-web-courier/target/pd-web-courier-1.0.0-SNAPSHOT.jar prod
        start_one pd-web-driver /opt/pinda-tms/pd-web/pd-web-driver/target/pd-web-driver-1.0.0-SNAPSHOT.jar prod
        start_one pd-web-customer /opt/pinda-tms/pd-web/pd-web-customer/target/pd-web-customer-1.0.0-SNAPSHOT.jar prod ;;
  status) status ;;
  stop) stop_all ;;
  *) echo "用法: $0 <all|auth|base|oms|work|user|dispatch|aggregation|netty|druid|gateway|web|status|stop>" ;;
esac
