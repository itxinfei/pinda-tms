#!/usr/bin/env bash
# =============================================================================
# 心飞物流TMS 定时备份（P0-8c）：MySQL 全库 + Redis RDB + Nacos 数据
# 用法：crontab 每日 2:00  →  /usr/local/bin/xinfei-backup.sh >> /var/log/xinfei-backup.log 2>&1
# 保留：/data/backup/auto/ 下按天目录，保留 7 天自动清理
# 恢复：见脚本尾注释
# =============================================================================
set -uo pipefail

TS=$(date +%Y%m%d_%H%M%S)
KEEP_DAYS=7
BK_ROOT=/data/backup/auto
BK="$BK_ROOT/$TS"
TAG="[xinfei-backup $TS]"

mkdir -p "$BK"

# 1) MySQL 全库（业务库 xinfei_tms + 调度 xxl_job；一致性快照含例程/触发器）
docker exec mysql8 mysqldump -uroot -p123456 --single-transaction --routines --triggers \
  --databases xinfei_tms xxl_job 2>/dev/null | gzip > "$BK/mysql_xinfei_tms_xxl_job.sql.gz"

# 2) Redis RDB（SAVE 同步落盘后拷宿主挂载文件）
docker exec redis redis-cli -a 123456 --no-auth-warning SAVE >/dev/null 2>&1
if [ -f /data/redis/dump.rdb ]; then
  cp /data/redis/dump.rdb "$BK/redis_dump.rdb"
else
  echo "$TAG WARN: /data/redis/dump.rdb 缺失，Redis 备份跳过"
fi

# 3) Nacos 数据目录（内嵌 derby + 配置快照）
tar -czf "$BK/nacos_data.tar.gz" -C /data nacos 2>/dev/null \
  || echo "$TAG WARN: nacos 数据拷贝失败"

# 4) 产出校验（三项任一为 0 字节即告警）
FAIL=0
for f in mysql_xinfei_tms_xxl_job.sql.gz nacos_data.tar.gz; do
  if [ ! -s "$BK/$f" ]; then echo "$TAG ERROR: $f 为空或缺失"; FAIL=1; fi
done
[ -f "$BK/redis_dump.rdb" ] || { echo "$TAG WARN: redis_dump.rdb 缺失"; }

# 5) 保留策略：清理 KEEP_DAYS 天前的按天目录
find "$BK_ROOT" -maxdepth 1 -mindepth 1 -type d -mtime +$KEEP_DAYS -exec rm -rf {} \; 2>/dev/null

if [ "$FAIL" = "1" ]; then
  echo "$TAG FAILED（存在空产物，请人工检查）"
  exit 1
fi
echo "$TAG OK: $BK ($(du -sh "$BK" | cut -f1))，保留 ${KEEP_DAYS} 天"

# ---------------- 恢复手册 ----------------
# MySQL：gunzip < mysql_xinfei_tms_xxl_job.sql.gz | docker exec -i mysql8 mysql -uroot -p123456
#        （整库含 CREATE DATABASE，可直接导入；或先导入临时库验证）
# Redis：停 redis → 用 redis_dump.rdb 覆盖 /data/redis/dump.rdb → 起 redis
# Nacos：停 nacos → 解包覆盖 /data/nacos → 起 nacos（或经控制台导入配置）
