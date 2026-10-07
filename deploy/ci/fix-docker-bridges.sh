#!/usr/bin/env bash
# 补齐 docker 网桥的网关 IPv4。
#
# 为什么需要：虚拟机 suspend/resume 时宿主网络会被重新配置（VMware Tools 的
# /etc/vmware-tools/scripts/vmware/network 会按接口 ifdown/ifup），网桥被拆了重连，
# 但网关 IP 是 dockerd(libnetwork) 直接刷到接口上的、不属于任何网络配置文件，
# 没人补回来 —— 于是宿主机没有到容器子网的路由，所有 -p 发布的端口（前端 8080、
# 网关 8760、Gitea 3000、MySQL 3306）全部超时；而容器之间照常通信（同一网桥上二层
# 转发不需要网关），所以 compose 的健康检查全是绿的、看不出问题。
#
# 用法: bash deploy/ci/fix-docker-bridges.sh          # 修复
#       DRY_RUN=1 bash deploy/ci/fix-docker-bridges.sh # 只报告
set -uo pipefail

DRY_RUN=${DRY_RUN:-0}
changed=0

names=$(docker network ls --format '{{.Name}}' | grep -vE '^(host|bridge|none)$')
[ -z "$names" ] && { echo "没有自定义网络"; exit 0; }

# 一次 inspect 取全部网络，避免每分钟起十几个 docker 进程
while IFS='|' read -r net driver id sub gw; do
  [ "$driver" = "bridge" ] || continue
  [ -n "$sub" ] && [ -n "$gw" ] || continue

  br="br-${id:0:12}"
  if ! ip link show "$br" >/dev/null 2>&1; then
    echo "跳过 $net: 网桥 $br 不存在"
    continue
  fi

  if ip -4 -o addr show dev "$br" 2>/dev/null | grep -q "inet ${gw%/*}/"; then
    continue
  fi

  echo "缺失 $net: $br 上没有 ${gw}/${sub##*/}"
  if [ "$DRY_RUN" = "1" ]; then
    echo "  (预演) ip addr add ${gw}/${sub##*/} dev $br"
  else
    ip addr add "${gw}/${sub##*/}" dev "$br" || { echo "  添加失败"; continue; }
    ip link set "$br" up
    echo "  已补 ${gw}/${sub##*/} -> $br"
  fi
  changed=$((changed+1))
done < <(docker network inspect $names --format '{{.Name}}|{{.Driver}}|{{.Id}}|{{range .IPAM.Config}}{{.Subnet}}|{{.Gateway}}{{end}}')

if [ "$changed" = "0" ]; then
  echo "全部网桥网关地址正常，无需处理"
else
  echo "本次补齐 $changed 个网桥地址"
fi
exit 0
