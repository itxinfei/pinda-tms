#!/usr/bin/env bash
# =============================================================================
# 心飞物流TMS 容器健康巡检（P0-8b 最小集）
# 检测：unhealthy / exited / restarting 容器（排除 Gitea Actions 临时构建容器）+ 磁盘水位
# cron：*/5 * * * *  → 告警落 /var/log/xinfei-health-alert.log
# 扩展：短信/IM 通知渠道开通后，在 ALERT 段接 dispatch /sms/send 或企业微信
# =============================================================================
set -uo pipefail
TAG="[xinfei-health $(date '+%F %T')]"
ALERT_LOG=/var/log/xinfei-health-alert.log
DISK_THRESHOLD=85

# 异常容器（排除 Gitea Actions 临时构建容器：名称含 GITEA-ACTIONS）
BAD=$(docker ps -a --format '{{.Names}}\t{{.Status}}' 2>/dev/null \
  | grep -vE 'GITEA-ACTIONS' \
  | awk -F'\t' '$2 ~ /unhealthy|Exited|restarting/ {print $1"  "$2}')

# 磁盘水位（/ 分区）
DISK=$(df -P / 2>/dev/null | awk 'NR==2 {gsub("%",""); print $5}')

ALERT=0
if [ -n "$BAD" ]; then
  {
    echo "$TAG ALERT 异常容器:"
    echo "$BAD"
  } >> "$ALERT_LOG"
  ALERT=1
fi

if [ "${DISK:-0}" -ge "$DISK_THRESHOLD" ]; then
  echo "$TAG ALERT 磁盘使用 ${DISK}% ≥ ${DISK_THRESHOLD}%（建议 docker system df 定位，镜像按 keep2 清理）" >> "$ALERT_LOG"
  ALERT=1
fi

if [ "$ALERT" = "0" ]; then
  # 健康时轻量心跳，便于确认巡检自身在跑
  echo "$TAG OK（容器全部正常，磁盘 ${DISK:-?}%）" >> /var/log/xinfei-health-check.log
fi

# TODO(P1 通知): 短信通道开通后在此调 dispatch /sms/send 或企业微信 webhook
[ "$ALERT" = "1" ] && exit 1
exit 0
