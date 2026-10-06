#!/usr/bin/env bash
# 部署后自检：加固有没有被冲掉、构建残留有没有被清掉。
# 用法: bash deploy/ci/deploy-check.sh   （在 CI 宿主机上跑，只读，不改任何东西）
set -uo pipefail

APP_FILE=${APP_FILE:-deploy/apps/docker-compose.app.yml}
KEEP=${KEEP:-5}
fail=0

echo "=== 1. 应用服务加固是否在线 ==="
printf "%-18s %-10s %-8s %-10s %s\n" 服务 内存上限 nofile 健康 部署来源
for c in $(docker ps --format '{{.Names}}' | grep '^pd-' | sort); do
  mem=$(docker inspect -f '{{.HostConfig.Memory}}' "$c")
  fd=$(docker inspect -f '{{range .HostConfig.Ulimits}}{{if eq .Name "nofile"}}{{.Hard}}{{end}}{{end}}' "$c")
  hp=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}无探针{{end}}' "$c")
  wd=$(docker inspect -f '{{index .Config.Labels "com.docker.compose.project.working_dir"}}' "$c")
  memm=$([ "${mem:-0}" -gt 0 ] && awk -v m="$mem" 'BEGIN{printf "%.0fM", m/1048576}' || echo "0M(!)")
  [ "${mem:-0}" -gt 0 ] && [ -n "$fd" ] || fail=1
  printf "%-18s %-10s %-8s %-10s %s\n" "$c" "$memm" "${fd:-无(!)}" "$hp" "${wd:-?}"
done

echo
echo "=== 2. 中间件是否健康 ==="
for c in mysql57 redis nacos zookeeper kafka rabbitmq; do
  printf "%-12s %s\n" "$c" "$(docker inspect -f '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{end}}' "$c" 2>/dev/null || echo 不存在)"
done

echo
echo "=== 3. 构建残留是否收在保留窗口内 ==="
worst=0
while read -r repo n; do
  [ "$n" -gt "$worst" ] && worst=$n
  [ "$n" -gt "$KEEP" ] && printf "  超出 KEEP=%s: %s 有 %s 个 tag\n" "$KEEP" "$repo" "$n" && fail=1
done < <(docker images --format '{{.Repository}}' | grep '^pinda/pd-' | sort | uniq -c | awk '{print $2" "$1}')
echo "  单服务最多 tag 数: $worst（KEEP=$KEEP）"
docker images -f dangling=true -q | wc -l | sed 's/^/  悬空镜像: /'
docker builder du 2>/dev/null | tail -1 | sed 's/^/  /'
docker system df | awk 'NR==1 || /Volumes/ {print "  "$0}'

echo
echo "=== 4. 磁盘与内存水位 ==="
df -h / | tail -1 | awk '{print "  根分区: "$3" 已用 / "$5" 使用率 / "$4" 可用"}'
free -m | awk 'NR==2{printf "  内存: 用 %sM / 共 %sM，可用 %sM\n", $3, $2, $7} NR==3{printf "  交换: 用 %sM / 共 %sM\n", $3, $2}'

echo
echo "=== 5. 鉴权链路冒烟（nginx -> 网关 -> auth-server -> redis） ==="
code=$(curl -s -o /dev/null -w '%{http_code}' -m 8 http://127.0.0.1:8080/api/authority/anno/captcha)
echo "  验证码接口: HTTP $code"
[ "$code" = "200" ] || fail=1

echo
[ "$fail" = 0 ] && echo "结论: 全部通过" || echo "结论: 有项目需要处理（见上面带 ! 或超出的行）"
exit "$fail"
