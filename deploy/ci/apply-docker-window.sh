#!/usr/bin/env bash
# 在"可以中断整栈 1-2 分钟"的窗口里执行：让 dockerd 重新接管网桥与桥接过滤。
# 三件事打包，避免为同一个目的重启两次：
#   1) /etc/docker/daemon.json 增加容器默认 nofile=65535 与 BuildKit 自动 GC；
#   2) 加载 br_netfilter（Docker 官方前置条件，当前缺失：/proc/sys/net/bridge 不存在）；
#   3) 重启 dockerd 后补一次网桥网关地址（pinda-bridge-fix）。
#
# 用法: bash deploy/ci/apply-docker-window.sh            # 实际执行
#       DRY_RUN=1 bash deploy/ci/apply-docker-window.sh  # 只看要改什么
# 前提: deploy/ci/fix-docker-bridges.sh 已在 /usr/local/bin/pinda-bridge-fix.sh
set -uo pipefail

DRY_RUN=${DRY_RUN:-0}
TARGET=/etc/docker/daemon.json
PENDING=/etc/docker/daemon.json.pinda

cat > "$PENDING" <<'JSON'
{
  "registry-mirrors": [
    "https://docker.1ms.run",
    "https://docker.1panel.live",
    "https://docker.m.daocloud.io"
  ],
  "log-driver": "json-file",
  "log-opts": { "max-size": "100m", "max-file": "2" },
  "default-ulimits": {
    "nofile": { "Name": "nofile", "Hard": 65535, "Soft": 65535 }
  },
  "builder": {
    "gc": {
      "enabled": true,
      "policy": [
        { "maxUsedSpace": "2GB", "minFreeSpace": "10GB", "keepDuration": "48h" }
      ]
    }
  }
}
JSON

echo "=== 1) daemon.json 差异 ==="
diff "$TARGET" "$PENDING" && echo "  (无差异)"

echo "=== 2) 桥接过滤前置条件 ==="
echo "  br_netfilter 已加载: $(lsmod | grep -c br_netfilter)  (0=未加载)"
echo "  /etc/modules-load.d/pinda-docker.conf -> $(cat /etc/modules-load.d/pinda-docker.conf 2>/dev/null || echo 尚未创建)"
echo "  ufw 桥接口 FORWARD 放行: $(grep -c 'ufw-before-forward -i br-+' /etc/ufw/before.rules 2>/dev/null) 条"

if [ "$DRY_RUN" = "1" ]; then
  echo
  echo "(预演模式，未做任何改动。去掉 DRY_RUN=1 才会真的重启 dockerd)"
  exit 0
fi

echo "=== 3) 执行 ==="
cp -a "$TARGET" "$TARGET.bak-$(date +%F-%H%M%S)"
cp -a "$PENDING" "$TARGET"
printf 'br_netfilter\n' > /etc/modules-load.d/pinda-docker.conf
printf 'net.bridge.bridge-nf-call-iptables=1\nnet.bridge.bridge-nf-call-ip6tables=1\n' > /etc/sysctl.d/99-pinda-bridge.conf
modprobe br_netfilter 2>/dev/null || echo "  modprobe br_netfilter 失败（继续，重启后 modules-load 会再试）"
sysctl --system >/dev/null 2>&1

echo "  重启 dockerd（整栈会中断约 1 分钟）"
BAK=$(ls -t "$TARGET".bak-* | head -1)
systemctl restart docker
sleep 8

# builder.gc 的取值格式（字符串 "2GB"/"48h"）没有实测验证过，写错会让 dockerd 起不来，
# 所以这里给 60 秒观察窗，起不来就立刻用刚才的备份回滚并再重启一次。
ok=0
for i in $(seq 1 12); do
  if docker version >/dev/null 2>&1 && [ "$(docker ps -q | wc -l)" -gt 15 ]; then ok=1; break; fi
  sleep 5
done
if [ "$ok" != "1" ]; then
  echo "  !! dockerd 没起来，回滚 $BAK"
  cp -a "$BAK" "$TARGET"
  systemctl restart docker
  sleep 10
  echo "  回滚后运行容器数: $(docker ps -q | wc -l)（builder.gc 需换成字节/纳秒整数等格式再试）"
  bash /usr/local/bin/pinda-bridge-fix.sh
  exit 1
fi

bash /usr/local/bin/pinda-bridge-fix.sh

echo "=== 4) 核对 ==="
docker info 2>/dev/null | grep -A3 -i "default ulimits" | head -4
echo "  运行容器数: $(docker ps -q | wc -l) / 期望 22"
echo "  前端 8081: $(timeout 8 curl -s -o /dev/null -w '%{http_code}' -m 7 http://127.0.0.1:8081/)"
echo "  Gitea 3000: $(timeout 8 curl -s -o /dev/null -w '%{http_code}' -m 7 http://127.0.0.1:3000/api/healthz)"
echo "  验证码: $(timeout 8 curl -s -o /dev/null -w '%{http_code}' -m 7 http://127.0.0.1:8081/prod-api/authority/anno/captcha)"
echo
echo "回滚: cp -a $TARGET.bak-<时间戳> $TARGET && systemctl restart docker"
