#!/usr/bin/env bash
# ============================================================
# 心飞物流TMS 后端手动部署脚本（在宿主 pdwl 执行）
#
# 用法:
#   bash deploy-be.sh <svc>[,<svc>...]   # 部署指定服务
#   bash deploy-be.sh all                # 按固定顺序部署全部 14 个后端
# 例:
#   bash deploy-be.sh pd-dispatch
#   bash deploy-be.sh pd-base,pd-oms
#
# 流程: 白名单校验 -> 容器归属检查 -> 记录 PREV 镜像基线
#       -> ci/xinfei-ci 容器内 mvn 打包 -> 硬链接 fat jar 到 .ctx
#       -> docker build -> compose 滚动 -> 健康检查 -> 失败按 PREV 自动回滚
#
# Maven 构建（2026-10-12 提速改造，与 Gitea deploy-backend.yml 同构）:
#   - 单/多服务默认只 package 目标模块，上游 SNAPSHOT 取自持久 m2（宿主 /data/m2 挂为
#     容器 /root/.m2，与 Actions job 的 container.options 挂载完全一致；settings 已写死
#     localRepository=/root/.m2/repository；基线文件 /root/.m2/.xinfei-baseline.revision）；
#   - 基线之后改过的内部上游模块先 install 刷新，再打目标包（不带 -am）；
#   - 无基线 / 命中 pom.xml 变更 / install 或 package 失败，自动回退 -am 全链构建（旧行为兜底）；
#   - all 维持 -pl 全模块 -am clean package。
#
# 约定:
#   - 不自动 git pull：服务器 compose/env 含本地-only 配置，自动拉取有冲突风险；
#     部署前自行确认 /data/xinfei-tms 已检出目标提交（脚本会打印当前 HEAD）。
#   - Maven 在 ci/xinfei-ci 容器内执行（与 Gitea Actions 同一 JDK21/Maven3.9.9/aliyun 源），
#     镜像构建与 compose 在宿主执行。
#   - compose 顶层 name: xinfei-apps；逐服务以 PD_<SVC>_TAG 覆盖镜像 tag。
# ============================================================
set -uo pipefail

REPO_DIR=/data/xinfei-tms
CI_IMAGE=ci/xinfei-ci:latest
COMPOSE_FILE=$REPO_DIR/deploy/apps/docker-compose.app.yml
APPS_ENV=/data/ci/apps.env
# Maven settings：mvn 在 ci 容器内执行，宿主 /data/ci 挂为容器内 /ci（只读），必须用容器路径
SETTINGS=/ci/maven-settings.xml
CTX_ROOT=$REPO_DIR/.ctx
HEALTH_TRIES=48   # 每 5s 一次，最长 4 分钟

# 固定部署顺序：基础/业务先上，网关 pd-gateway 始终最后切
ORDER=(pd-auth pd-base pd-oms pd-work pd-user pd-dispatch pd-aggregation pd-netty \
       pd-web-manager pd-web-driver pd-web-courier pd-web-customer pd-gateway)
declare -A MOD_OF=(
  [pd-auth]=pd-auth [pd-base]=pd-base [pd-oms]=pd-oms [pd-work]=pd-work
  [pd-user]=pd-user [pd-dispatch]=pd-dispatch [pd-aggregation]=pd-aggregation
  [pd-netty]=pd-netty
  [pd-web-manager]=pd-web/pd-web-manager [pd-web-driver]=pd-web/pd-web-driver
  [pd-web-courier]=pd-web/pd-web-courier [pd-web-customer]=pd-web/pd-web-customer
  [pd-gateway]=pd-gateway
)

usage() {
  echo "用法: bash deploy-be.sh <svc>[,<svc>...] | all"
  echo "可选服务: ${ORDER[*]}"
  exit 1
}
[ $# -ge 1 ] || usage
RAW="$1"

# 解析并校验服务白名单
WANT=()
if [ "$RAW" = "all" ] || [ "$RAW" = "ALL" ]; then
  WANT=("${ORDER[@]}")
else
  IFS=', ' read -ra TOKS <<< "$RAW"
  for t in "${TOKS[@]}"; do
    [ -z "$t" ] && continue
    if [ -z "${MOD_OF[$t]+x}" ]; then echo "未知服务: $t"; usage; fi
    dup=0
    for w in ${WANT[@]+"${WANT[@]}"}; do [ "$w" = "$t" ] && dup=1; done
    [ $dup -eq 0 ] && WANT+=("$t")
  done
fi
# 按固定顺序重排
SERVICES=()
for s in "${ORDER[@]}"; do
  for w in ${WANT[@]+"${WANT[@]}"}; do [ "$w" = "$s" ] && SERVICES+=("$s") && break; done
done
[ ${#SERVICES[@]} -gt 0 ] || { echo "未选中任何有效服务"; exit 1; }

# 全量发布走全链构建；其余走 m2 基线增量（容器内脚本据此分支）
if [ "$RAW" = "all" ] || [ "$RAW" = "ALL" ]; then FULL_BUILD=true; else FULL_BUILD=false; fi

cd "$REPO_DIR" || { echo "仓库目录不存在: $REPO_DIR"; exit 1; }
TAG="manual-$(date +%Y%m%d%H%M%S)"
HEAD=$(git rev-parse --short HEAD 2>/dev/null || echo unknown)
echo "==> 仓库 $REPO_DIR  HEAD=$HEAD  TAG=$TAG"
echo "==> 部署顺序: ${SERVICES[*]}"

# 容器归属检查：同名容器存在但非 xinfei-apps 管理则中止（不自动删，防误删）
for s in "${SERVICES[@]}"; do
  if docker inspect -f '{{.Id}}' "$s" >/dev/null 2>&1; then
    pj=$(docker inspect -f '{{index .Config.Labels "com.docker.compose.project"}}' "$s" 2>/dev/null || echo "")
    if [ "$pj" != "xinfei-apps" ]; then
      echo "!! 容器 $s 存在但 project='$pj'（非 xinfei-apps），compose up 会撞名。"
      echo "   确认可由 compose 接管后先执行: docker rm -f $s"
      exit 1
    fi
  fi
done

# 记录回滚基线
declare -A PREV=()
for s in "${SERVICES[@]}"; do
  img=$(docker inspect -f '{{.Config.Image}}' "$s" 2>/dev/null || true)
  if [ -n "$img" ]; then PREV[$s]="${img##*:}"; echo "  基线 $s -> ${PREV[$s]}"; else echo "  基线 $s -> （无容器，首次部署）"; fi
done

# mvn -pl 模块路径逗号列表
MODS=""
for s in "${SERVICES[@]}"; do MODS="${MODS},${MOD_OF[$s]}"; done
MODS="${MODS#,}"

echo "==> [1/5] ci 容器内 Maven 打包（目标 $MODS；m2 基线增量优先，异常自动回退 -am）"
# 宿主 /data/m2 挂为容器 /root/.m2（与 act_runner container.options 同款挂载），
# settings 写死 /root/.m2/repository，两条链路共用同一持久仓库与基线文件。
# FULL_BUILD/MODS 经 -e 传入容器；容器内 git 读 /workspace（=宿主仓库，完整历史）。
docker run -i --rm --network host \
  -v "$REPO_DIR":/workspace -w /workspace \
  -v /data/m2:/root/.m2 \
  -v /data/ci:/ci:ro \
  -e FULL_BUILD="$FULL_BUILD" -e MODS="$MODS" \
  "$CI_IMAGE" bash -s <<'CTR' || { echo "!! Maven 打包失败，未改动任何容器"; exit 1; }
set -euo pipefail
SETTINGS=/ci/maven-settings.xml
BASELINE_FILE=/root/.m2/.xinfei-baseline.revision
MVN_COMMON="-B -Dmaven.test.skip=true -s $SETTINGS"

fallback_build() {
  echo "   !! 回退 -am 全链构建（目标模块：$MODS）"
  mvn $MVN_COMMON -pl "$MODS" -am clean package
}

if [ "$FULL_BUILD" = "true" ]; then
  echo "   全量发布（all），走 -am 全链构建"
  mvn $MVN_COMMON -pl "$MODS" -am clean package
  exit 0
fi

baseline=""
[ -f "$BASELINE_FILE" ] && baseline=$(cat "$BASELINE_FILE" 2>/dev/null || true)
if [ -z "$baseline" ] || ! git cat-file -e "${baseline}^{commit}" 2>/dev/null; then
  echo "   m2 基线不可用（baseline='${baseline:-空}'，可能新版 build-check 尚未跑过）"
  fallback_build
  exit 0
fi
echo "   m2 基线=$baseline  HEAD=$(git rev-parse HEAD)"

# 变更文件 → 内部模块归属（向上找最近的含 pom.xml 目录）；命中任意 pom.xml 直接保守回退
changed_mods=""
pom_changed=0
while IFS= read -r f; do
  [ -z "$f" ] && continue
  case "$f" in
    pom.xml|*/pom.xml) pom_changed=1; echo "   命中 pom 变更：$f"; break ;;
  esac
  d=$(dirname "$f")
  while [ "$d" != "." ] && [ ! -f "$d/pom.xml" ]; do d=$(dirname "$d"); done
  [ "$d" = "." ] && continue
  case ",$changed_mods," in *",$d,"*) ;; *) changed_mods="$changed_mods,$d";; esac
done < <(git diff --name-only "$baseline" HEAD)
changed_mods="${changed_mods#,}"

if [ "$pom_changed" = "1" ]; then
  fallback_build
  exit 0
fi

# 目标模块自身马上 package，不必先 install
install_mods=""
if [ -n "$changed_mods" ]; then
  IFS=',' read -ra arr <<< "$changed_mods"
  for m in "${arr[@]}"; do
    [ -z "$m" ] && continue
    case ",$MODS," in
      *",$m,"*) echo "   跳过目标模块自身：$m" ;;
      *) install_mods="$install_mods,$m" ;;
    esac
  done
fi
install_mods="${install_mods#,}"

if [ -n "$install_mods" ]; then
  echo "   基线后变更的上游模块，先 install：$install_mods"
  if ! mvn $MVN_COMMON -pl "$install_mods" clean install -T 1C; then
    echo "   !! 变更上游模块 install 失败"
    fallback_build
    exit 0
  fi
else
  echo "   基线后无上游模块变更，目标模块直接从 m2 解析打包"
fi

if ! mvn $MVN_COMMON -pl "$MODS" clean package; then
  echo "   !! 目标模块直接打包失败（m2 上游可能与源码不一致）"
  fallback_build
  exit 0
fi
CTR

echo "==> [2/5] 准备构建上下文 .ctx"
for s in "${SERVICES[@]}"; do
  m="${MOD_OF[$s]}"
  jar=$(find "$m/target" -maxdepth 1 -type f -name '*.jar' \
    ! -name '*sources*' ! -name '*javadoc*' ! -name '*.original' 2>/dev/null | head -1)
  [ -n "$jar" ] || { echo "!! 未找到 $s（$m）的 fat jar"; exit 1; }
  mkdir -p "$CTX_ROOT/$s"
  ln -f "$jar" "$CTX_ROOT/$s/app.jar"
  echo "   $s <- $(basename "$jar") ($(du -h "$jar" | cut -f1))"
done

echo "==> [3/5] docker build"
for s in "${SERVICES[@]}"; do
  echo "   build xinfei/$s:$TAG"
  docker build -f deploy/apps/Dockerfile -t "xinfei/$s:$TAG" "$CTX_ROOT/$s" \
    || { echo "!! 镜像构建失败: $s（旧容器未改动）"; exit 1; }
done

# 回滚：把本次部署、且有 PREV 基线的服务恢复到旧镜像；首次部署无基线则跳过
rollback() {
  echo "!! 触发回滚"
  local any=0 ok=1 s p tv
  for s in "${SERVICES[@]}"; do
    p="${PREV[$s]:-}"
    if [ -z "$p" ]; then echo "   跳过 $s（首次部署无基线，需人工处理）"; continue; fi
    tv="$(echo "$s" | tr 'a-z-' 'A-Z_')_TAG"
    echo "   回滚 $s -> $p"
    env TAG="$TAG" "$tv=$p" docker compose --env-file "$APPS_ENV" -f "$COMPOSE_FILE" up -d "$s" && any=1
  done
  if [ $any -eq 1 ]; then
    for _ in $(seq 1 $HEALTH_TRIES); do
      ok=1
      for s in "${SERVICES[@]}"; do
        [ -z "${PREV[$s]:-}" ] && continue
        st=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$s" 2>/dev/null || echo missing)
        [ "$st" = "healthy" ] || ok=0
      done
      [ $ok -eq 1 ] && break
      sleep 5
    done
  fi
  echo "!! 已回滚 any=$any healthy=$ok"
}

echo "==> [4/5] compose 滚动 + 健康检查"
for s in "${SERVICES[@]}"; do
  tv="$(echo "$s" | tr 'a-z-' 'A-Z_')_TAG"
  echo "   >> 部署 $s:$TAG"
  env TAG="$TAG" "$tv=$TAG" docker compose --env-file "$APPS_ENV" -f "$COMPOSE_FILE" up -d "$s"
  ok=0
  for _ in $(seq 1 $HEALTH_TRIES); do
    st=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$s" 2>/dev/null || echo missing)
    [ "$st" = "healthy" ] && { ok=1; break; }
    sleep 5
  done
  docker inspect -f '   {{.Name}} health={{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$s" || true
  if [ $ok -ne 1 ]; then
    echo "!! $s 部署后 4 分钟内未健康"
    rollback
    exit 1
  fi
done

echo "==> [5/5] 清理悬空镜像"
docker image prune -f >/dev/null
echo "✅ 部署完成: ${SERVICES[*]}  TAG=$TAG  HEAD=$HEAD"
