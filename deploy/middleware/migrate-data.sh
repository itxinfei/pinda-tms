#!/bin/bash
# 把裸 docker run 创建的中间件容器迁到 docker-compose.infra.yml 之前，
# 将容器可写层里的数据复制到宿主 /data 下，并删除旧容器让 compose 接管。
#
# 逐个服务执行，每步之间可独立验证：
#   sudo bash migrate-data.sh status      # 只看现状，不动任何东西
#   sudo bash migrate-data.sh rabbitmq
#   sudo docker compose -f ../middleware/docker-compose.infra.yml up -d rabbitmq
#
# rabbitmq 原本完全没有挂载卷，nacos 只挂了 logs，
# 因此这两个必须复制；redis/mysql57 的数据已经在 /data 上，只需删旧容器让 compose 重建。
set -euo pipefail

COMPOSE=$(dirname "$(readlink -f "$0")")/docker-compose.infra.yml

# 每个服务一组 "容器内路径|宿主路径|属主uid"
pairs_of() {
  case "$1" in
    rabbitmq) printf '%s\n' "/var/lib/rabbitmq|/data/rabbitmq|999";;
    nacos)    printf '%s\n' "/home/nacos/data|/data/nacos-data|0";;
    redis|mysql57) printf '\n';;
    *) return 1;;
  esac
}

status() {
  echo "=== 容器编排来源（project 为空即仍是裸 docker run）==="
  for c in rabbitmq redis mysql57 nacos; do
    docker inspect -f "{{.Name}} image={{.Config.Image}} restart={{.HostConfig.RestartPolicy.Name}}" "$c" 2>/dev/null \
      || { echo "$c  不存在"; continue; }
    # 带点的 label key 必须用 index 取，{{.Config.Labels.com.docker.compose.project}} 解析不出会恒返回 <no value>
    docker inspect -f "{{if index .Config.Labels \"com.docker.compose.project\"}}    project={{index .Config.Labels \"com.docker.compose.project\"}} (compose 管理){{else}}    project=无 (仍是裸 docker run){{end}}" "$c"
  done
  echo
  echo "=== 宿主 /data 现状 ==="
  for d in /data/mysql /data/redis /data/nacos /data/nacos-data /data/rabbitmq; do
    [ -e "$d" ] && printf '%s  %s\n' "$(du -sh "$d" 2>/dev/null | cut -f1)" "$d" || echo "不存在  $d"
  done
}

migrate() {
  [ "$(id -u)" = 0 ] || { echo "需要 root：sudo bash $0 $1"; exit 1; }
  svc=$1
  docker inspect "$svc" >/dev/null 2>&1 || { echo "容器 $svc 不存在"; exit 1; }

  pairs_of "$svc" >/tmp/.pairs.$$ || { echo "未知服务 $svc"; exit 1; }

  # 目标目录非空一律拒绝，避免把已有数据盖进新卷
  while IFS='|' read -r src dst uid; do
    [ -n "${dst:-}" ] || continue
    if [ -d "$dst" ] && [ -n "$(ls -A "$dst" 2>/dev/null)" ]; then
      echo "中止：$dst 已有内容，不覆盖。确认无误后再手工处理。"; exit 1
    fi
  done < /tmp/.pairs.$$

  echo ">>> 优雅停止 $svc"
  docker stop -t 120 "$svc" >/dev/null

  while IFS='|' read -r src dst uid; do
    [ -n "${src:-}" ] || continue
    echo ">>> 复制 $svc:$src -> $dst"
    mkdir -p "$dst"
    docker cp "$svc:$src/." "$dst/"
    chown -R "$uid:$uid" "$dst"
    n=$(find "$dst" -mindepth 1 | wc -l)
    [ "$n" -gt 0 ] || { echo "中止：$dst 复制后为空，保留容器不删。"; exit 1; }
    echo "    $(du -sh "$dst" | cut -f1), $n 个条目, 属主 $uid"
  done < /tmp/.pairs.$$
  rm -f /tmp/.pairs.$$

  echo ">>> 删除旧容器 $svc（数据已落到宿主盘）"
  docker rm "$svc" >/dev/null

  echo
  echo "下一步："
  echo "  docker compose -f $COMPOSE up -d $svc"
  echo "然后验证该服务，再处理下一个。"
}

case "${1:-}" in
  status) status;;
  rabbitmq|nacos|redis|mysql57) migrate "$1";;
  *) echo "用法: sudo bash $0 status|rabbitmq|nacos|redis|mysql57"; exit 1;;
esac
