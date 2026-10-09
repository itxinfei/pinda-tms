#!/usr/bin/env bash
# 安装宿主机 :80 入口配置（反代到新版后台容器 pd-admin-h5）。
# 用途：换机/重建后一条命令恢复，避免 :80 与容器端口又各跑一份产物。
# 用法: bash deploy/nginx/install-host-80.sh
#
# ！！重要（2026-10-09 实测踩坑）：
#   本脚本装的是「系统自带 nginx」的路径（/etc/nginx/）。
#   宝塔面板装的 nginx 是另一套：读 /www/server/nginx/conf/nginx.conf，
#   其中 include 的是 /www/server/panel/vhost/nginx/*.conf。
#   两套体系互不 include，所以本脚本在宝塔机器上「执行成功但完全没生效」——
#   访问 :80 会命中宝塔的「没有找到站点」页面。
#   宝塔环境请直接在面板「网站」里建站点，或把本文件放到 /www/server/panel/vhost/nginx/ 下。
set -euo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
TS=$(date +%F-%H%M)

if [ -d /www/server/panel/vhost/nginx ]; then
    echo "!! 检测到宝塔 nginx。本脚本的 /etc/nginx 路径对它无效，改装到宝塔 vhost 目录。"
    DEST_DIR=/www/server/panel/vhost/nginx
    install -m 0644 "$HERE/pinda-upgrade.conf" "$DEST_DIR/pinda-upgrade.conf"
    install -m 0644 "$HERE/pinda-80.conf" "$DEST_DIR/pinda.conf"
    echo "已安装到 $DEST_DIR，请执行: /www/server/nginx/sbin/nginx -t && /www/server/nginx/sbin/nginx -s reload"
    exit 0
fi

# conf.d 只 include *.conf，后缀写错文件根本不会被加载（踩过）
install -m 0644 "$HERE/pinda-upgrade.conf" /etc/nginx/conf.d/pinda-upgrade.conf

if [ -f /etc/nginx/sites-available/pinda ]; then
    cp -a /etc/nginx/sites-available/pinda "/etc/nginx/sites-available/pinda.bak-$TS"
fi
install -m 0644 "$HERE/pinda-80.conf" /etc/nginx/sites-available/pinda
ln -sfn /etc/nginx/sites-available/pinda /etc/nginx/sites-enabled/pinda

nginx -t
systemctl reload nginx

echo "=== 核对 :80 与 :8081 是否为同一份产物 ==="
a=$(curl -s -m 7 http://127.0.0.1/ | grep -oE "assets/js/index\.[a-zA-Z0-9]+\.js" | head -1)
b=$(curl -s -m 7 http://127.0.0.1:8081/ | grep -oE "assets/js/index\.[a-zA-Z0-9]+\.js" | head -1)
echo "  :80    $a"
echo "  :8081  $b"
[ "$a" = "$b" ] || { echo "  两个入口产物不一致，检查 pd-admin-h5 是否健康"; exit 1; }
curl -s -m 7 http://127.0.0.1/prod-api/authority/anno/captcha -o /dev/null -w "  :80 /prod-api 验证码: HTTP %{http_code}\n"
