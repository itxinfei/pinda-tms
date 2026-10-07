#!/bin/bash
# 构建残留清理：镜像按服务保留最近 N 个 tag + 回收悬空镜像 + 封顶 BuildKit 缓存。
#
# 为什么需要它：act_runner v0.6.1 没有内置 GC（config 只支持 log/runner/cache/
# container/host），而流水线是本地 docker build、不推 registry，所以每次构建都往
# 本机堆一批 git-sha tag（实测 22 小时堆出 191 个 tag / 可回收 19GB）。
#
# 安全边界（重要）：
#   - 只动 pinda/pd-* 这一批应用镜像，绝不碰基础镜像与中间件镜像
#     （运行时基础镜像 pinda/jre8-fontconfig:*、mysql/redis/nacos/kafka/
#      zookeeper/rabbitmq/gitea/*、ci/pinda-ci、eclipse-temurin 等，这些是
#      有意长期保留的）。注意：基础镜像名是 pinda/jre8-fontconfig，不以
#      pinda/pd- 开头，本脚本的仓库过滤(grep '^pinda/pd-')天然不会选中它；
#      若以后放宽该过滤，必须同时显式排除 pinda/jre8-fontconfig，否则下次
#      构建会退回 14 次 apt；
#   - 任何被容器（含已退出容器）引用的镜像一律不删；
#   - 当前部署的 TAG 与最新 KEEP 个 tag 保留；
#   - 支持 DRY_RUN=1 先看会删什么，不实际执行。
#
# 用法：
#   DRY_RUN=1 KEEP=5 bash cleanup-images.sh          # 预演
#   KEEP=5 TAG=1a2b3c4 bash cleanup-images.sh        # 实际执行
set -uo pipefail

KEEP=${KEEP:-5}
TAG=${TAG:-}
DRY_RUN=${DRY_RUN:-0}
BUILDER_KEEP_AGE=${BUILDER_KEEP_AGE:-72h}

log() { printf '%s\n' "$*"; }

before=$(docker system df --format '{{.Size}}' | head -1)

# 收集"绝不能删"的镜像：所有容器（含 exited）引用的镜像名与镜像 ID
protected_ids=$(mktemp)
protected_tags=$(mktemp)
trap 'rm -f "$protected_ids" "$protected_tags"' EXIT

for cid in $(docker ps -aq 2>/dev/null); do
  img=$(docker inspect -f '{{.Image}}' "$cid" 2>/dev/null)
  name=$(docker inspect -f '{{.Config.Image}}' "$cid" 2>/dev/null)
  [ -n "$img" ] && echo "$img" >> "$protected_ids"
  [ -n "$name" ] && [ "$name" != "<no value>" ] && echo "$name" >> "$protected_tags"
done
[ -n "$TAG" ] && for r in $(docker images --format '{{.Repository}}' | grep '^pinda/pd-' | sort -u); do
  echo "$r:$TAG" >> "$protected_tags"
done

kept_total=0; deleted_total=0

for repo in $(docker images --format '{{.Repository}}' | grep '^pinda/pd-' | sort -u); do
  # 该服务下的 tag 按创建时间倒序（本机 docker CLI 不支持 --sort，故显式排）；
  # CreatedAt 形如 "2026-10-06 08:12:34 +0000 UTC"，年月日在前，字典序即时间序。
  all=$(docker images --format '{{.Tag}}|{{.CreatedAt}}' "$repo" \
        | grep -v '^<none>|' | sort -t'|' -k2 -r | cut -d'|' -f1)
  n=$(printf '%s\n' "$all" | grep -c . || true)
  [ "$n" -le "$KEEP" ] && continue

  keep_list=$(printf '%s\n' "$all" | head -n "$KEEP")
  for t in $(printf '%s\n' "$all"); do
    printf '%s\n' "$keep_list" | grep -qx "$t" && { kept_total=$((kept_total+1)); continue; }
    ref="$repo:$t"
    if grep -Fxq "$ref" "$protected_tags" 2>/dev/null; then
      log "  跳过在用: $ref"
      continue
    fi
    iid=$(docker images --format '{{.Repository}}:{{.Tag}} {{.ID}}' "$repo" | awk -v r="$ref" '$1==r{print $2}')
    if [ -n "$iid" ] && grep -qF "$iid" "$protected_ids" 2>/dev/null; then
      log "  跳过被容器引用: $ref ($iid)"
      continue
    fi
    if [ "$DRY_RUN" = "1" ]; then
      log "  [预演] 将删除 $ref"
    else
      docker rmi "$ref" >/dev/null 2>&1 && log "  删除 $ref" || log "  删除失败(可能有依赖) $ref"
    fi
    deleted_total=$((deleted_total+1))
  done
done

log ""
log "=== 悬空镜像回收 ==="
d=$(docker images -f dangling=true -q | wc -l)
log "  悬空 $d 个"
[ "$DRY_RUN" = "1" ] || docker image prune -f >/dev/null 2>&1 || true

log "=== BuildKit 缓存按时间回收（未使用超过 $BUILDER_KEEP_AGE） ==="
# 实测：dockerd 内置 BuildKit 上 --max-used-space / --keep-storage 都是空操作（输出 Total: 0B），
# 容量封顶要靠 daemon.json 的 builder.gc.policy；这里能真正生效的是按时间的 --filter until=
[ "$DRY_RUN" = "1" ] || docker builder prune -f --filter "until=$BUILDER_KEEP_AGE" >/dev/null 2>&1 || true

after=$(docker system df --format '{{.Size}}' | head -1)
log ""
log "汇总: 保留 $kept_total 个、处理 $deleted_total 个旧 tag；镜像层占用 $before -> $after"
[ "$DRY_RUN" = "1" ] && log "(预演模式，未做任何删除)"
exit 0
