#!/bin/bash
# ============================================
# 品达TMS 一键导入业务数据
# 前提：已在 MySQL 建好各业务库 pd_auth / pd_base / pd_oms / pd_work / pd_users / pd_dispatch / pd_aggregation
# 用法：在【仓库根目录】执行
#   bash deploy/server/import-data.sh
# 可选变量：
#   MYSQL_CONTAINER=mysql57 DB_PASS=123456 bash deploy/server/import-data.sh
# ============================================
set -e

MYSQL_CONTAINER="${MYSQL_CONTAINER:-mysql57}"
DB_PASS="${DB_PASS:-123456}"

dc() { docker exec -i "$MYSQL_CONTAINER" mysql -uroot -p"$DB_PASS" "$@"; }
# 严格导入（失败即中断）
imp() { echo ">> 导入 $2 -> 库 $1"; dc "$1" < "$2"; }
# 增量导入（表/记录已存在不致命，仅告警跳过）
imp_opt() { echo ">> 增量 $2 -> 库 $1"; dc "$1" < "$2" || echo "   [跳过] $2（可能已存在）"; }

echo "[1/3] 各服务业务库（建表 + 初始数据）"
imp pd_base        "mysql/TMS项目建库脚本/pd_base.sql"
imp pd_oms         "mysql/TMS项目建库脚本/pd_oms.sql"
imp pd_work        "mysql/TMS项目建库脚本/pd_work.sql"
imp pd_users       "mysql/TMS项目建库脚本/pd_users.sql"
imp pd_dispatch    "mysql/TMS项目建库脚本/pd_dispatch.sql"
imp pd_aggregation "mysql/TMS项目建库脚本/pd_aggregation.sql"

echo "[2/3] 权限库 + 货物信息"
imp pd_auth pd-authority/pd_auth.sql
imp pd_base pd-authority/pd_goods_info.sql

echo "[3/3] 业务增量脚本"
imp_opt pd_oms      docs/sql/V1.0__add_transport_order_unique_index.sql
imp_opt pd_oms      docs/sql/V1.1__create_truck_location_table.sql
imp_opt pd_auth     docs/sql/V1.2__add_trace_menu.sql
imp_opt pd_auth     docs/sql/V1.3__grant_trace_menu.sql
imp_opt pd_dispatch docs/sql/V1.4__create_schedule_exception_order.sql
imp_opt pd_oms     docs/sql/V1.5__create_truck_location_archive.sql
imp_opt pd_oms     docs/sql/V1.6__create_payment_order.sql

echo ""
echo "校验：各库表数量"
for db in pd_auth pd_base pd_oms pd_work pd_users pd_dispatch pd_aggregation; do
  n=$(dc -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${db}';")
  echo "  ${db}: ${n} 张表"
done
