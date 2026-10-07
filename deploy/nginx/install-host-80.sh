#!/usr/bin/env bash
# 安装宿主机 :80 入口配置（反代到 CI 构建的 pd-admin-ui 容器）。
# 用途：换机/重建后一条命令恢复，避免 :80 与 :8080 又各跑一份产物。
# 用法: bash deploy/nginx/install-host-80.sh
set -euo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
TS=$(date +%F-%H%M)

# conf.d 只 include *.conf，后缀写错文件根本不会被加载（踩过）
install -m 0644 "$HERE/pinda-upgrade.conf" /etc/nginx/conf.d/pinda-upgrade.conf

if [ -f /etc/nginx/sites-available/pinda ]; then
  cp -a /etc/nginx/sites-available/pinda "/etc/nginx/sites-available/pinda.bak-$TS"
fi
install -m 0644 "$HERE/pinda-80.conf" /etc/nginx/sites-available/pinda
ln -sfn /etc/nginx/sites-available/pinda /etc/nginx/sites-enabled/pinda

nginx -t
systemctl reload nginx

echo "=== 核对 :80 与 :8080 是否为同一份产物 ==="
a=$(curl -s -m 7 http://127.0.0.1/ | grep -oE "static/js/app\.[a-z0-9]+\.js" | head -1)
b=$(curl -s -m 7 http://127.0.0.1:8080/ | grep -oE "static/js/app\.[a-z0-9]+\.js" | head -1)
echo "  :80    $a"
echo "  :8080  $b"
[ "$a" = "$b" ] || { echo "  两个入口产物不一致，检查 pd-admin-ui 是否健康"; exit 1; }
curl -s -m 7 http://127.0.0.1/api/authority/anno/captcha -o /dev/null -w "  :80 /api 验证码: HTTP %{http_code}\n"
