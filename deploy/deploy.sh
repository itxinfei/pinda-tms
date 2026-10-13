#!/bin/bash
# ============================================================
# 品达TMS 统一部署入口（服务器 /data/pinda-deploy/deploy.sh）
# 真源在仓库 deploy/deploy.sh，改脚本一律改仓库版再同步，勿直接编辑本文件
# ============================================================
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"

help() {
  cat <<'EOF'
品达 TMS 统一部署入口
用法: bash deploy.sh <命令> [参数]

后端微服务（13 个，失败自动回滚）:
  deploy.sh be <服务>          例: deploy.sh be pd-base
  deploy.sh be <服务,服务>     例: deploy.sh be pd-base,pd-oms （逗号分隔多服务）
  deploy.sh be all             全部 13 个后端

前端 H5（4 端，失败自动回滚）:
  deploy.sh fe admin           管理端  (http://192.168.20.130:8081)
  deploy.sh fe driver          司机端  (http://192.168.20.130:8082)
  deploy.sh fe courier         快递员端(http://192.168.20.130:8083)
  deploy.sh fe customer        客户端  (http://192.168.20.130:8084)
  deploy.sh fe all             全部 4 端

全部一次性部署:
  deploy.sh all                后端 13 个 + 前端 4 端（顺序: 先后端后前端）

其他:
  deploy.sh help               本帮助

后端可选服务清单:
  pd-auth pd-base pd-oms pd-work pd-user pd-dispatch pd-aggregation pd-netty
  pd-web-manager pd-web-driver pd-web-courier pd-web-customer pd-gateway

也可在 Gitea UI 下拉选择部署（免记命令）:
  Actions → deploy-backend（后端，下拉选服务/全部）
  Actions → deploy-frontend（前端，下拉选端/全部）

注意:
  - 后端从 /data/pinda-tms checkout 构建，脚本【不自动 pull】；
    部署前确认: git -C /data/pinda-tms pull --ff-only（或核对 HEAD 为目标提交）
  - 前端自动从 Gitea master 拉最新代码构建
  - 任一服务/端部署后 4 分钟(前端 60 秒)内未健康，自动回滚到上一版本
EOF
}

case "${1:-help}" in
  be)
    [ -n "${2:-}" ] || { echo "用法: deploy.sh be <服务>[,服务...] | all（deploy.sh help 查清单）"; exit 1; }
    exec bash "$DIR/deploy-be.sh" "$2"
    ;;
  fe)
    [ -n "${2:-}" ] || { echo "用法: deploy.sh fe <admin|driver|courier|customer|all>"; exit 1; }
    exec bash "$DIR/deploy-fe.sh" "$2"
    ;;
  all)
    echo "==> [1/2] 后端全部（13 个）"
    bash "$DIR/deploy-be.sh" all
    echo "==> [2/2] 前端全部（4 端）"
    bash "$DIR/deploy-fe.sh" all
    echo "✅ 全部 17 个容器部署完成"
    ;;
  help|-h|--help)
    help
    ;;
  *)
    echo "未知命令: $1"
    echo ""
    help
    exit 1
    ;;
esac
